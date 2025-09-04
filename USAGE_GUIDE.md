# Bluetooth Monitor Plugin - Complete Usage Guide

A comprehensive Flutter plugin for monitoring Bluetooth device connections, disconnections, and device information across Android and iOS platforms.

## Table of Contents
- [Installation](#installation)
- [Platform Capabilities](#platform-capabilities)
- [Basic Setup](#basic-setup)
- [Use Cases](#use-cases)
- [API Reference](#api-reference)
- [Platform-Specific Notes](#platform-specific-notes)
- [Troubleshooting](#troubleshooting)

## Installation

Add the plugin to your `pubspec.yaml`:

```yaml
dependencies:
  bluetooth_monitor:
    git:
      url: https://github.com/SyedAhmed334/bluetooth_monitor_plugin.git
    # Or for local development:
    # bluetooth_monitor:
    #   path: path/to/bluetooth_monitor_plugin
```

Run `flutter pub get` to install.

## Platform Capabilities

### Android
- ✅ Full Bluetooth Classic and BLE device monitoring
- ✅ Bluetooth on/off/turning on/turning off state detection
- ✅ Real-time connection/disconnection events
- ✅ Device type recognition (Headphones, Speaker, Phone, etc.)
- ✅ Battery level monitoring (limited support)
- ✅ RSSI (signal strength) monitoring (limited support)
- ❌ Connect/disconnect device control (disabled for security)

### iOS
- ✅ Audio device monitoring via AVAudioSession
- ✅ Bluetooth on/off state detection via Core Bluetooth
- ✅ Connection/disconnection events for audio devices
- ✅ Device type recognition for audio devices
- ❌ Battery/RSSI not available (iOS limitation)
- ❌ Non-audio devices not supported

## Basic Setup

```dart
import 'package:bluetooth_monitor/bluetooth_monitor.dart';

class BluetoothService {
  StreamSubscription<BluetoothEvent>? _subscription;

  void startMonitoring() {
    // Initialize the plugin
    BluetoothMonitor.startListening();
    
    // Listen to all events
    _subscription = BluetoothMonitor.events.listen((event) {
      print('Bluetooth event: ${event.type}');
    });
  }

  void stopMonitoring() {
    _subscription?.cancel();
    BluetoothMonitor.stopListening();
  }
}
```

## Use Cases

### 1. Audio App - Auto-pause when headphones disconnect

```dart
class AudioPlayerService {
  StreamSubscription<BluetoothEvent>? _connectionSubscription;
  
  void setupHeadphoneDetection() {
    BluetoothMonitor.startListening();
    
    _connectionSubscription = BluetoothMonitor.connectionEvents.listen((event) {
      if (_isAudioDevice(event.deviceType)) {
        if (event.type == BluetoothEventType.connected) {
          resumePlayback();
          showNotification('${event.deviceName} connected - Ready to play');
        } else if (event.type == BluetoothEventType.disconnected) {
          pausePlayback();
          showNotification('${event.deviceName} disconnected - Playback paused');
        }
      }
    });
  }
  
  bool _isAudioDevice(String? deviceType) {
    return deviceType != null && 
           (deviceType.contains('Headphones') || 
            deviceType.contains('Headset') || 
            deviceType.contains('Speaker'));
  }
}
```

### 2. Smart Home App - Device presence detection

```dart
class PresenceDetector {
  final Map<String, DateTime> _lastSeen = {};
  
  void setupPresenceMonitoring() {
    BluetoothMonitor.startListening();
    
    BluetoothMonitor.connectionEvents.listen((event) {
      if (event.type == BluetoothEventType.connected) {
        _userPresent(event.deviceName, event.deviceType);
      } else if (event.type == BluetoothEventType.disconnected) {
        _userLeft(event.deviceName);
      }
    });
    
    // Check for known devices periodically
    Timer.periodic(Duration(minutes: 1), (_) => _checkConnectedDevices());
  }
  
  void _userPresent(String? deviceName, String? deviceType) {
    print('User present - detected $deviceName ($deviceType)');
    _lastSeen[deviceName ?? 'Unknown'] = DateTime.now();
    
    // Trigger smart home actions
    turnOnLights();
    setTemperature(22);
  }
  
  void _userLeft(String? deviceName) {
    print('User may have left - $deviceName disconnected');
    
    // Start energy saving mode after 10 minutes
    Timer(Duration(minutes: 10), () {
      if (!_hasRecentActivity()) {
        enableEnergyMode();
      }
    });
  }
  
  Future<void> _checkConnectedDevices() async {
    final devices = await BluetoothMonitor.getConnectedDevices();
    if (devices.isEmpty) {
      enableAwayMode();
    }
  }
}
```

### 3. Battery Monitoring Dashboard

```dart
class BatteryMonitorApp extends StatefulWidget {
  @override
  _BatteryMonitorAppState createState() => _BatteryMonitorAppState();
}

class _BatteryMonitorAppState extends State<BatteryMonitorApp> {
  Map<String, int> deviceBatteries = {};
  
  @override
  void initState() {
    super.initState();
    _setupBatteryMonitoring();
  }
  
  void _setupBatteryMonitoring() {
    BluetoothMonitor.startListening();
    
    // Listen specifically for battery updates
    BluetoothMonitor.batteryEvents.listen((event) {
      setState(() {
        deviceBatteries[event.deviceName ?? 'Unknown'] = event.batteryLevel ?? 0;
      });
      
      // Send low battery notifications
      if ((event.batteryLevel ?? 100) < 20) {
        _sendLowBatteryAlert(event.deviceName, event.batteryLevel);
      }
    });
    
    // Load initial battery levels
    _loadInitialBatteries();
  }
  
  Future<void> _loadInitialBatteries() async {
    final devices = await BluetoothMonitor.getConnectedDevices();
    setState(() {
      for (final device in devices) {
        if (device.batteryLevel >= 0) {
          deviceBatteries[device.name] = device.batteryLevel;
        }
      }
    });
  }
  
  Widget build(BuildContext context) {
    return ListView.builder(
      itemCount: deviceBatteries.length,
      itemBuilder: (context, index) {
        final entry = deviceBatteries.entries.elementAt(index);
        return BatteryCard(
          deviceName: entry.key,
          batteryLevel: entry.value,
        );
      },
    );
  }
}
```

### 4. Fitness App - Workout device management

```dart
class WorkoutDeviceManager {
  Set<String> _workoutDevices = {};
  
  void startWorkout() {
    BluetoothMonitor.startListening();
    
    BluetoothMonitor.connectionEvents.listen((event) {
      if (_isWorkoutDevice(event.deviceType)) {
        if (event.type == BluetoothEventType.connected) {
          _addWorkoutDevice(event);
        } else if (event.type == BluetoothEventType.disconnected) {
          _removeWorkoutDevice(event);
        }
      }
    });
    
    _scanForWorkoutDevices();
  }
  
  bool _isWorkoutDevice(String? deviceType) {
    return deviceType != null && 
           (deviceType.contains('Headphones') ||
            deviceType.contains('Smartwatch') ||
            deviceType.contains('Heart Rate'));
  }
  
  void _addWorkoutDevice(BluetoothEvent event) {
    _workoutDevices.add(event.deviceAddress ?? '');
    showSnackbar('✅ ${event.deviceName} ready for workout');
    
    // Auto-start music if headphones connected
    if (event.deviceType?.contains('Headphones') == true) {
      startWorkoutPlaylist();
    }
  }
  
  Future<void> _scanForWorkoutDevices() async {
    final devices = await BluetoothMonitor.getConnectedDevices();
    for (final device in devices) {
      if (_isWorkoutDevice(device.deviceType)) {
        _workoutDevices.add(device.address);
      }
    }
  }
}
```

### 5. Car Integration - Auto-connect to car audio

```dart
class CarIntegrationService {
  String? _knownCarAddress;
  
  void setupCarIntegration() {
    BluetoothMonitor.startListening();
    
    BluetoothMonitor.connectionEvents.listen((event) {
      if (event.deviceType?.contains('Car Audio') == true) {
        if (event.type == BluetoothEventType.connected) {
          _carConnected(event);
        } else if (event.type == BluetoothEventType.disconnected) {
          _carDisconnected(event);
        }
      }
    });
  }
  
  void _carConnected(BluetoothEvent event) {
    _knownCarAddress = event.deviceAddress;
    
    // Auto-launch navigation
    launchNavigation();
    
    // Start driving mode
    enableDrivingMode();
    
    // Sync music library
    syncMusicToCarDisplay();
    
    print('🚗 Car connected: ${event.deviceName}');
  }
  
  void _carDisconnected(BluetoothEvent event) {
    // End driving session
    disableDrivingMode();
    
    // Save trip data
    saveTripSummary();
    
    print('🚗 Car disconnected: ${event.deviceName}');
  }
  
  Future<void> autoConnectToCar() async {
    if (_knownCarAddress != null) {
      final success = await BluetoothMonitor.connectDevice(_knownCarAddress!);
      if (success) {
        print('✅ Auto-connected to car');
      }
    }
  }
}
```

### 6. Device Management Dashboard

```dart
class DeviceManagerScreen extends StatefulWidget {
  @override
  _DeviceManagerScreenState createState() => _DeviceManagerScreenState();
}

class _DeviceManagerScreenState extends State<DeviceManagerScreen> {
  List<BluetoothDevice> _devices = [];
  String _bluetoothState = 'Unknown';
  
  @override
  void initState() {
    super.initState();
    _setupDeviceMonitoring();
  }
  
  void _setupDeviceMonitoring() {
    BluetoothMonitor.startListening();
    
    // Monitor all events for real-time updates
    BluetoothMonitor.events.listen((event) {
      switch (event.type) {
        case BluetoothEventType.connected:
        case BluetoothEventType.disconnected:
          _refreshDeviceList();
          break;
        case BluetoothEventType.bluetoothOn:
        case BluetoothEventType.bluetoothOff:
          setState(() {
            _bluetoothState = event.type.toString();
          });
          break;
        case BluetoothEventType.batteryLevel:
          _updateDeviceBattery(event);
          break;
      }
    });
    
    _loadInitialData();
  }
  
  Future<void> _loadInitialData() async {
    final state = await BluetoothMonitor.getBluetoothState();
    final devices = await BluetoothMonitor.getConnectedDevices();
    
    setState(() {
      _bluetoothState = state;
      _devices = devices;
    });
  }
  
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Device Manager'),
        actions: [
          IconButton(
            icon: Icon(Icons.refresh),
            onPressed: _refreshDeviceList,
          ),
        ],
      ),
      body: Column(
        children: [
          // Bluetooth Status Card
          Card(
            child: ListTile(
              leading: Icon(
                _bluetoothState.contains('ON') ? Icons.bluetooth : Icons.bluetooth_disabled,
                color: _bluetoothState.contains('ON') ? Colors.green : Colors.red,
              ),
              title: Text('Bluetooth Status'),
              subtitle: Text(_bluetoothState),
            ),
          ),
          
          // Connected Devices List
          Expanded(
            child: ListView.builder(
              itemCount: _devices.length,
              itemBuilder: (context, index) {
                final device = _devices[index];
                return DeviceTile(
                  device: device,
                  onDisconnect: () => _disconnectDevice(device),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
```

### 7. Gaming App - Controller detection

```dart
class GameControllerService {
  void setupControllerDetection() {
    BluetoothMonitor.startListening();
    
    BluetoothMonitor.connectionEvents.listen((event) {
      if (_isGameController(event.deviceType)) {
        if (event.type == BluetoothEventType.connected) {
          enableGameMode();
          showControllerConnectedUI();
        } else {
          disableGameMode();
          showTouchControlsUI();
        }
      }
    });
  }
  
  bool _isGameController(String? deviceType) {
    return deviceType?.toLowerCase().contains('controller') == true ||
           deviceType?.toLowerCase().contains('gamepad') == true;
  }
}
```

## API Reference

### Core Methods

```dart
// Start/Stop monitoring
BluetoothMonitor.startListening()
BluetoothMonitor.stopListening()

// Get current state
Future<String> BluetoothMonitor.getBluetoothState()
Future<List<BluetoothDevice>> BluetoothMonitor.getConnectedDevices()

// Device control
Future<bool> BluetoothMonitor.connectDevice(String address)
Future<bool> BluetoothMonitor.disconnectDevice(String address)
```

### Event Streams

```dart
// All events
Stream<BluetoothEvent> BluetoothMonitor.events

// Filtered streams
Stream<BluetoothEvent> BluetoothMonitor.connectionEvents  // connect/disconnect only
Stream<BluetoothEvent> BluetoothMonitor.stateEvents       // bluetooth on/off only
Stream<BluetoothEvent> BluetoothMonitor.batteryEvents     // battery updates only

// Custom filtering
Stream<BluetoothEvent> BluetoothMonitor.listenForEvents(List<BluetoothEventType> types)
```

### Event Types

```dart
enum BluetoothEventType {
  connected,           // Device connected
  disconnected,        // Device disconnected
  bluetoothOn,         // Bluetooth enabled
  bluetoothOff,        // Bluetooth disabled
  bluetoothTurningOn,  // Bluetooth turning on
  bluetoothTurningOff, // Bluetooth turning off
  batteryLevel,        // Battery level updated
  rssiUpdate,          // Signal strength updated
}
```

### Device Information

```dart
class BluetoothDevice {
  final String name;         // Device display name
  final String address;      // MAC address
  final int batteryLevel;    // 0-100, -1 if unavailable
  final int rssi;            // Signal strength in dBm, -999 if unavailable
  final String deviceType;   // "Headphones", "Speaker", "Phone", etc.
}
```

## Platform-Specific Notes

### Android Permissions

Add to `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- For Android 12+ -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
```

### iOS Configuration

Add to `ios/Runner/Info.plist`:

```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>This app uses Bluetooth to monitor audio device connections</string>
```

## Device Type Recognition

The plugin automatically detects device types:

| Device Type | Android | iOS | Description |
|-------------|---------|-----|-------------|
| Headphones | ✅ | ✅ | Over-ear headphones |
| Headset | ✅ | ✅ | Gaming/call headsets |
| AirPods | ✅ | ✅ | Apple wireless earbuds |
| Speaker | ✅ | ✅ | Bluetooth speakers |
| Car Audio | ✅ | ✅ | Vehicle audio systems |
| Phone | ✅ | ❌ | Mobile phones |
| Computer | ✅ | ❌ | Laptops/desktops |
| Smartwatch | ✅ | ❌ | Wearable devices |

## Advanced Usage Examples

### Real-time Signal Strength Monitoring

```dart
class SignalMonitor {
  void monitorSignalStrength() {
    BluetoothMonitor.events
        .where((event) => event.type == BluetoothEventType.rssiUpdate)
        .listen((event) {
      final signalStrength = _categorizeSignal(event.rssi);
      updateSignalIndicator(event.deviceName, signalStrength);
    });
  }
  
  String _categorizeSignal(int? rssi) {
    if (rssi == null || rssi == -999) return 'Unknown';
    if (rssi >= -50) return 'Excellent';
    if (rssi >= -70) return 'Good';
    if (rssi >= -85) return 'Fair';
    return 'Poor';
  }
}
```

### Multi-device Audio Switching

```dart
class AudioSwitcher {
  String? _primaryAudioDevice;
  
  void setupAudioSwitching() {
    BluetoothMonitor.connectionEvents.listen((event) {
      if (_isAudioDevice(event.deviceType)) {
        if (event.type == BluetoothEventType.connected) {
          _handleNewAudioDevice(event);
        } else if (event.type == BluetoothEventType.disconnected) {
          _handleAudioDeviceDisconnect(event);
        }
      }
    });
  }
  
  void _handleNewAudioDevice(BluetoothEvent event) {
    // Priority: AirPods > Headphones > Speaker
    final priority = _getAudioDevicePriority(event.deviceType);
    
    if (_primaryAudioDevice == null || 
        priority > _getAudioDevicePriority(_getDeviceType(_primaryAudioDevice!))) {
      _switchToPrimaryDevice(event.deviceAddress!);
      _primaryAudioDevice = event.deviceAddress;
    }
  }
  
  int _getAudioDevicePriority(String? deviceType) {
    if (deviceType?.contains('AirPods') == true) return 3;
    if (deviceType?.contains('Headphones') == true) return 2;
    if (deviceType?.contains('Speaker') == true) return 1;
    return 0;
  }
}
```

## Troubleshooting

### Common Issues

1. **Missing Bluetooth on/off events**: Use `BluetoothMonitor.events` instead of `BluetoothMonitor.connectionEvents` to receive all event types
2. **No events on iOS**: iOS only supports audio devices via AVAudioSession
3. **Permission errors on Android**: Ensure Bluetooth permissions are granted
4. **Battery shows -1**: Not all devices support battery reporting
5. **RSSI shows -999**: Some devices don't report signal strength

### Important: Listen to All Events

To receive Bluetooth on/off state changes, use the main events stream:

```dart
// ✅ Correct - receives ALL events including on/off
BluetoothMonitor.events.listen((event) {
  if (event.type == BluetoothEventType.bluetoothOn) {
    print('Bluetooth turned ON');
  } else if (event.type == BluetoothEventType.bluetoothOff) {
    print('Bluetooth turned OFF');
  } else if (event.type == BluetoothEventType.connected) {
    print('Device connected: ${event.deviceName}');
  }
});

// ❌ Incorrect - only receives connect/disconnect
BluetoothMonitor.connectionEvents.listen((event) {
  // Will NOT receive bluetoothOn/bluetoothOff events
});
```

### Debug Mode

```dart
void enableDebugMode() {
  BluetoothMonitor.events.listen((event) {
    debugPrint('Event: ${event.type}');
    debugPrint('Device: ${event.deviceName} (${event.deviceType})');
    debugPrint('Battery: ${event.batteryLevel}% | RSSI: ${event.rssi}dBm');
  });
}
```

### Testing Without Physical Devices

```dart
void testWithMockEvents() {
  // The plugin provides real device events only
  // For testing, use flutter_test with mocked method channels
  // See test/ directory for examples
}
```

## Best Practices

1. **Always call `startListening()`** before using any streams
2. **Clean up subscriptions** in `dispose()` methods
3. **Handle platform differences** - check device type availability
4. **Request permissions** at runtime for Android 12+
5. **Use specific event streams** for better performance
6. **Cache device information** to reduce platform channel calls

## Performance Tips

- Use filtered streams (`connectionEvents`, `batteryEvents`) instead of listening to all events
- Cache connected device lists and refresh only when needed
- Unsubscribe from unused streams to prevent memory leaks
- Batch device operations when possible

This plugin provides a robust foundation for any Bluetooth-aware Flutter application with cross-platform compatibility and comprehensive device monitoring capabilities.