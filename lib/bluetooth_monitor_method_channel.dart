import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'bluetooth_monitor_platform_interface.dart';

/// An implementation of [BluetoothMonitorPlatform] that uses method channels.
class MethodChannelBluetoothMonitor extends BluetoothMonitorPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('bluetooth_monitor');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
