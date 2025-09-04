import 'dart:async';
import 'package:flutter/services.dart';
import 'bluetooth_device.dart';
import 'bluetooth_event.dart';

class BluetoothMonitor {
  static const MethodChannel _channel = MethodChannel('bluetooth_monitor');
  static const EventChannel _eventChannel = EventChannel('bluetooth_monitor_events');
  
  static StreamSubscription<BluetoothEvent>? _eventSubscription;
  static final StreamController<BluetoothEvent> _eventController = StreamController<BluetoothEvent>.broadcast();

  /// Stream of Bluetooth events (connect/disconnect/battery/rssi updates)
  static Stream<BluetoothEvent> get events => _eventController.stream;

  /// Start listening to Bluetooth events
  static void startListening() {
    _eventSubscription = _eventChannel.receiveBroadcastStream()
        .map((event) => BluetoothEvent.fromMap(Map<String, dynamic>.from(event)))
        .listen(
          (bluetoothEvent) => _eventController.add(bluetoothEvent),
          onError: (error) => {},  // Ignore errors
        );
  }

  /// Stop listening to Bluetooth events
  static void stopListening() {
    _eventSubscription?.cancel();
    _eventSubscription = null;
  }

  /// Get current Bluetooth state (ON/OFF/TURNING_ON/TURNING_OFF)
  static Future<String> getBluetoothState() async {
    try {
      final String state = await _channel.invokeMethod('getBluetoothState');
      return state;
    } on PlatformException {
      // Error getting Bluetooth state
      return 'UNKNOWN';
    }
  }

  /// Get list of connected Bluetooth devices
  static Future<List<BluetoothDevice>> getConnectedDevices() async {
    try {
      final List<dynamic> result = await _channel.invokeMethod('getConnectedDevices');
      return result
          .whereType<Map>()
          .map((deviceMap) => BluetoothDevice.fromMap(Map<String, dynamic>.from(deviceMap)))
          .toList();
    } on PlatformException {
      // Error getting connected devices
      return [];
    }
  }

  /// Disconnect from a specific Bluetooth device
  /// Returns true if disconnection was successful
  static Future<bool> disconnectDevice(String address) async {
    try {
      final bool result = await _channel.invokeMethod('disconnectDevice', {'address': address});
      return result;
    } on PlatformException {
      // Error disconnecting device
      return false;
    }
  }

  /// Connect to a specific Bluetooth device
  /// Returns true if connection was successful
  static Future<bool> connectDevice(String address) async {
    try {
      final bool result = await _channel.invokeMethod('connectDevice', {'address': address});
      return result;
    } on PlatformException {
      // Error connecting device
      return false;
    }
  }

  /// Convenience method to listen for specific event types
  static Stream<BluetoothEvent> listenForEvents(List<BluetoothEventType> eventTypes) {
    return events.where((event) => eventTypes.contains(event.type));
  }

  /// Convenience method to listen for connection changes only
  static Stream<BluetoothEvent> get connectionEvents {
    return listenForEvents([
      BluetoothEventType.connected,
      BluetoothEventType.disconnected,
    ]);
  }

  /// Convenience method to listen for Bluetooth state changes only
  static Stream<BluetoothEvent> get stateEvents {
    return listenForEvents([
      BluetoothEventType.bluetoothOn,
      BluetoothEventType.bluetoothOff,
      BluetoothEventType.bluetoothTurningOn,
      BluetoothEventType.bluetoothTurningOff,
    ]);
  }

  /// Convenience method to listen for battery updates only
  static Stream<BluetoothEvent> get batteryEvents {
    return listenForEvents([BluetoothEventType.batteryLevel]);
  }
}