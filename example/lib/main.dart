import 'package:flutter/material.dart';
import 'dart:async';
import 'package:bluetooth_monitor/bluetooth_monitor.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _bluetoothState = 'Unknown';
  List<BluetoothDevice> _connectedDevices = [];
  final List<BluetoothEvent> _recentEvents = [];
  StreamSubscription<BluetoothEvent>? _eventSubscription;

  @override
  void initState() {
    super.initState();
    _initBluetooth();
  }

  Future<void> _initBluetooth() async {
    // Start listening to Bluetooth events
    BluetoothMonitor.startListening();
    
    // Listen to all events and update UI
    _eventSubscription = BluetoothMonitor.events.listen((event) {
      setState(() {
        _recentEvents.insert(0, event);
        if (_recentEvents.length > 10) {
          _recentEvents.removeLast();
        }
      });
    });
    
    // Get initial state and devices
    final state = await BluetoothMonitor.getBluetoothState();
    final devices = await BluetoothMonitor.getConnectedDevices();
    
    setState(() {
      _bluetoothState = state;
      _connectedDevices = devices;
    });
  }

  @override
  void dispose() {
    _eventSubscription?.cancel();
    BluetoothMonitor.stopListening();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Bluetooth Monitor Example',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Bluetooth Monitor Example'),
          actions: [
            IconButton(
              icon: const Icon(Icons.refresh),
              onPressed: () async {
                final devices = await BluetoothMonitor.getConnectedDevices();
                setState(() {
                  _connectedDevices = devices;
                });
              },
            ),
          ],
        ),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Bluetooth State
              Card(
                child: ListTile(
                  leading: Icon(
                    _bluetoothState.contains('ON') ? Icons.bluetooth : Icons.bluetooth_disabled,
                    color: _bluetoothState.contains('ON') ? Colors.green : Colors.red,
                  ),
                  title: const Text('Bluetooth State'),
                  subtitle: Text(_bluetoothState),
                ),
              ),
              
              const SizedBox(height: 16),
              
              // Connected Devices
              Text(
                'Connected Devices (${_connectedDevices.length})',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 8),
              
              Expanded(
                flex: 2,
                child: _connectedDevices.isEmpty
                    ? const Card(
                        child: Center(
                          child: Text('No devices connected'),
                        ),
                      )
                    : ListView.builder(
                        itemCount: _connectedDevices.length,
                        itemBuilder: (context, index) {
                          final device = _connectedDevices[index];
                          return Card(
                            child: ListTile(
                              leading: _getDeviceIcon(device.deviceType),
                              title: Text(device.name),
                              subtitle: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text('Type: ${device.deviceType}'),
                                  Text('Address: ${device.address}'),
                                  if (device.batteryLevel >= 0)
                                    Text('Battery: ${device.batteryLevel}%'),
                                  if (device.rssi != -999)
                                    Text('Signal: ${device.rssi}dBm'),
                                ],
                              ),
                              trailing: IconButton(
                                icon: const Icon(Icons.close),
                                onPressed: () async {
                                  final success = await BluetoothMonitor.disconnectDevice(device.address);
                                  if (!context.mounted) return;
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    SnackBar(
                                      content: Text(success 
                                          ? 'Disconnected from ${device.name}'
                                          : 'Failed to disconnect from ${device.name}'),
                                      backgroundColor: success ? Colors.green : Colors.red,
                                    ),
                                  );
                                },
                              ),
                            ),
                          );
                        },
                      ),
              ),
              
              const SizedBox(height: 16),
              
              // Recent Events
              Text(
                'Recent Events',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 8),
              
              Expanded(
                flex: 1,
                child: _recentEvents.isEmpty
                    ? const Card(
                        child: Center(
                          child: Text('No recent events'),
                        ),
                      )
                    : ListView.builder(
                        itemCount: _recentEvents.length,
                        itemBuilder: (context, index) {
                          final event = _recentEvents[index];
                          return Card(
                            child: ListTile(
                              leading: _getEventIcon(event.type),
                              title: Text(_getEventTitle(event)),
                              subtitle: Text(_getEventSubtitle(event)),
                            ),
                          );
                        },
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _getDeviceIcon(String deviceType) {
    switch (deviceType.toLowerCase()) {
      case 'headphones':
      case 'headset':
      case 'airpods':
        return const Icon(Icons.headphones, color: Colors.blue);
      case 'speaker':
        return const Icon(Icons.speaker, color: Colors.orange);
      case 'phone':
        return const Icon(Icons.phone_android, color: Colors.green);
      case 'car audio':
        return const Icon(Icons.directions_car, color: Colors.purple);
      default:
        return const Icon(Icons.bluetooth, color: Colors.grey);
    }
  }

  Icon _getEventIcon(BluetoothEventType type) {
    switch (type) {
      case BluetoothEventType.connected:
        return const Icon(Icons.bluetooth_connected, color: Colors.green);
      case BluetoothEventType.disconnected:
        return const Icon(Icons.bluetooth_disabled, color: Colors.red);
      case BluetoothEventType.bluetoothOn:
        return const Icon(Icons.bluetooth, color: Colors.green);
      case BluetoothEventType.bluetoothOff:
        return const Icon(Icons.bluetooth_disabled, color: Colors.red);
      case BluetoothEventType.batteryLevel:
        return const Icon(Icons.battery_charging_full, color: Colors.orange);
      case BluetoothEventType.rssiUpdate:
        return const Icon(Icons.signal_cellular_alt, color: Colors.blue);
      default:
        return const Icon(Icons.info, color: Colors.grey);
    }
  }

  String _getEventTitle(BluetoothEvent event) {
    switch (event.type) {
      case BluetoothEventType.connected:
        return 'Device Connected';
      case BluetoothEventType.disconnected:
        return 'Device Disconnected';
      case BluetoothEventType.bluetoothOn:
        return 'Bluetooth Enabled';
      case BluetoothEventType.bluetoothOff:
        return 'Bluetooth Disabled';
      case BluetoothEventType.batteryLevel:
        return 'Battery Update';
      case BluetoothEventType.rssiUpdate:
        return 'Signal Update';
      default:
        return 'Bluetooth Event';
    }
  }

  String _getEventSubtitle(BluetoothEvent event) {
    switch (event.type) {
      case BluetoothEventType.connected:
      case BluetoothEventType.disconnected:
        return '${event.deviceName ?? 'Unknown'} (${event.deviceType ?? 'Unknown'})';
      case BluetoothEventType.batteryLevel:
        return '${event.deviceName}: ${event.batteryLevel}%';
      case BluetoothEventType.rssiUpdate:
        return '${event.deviceName}: ${event.rssi}dBm';
      default:
        return 'Bluetooth state changed';
    }
  }
}
