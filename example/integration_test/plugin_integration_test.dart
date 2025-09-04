// This is a basic Flutter integration test.
//
// Since integration tests run in a full Flutter application, they can interact
// with the host side of a plugin implementation, unlike Dart unit tests.
//
// For more information about Flutter integration tests, please see
// https://flutter.dev/to/integration-testing


import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'package:bluetooth_monitor/bluetooth_monitor.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Bluetooth Monitor basic test', (WidgetTester tester) async {
    // Test that the plugin initializes without errors
    expect(() => BluetoothMonitor.startListening(), returnsNormally);
    
    // Test that streams are available
    expect(BluetoothMonitor.events, isNotNull);
    expect(BluetoothMonitor.connectionEvents, isNotNull);
    
    BluetoothMonitor.stopListening();
  });
}
