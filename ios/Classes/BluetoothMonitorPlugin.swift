import Flutter
import UIKit
import AVFoundation

public class BluetoothMonitorPlugin: NSObject, FlutterPlugin {
    private var bluetoothEventSink: FlutterEventSink?
    private var audioSession: AVAudioSession!
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "bluetooth_monitor", binaryMessenger: registrar.messenger())
        let eventChannel = FlutterEventChannel(name: "bluetooth_monitor_events", binaryMessenger: registrar.messenger())
        let instance = BluetoothMonitorPlugin()
        
        registrar.addMethodCallDelegate(instance, channel: channel)
        eventChannel.setStreamHandler(instance)
        
        instance.setupAudioSessionObservers()
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "getBluetoothState":
            result(getBluetoothState())
        case "getConnectedDevices", "getCurrentlyConnectedDevices":
            getConnectedDevices(result: result)
        case "disconnectDevice":
            result(false)
        case "connectDevice":
            result(false)
        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    private func setupAudioSessionObservers() {
        audioSession = AVAudioSession.sharedInstance()
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(audioRouteChanged),
            name: AVAudioSession.routeChangeNotification,
            object: nil
        )
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            self.checkConnectedAudioDevices()
        }
    }
    
    @objc private func audioRouteChanged(notification: Notification) {
        guard let info = notification.userInfo,
              let reason = info[AVAudioSessionRouteChangeReasonKey] as? UInt else { return }
        
        let changeReason = AVAudioSession.RouteChangeReason(rawValue: reason) ?? .unknown
        
        switch changeReason {
        case .newDeviceAvailable:
            checkConnectedAudioDevices()
        case .oldDeviceUnavailable:
            if let previousRoute = info[AVAudioSessionRouteChangePreviousRouteKey] as? AVAudioSessionRouteDescription {
                for output in previousRoute.outputs {
                    if output.portType != .builtInSpeaker && output.portType != .builtInReceiver {
                        let eventData: [String: Any] = [
                            "event": "DISCONNECTED",
                            "address": output.uid ?? output.portName,
                            "name": output.portName
                        ]
                        sendBluetoothEvent(eventData: eventData)
                    }
                }
            }
            checkConnectedAudioDevices()
        default:
            break
        }
    }
    
    private func checkConnectedAudioDevices() {
        let currentRoute = audioSession.currentRoute
        
        for output in currentRoute.outputs {
            if output.portType != .builtInSpeaker && output.portType != .builtInReceiver {
                let deviceType = getDeviceTypeFromPort(output.portType)
                let eventData: [String: Any] = [
                    "event": "CONNECTED",
                    "name": output.portName,
                    "address": output.uid ?? output.portName,
                    "deviceType": deviceType,
                    "rssi": -50,
                    "batteryLevel": -1
                ]
                sendBluetoothEvent(eventData: eventData)
            }
        }
    }
    
    private func getDeviceTypeFromPort(_ portType: AVAudioSession.Port) -> String {
        switch portType {
        case .bluetoothA2DP, .bluetoothHFP:
            return "Headphones"
        case .bluetoothLE:
            return "BLE Audio Device"
        case .headphones:
            return "Headphones"
        case .airPlay:
            return "Speaker"
        case .carAudio:
            return "Car Audio"
        case .headsetMic:
            return "Headset"
        default:
            return "Audio Device"
        }
    }
    
    private func getBluetoothState() -> String {
        let currentRoute = audioSession.currentRoute
        for output in currentRoute.outputs {
            if output.portType == .bluetoothA2DP || 
               output.portType == .bluetoothHFP || 
               output.portType == .bluetoothLE {
                return "BLUETOOTH_ON"
            }
        }
        return "BLUETOOTH_ON"
    }
    
    private func getConnectedDevices(result: @escaping FlutterResult) {
        var devices: [[String: Any]] = []
        let currentRoute = audioSession.currentRoute
        
        for output in currentRoute.outputs {
            if output.portType == .builtInSpeaker || output.portType == .builtInReceiver {
                continue
            }
            
            let deviceType = getDeviceTypeFromPort(output.portType)
            let deviceInfo: [String: Any] = [
                "name": output.portName,
                "address": output.uid ?? output.portName,
                "batteryLevel": -1,
                "rssi": -50,
                "deviceType": deviceType
            ]
            devices.append(deviceInfo)
        }
        
        result(devices)
    }
    
    private func sendBluetoothEvent(eventData: [String: Any]) {
        bluetoothEventSink?(eventData)
    }
}

extension BluetoothMonitorPlugin: FlutterStreamHandler {
    public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        bluetoothEventSink = events
        return nil
    }
    
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        bluetoothEventSink = nil
        return nil
    }
}
