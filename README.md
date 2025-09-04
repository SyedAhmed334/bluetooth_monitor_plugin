# Bluetooth Monitor Plugin

A cross-platform Flutter plugin for monitoring Bluetooth device connections, disconnections, and device information.

## Features

- ✅ **Connection/Disconnection Listeners**: Real-time events when devices connect or disconnect
- ✅ **Device Type Recognition**: Automatic detection of device types (Headphones, Speaker, Phone, etc.)
- ✅ **Device Information**: Get connected device details (name, address, battery, signal strength)
- ✅ **Cross-Platform**: Works on both Android and iOS with platform-specific optimizations
- ✅ **Real-time Events**: Stream-based API for reactive programming

## Quick Start

```dart
import 'package:bluetooth_monitor/bluetooth_monitor.dart';

// Start listening
BluetoothMonitor.startListening();

// Monitor connections
BluetoothMonitor.connectionEvents.listen((event) {
  if (event.type == BluetoothEventType.connected) {
    print('${event.deviceName} connected (${event.deviceType})');
  }
});

// Get connected devices
final devices = await BluetoothMonitor.getConnectedDevices();
```

## Platform Differences

**Android**: Full Bluetooth Classic + BLE support with battery/RSSI data  
**iOS**: Audio device monitoring via AVAudioSession (connected devices only)

See example app for complete implementation.

