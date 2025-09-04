class BluetoothDevice {
  final String name;
  final String address;
  final int batteryLevel;
  final int rssi;
  final String deviceType;

  const BluetoothDevice({
    required this.name,
    required this.address,
    required this.batteryLevel,
    required this.rssi,
    required this.deviceType,
  });

  factory BluetoothDevice.fromMap(Map<String, dynamic> map) {
    return BluetoothDevice(
      name: map['name'] ?? 'Unknown Device',
      address: map['address'] ?? '',
      batteryLevel: map['batteryLevel'] ?? -1,
      rssi: map['rssi'] ?? -999,
      deviceType: map['deviceType'] ?? 'Unknown Device',
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'name': name,
      'address': address,
      'batteryLevel': batteryLevel,
      'rssi': rssi,
      'deviceType': deviceType,
    };
  }

  @override
  String toString() {
    return 'BluetoothDevice(name: $name, address: $address, type: $deviceType, battery: $batteryLevel%, rssi: ${rssi}dBm)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is BluetoothDevice && other.address == address;
  }

  @override
  int get hashCode => address.hashCode;
}