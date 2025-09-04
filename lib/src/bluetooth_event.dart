enum BluetoothEventType {
  connected,
  disconnected,
  bluetoothOn,
  bluetoothOff,
  bluetoothTurningOn,
  bluetoothTurningOff,
  batteryLevel,
  rssiUpdate,
}

class BluetoothEvent {
  final BluetoothEventType type;
  final String? deviceName;
  final String? deviceAddress;
  final String? deviceType;
  final int? batteryLevel;
  final int? rssi;

  const BluetoothEvent({
    required this.type,
    this.deviceName,
    this.deviceAddress,
    this.deviceType,
    this.batteryLevel,
    this.rssi,
  });

  factory BluetoothEvent.fromMap(Map<String, dynamic> map) {
    final eventString = map['event'] as String?;
    BluetoothEventType type;
    
    switch (eventString) {
      case 'CONNECTED':
        type = BluetoothEventType.connected;
        break;
      case 'DISCONNECTED':
        type = BluetoothEventType.disconnected;
        break;
      case 'BLUETOOTH_ON':
        type = BluetoothEventType.bluetoothOn;
        break;
      case 'BLUETOOTH_OFF':
        type = BluetoothEventType.bluetoothOff;
        break;
      case 'BLUETOOTH_TURNING_ON':
        type = BluetoothEventType.bluetoothTurningOn;
        break;
      case 'BLUETOOTH_TURNING_OFF':
        type = BluetoothEventType.bluetoothTurningOff;
        break;
      case 'BATTERY_LEVEL':
        type = BluetoothEventType.batteryLevel;
        break;
      case 'RSSI_UPDATE':
        type = BluetoothEventType.rssiUpdate;
        break;
      default:
        throw ArgumentError('Unknown event type: $eventString');
    }

    return BluetoothEvent(
      type: type,
      deviceName: map['name'],
      deviceAddress: map['address'],
      deviceType: map['deviceType'],
      batteryLevel: map['batteryLevel'],
      rssi: map['rssi'],
    );
  }

  @override
  String toString() {
    switch (type) {
      case BluetoothEventType.connected:
        return 'BluetoothEvent.connected($deviceName, $deviceType)';
      case BluetoothEventType.disconnected:
        return 'BluetoothEvent.disconnected($deviceAddress)';
      case BluetoothEventType.batteryLevel:
        return 'BluetoothEvent.batteryLevel($deviceName: $batteryLevel%)';
      case BluetoothEventType.rssiUpdate:
        return 'BluetoothEvent.rssiUpdate($deviceName: ${rssi}dBm)';
      default:
        return 'BluetoothEvent.$type';
    }
  }
}