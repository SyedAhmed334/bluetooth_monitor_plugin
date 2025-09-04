import 'package:flutter_test/flutter_test.dart';
import 'package:bluetooth_monitor/bluetooth_monitor.dart';

void main() {
  setUpAll(() {
    TestWidgetsFlutterBinding.ensureInitialized();
  });

  test('BluetoothMonitor can be instantiated', () {
    expect(() => BluetoothMonitor.startListening(), returnsNormally);
  });

  test('Event streams are available', () {
    expect(BluetoothMonitor.events, isNotNull);
    expect(BluetoothMonitor.connectionEvents, isNotNull);
    expect(BluetoothMonitor.stateEvents, isNotNull);
    expect(BluetoothMonitor.batteryEvents, isNotNull);
  });
}
