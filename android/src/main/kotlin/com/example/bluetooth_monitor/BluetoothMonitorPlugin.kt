package com.example.bluetooth_monitor

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class BluetoothMonitorPlugin: FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {
    private lateinit var context: Context
    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var eventSink: EventChannel.EventSink? = null
    
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothReceiver: BroadcastReceiver? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        
        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "bluetooth_monitor")
        methodChannel.setMethodCallHandler(this)
        
        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "bluetooth_monitor_events")
        eventChannel.setStreamHandler(this)
        
        setupBluetooth()
    }

    private fun setupBluetooth() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        handleBluetoothStateChange(state)
                    }
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        device?.let { handleDeviceConnected(it) }
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        device?.let { handleDeviceDisconnected(it) }
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        
        context.registerReceiver(bluetoothReceiver, filter)
    }

    private fun handleBluetoothStateChange(state: Int) {
        val eventType = when (state) {
            BluetoothAdapter.STATE_ON -> "BLUETOOTH_ON"
            BluetoothAdapter.STATE_OFF -> "BLUETOOTH_OFF"
            BluetoothAdapter.STATE_TURNING_ON -> "BLUETOOTH_TURNING_ON"
            BluetoothAdapter.STATE_TURNING_OFF -> "BLUETOOTH_TURNING_OFF"
            else -> "BLUETOOTH_STATE_CHANGED"
        }
        
        val eventData = mapOf(
            "event" to eventType,
            "state" to getBluetoothStateString()
        )
        
        eventSink?.success(eventData)
    }

    private fun handleDeviceConnected(device: BluetoothDevice) {
        val deviceType = getDeviceType(device)
        val eventData = mapOf(
            "event" to "CONNECTED",
            "name" to (device.name ?: "Unknown Device"),
            "address" to device.address,
            "deviceType" to deviceType,
            "rssi" to -50,
            "batteryLevel" to -1
        )
        
        eventSink?.success(eventData)
    }

    private fun handleDeviceDisconnected(device: BluetoothDevice) {
        val deviceType = getDeviceType(device)
        val eventData = mapOf(
            "event" to "DISCONNECTED",
            "name" to (device.name ?: "Unknown Device"),
            "address" to device.address,
            "deviceType" to deviceType
        )
        
        eventSink?.success(eventData)
    }

    private fun getDeviceType(device: BluetoothDevice): String {
        val bluetoothClass = device.bluetoothClass?.majorDeviceClass
        return when (bluetoothClass) {
            0x0400 -> "Headphones" // AUDIO_VIDEO
            0x0200 -> "Phone"      // PHONE
            0x0100 -> "Computer"   // COMPUTER
            else -> {
                // Check device name for more specific classification
                val name = device.name?.lowercase() ?: ""
                when {
                    name.contains("airpods") || name.contains("headphone") || name.contains("headset") -> "Headphones"
                    name.contains("speaker") -> "Speaker"
                    name.contains("car") -> "Car Audio"
                    else -> "Unknown"
                }
            }
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getBluetoothState" -> {
                result.success(getBluetoothStateString())
            }
            "getConnectedDevices", "getCurrentlyConnectedDevices" -> {
                getConnectedDevices(result)
            }
            "disconnectDevice" -> {
                result.success(false) // Not implemented for security
            }
            "connectDevice" -> {
                result.success(false) // Not implemented for security
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun getBluetoothStateString(): String {
        return when (bluetoothAdapter?.state) {
            BluetoothAdapter.STATE_ON -> "BLUETOOTH_ON"
            BluetoothAdapter.STATE_OFF -> "BLUETOOTH_OFF"
            BluetoothAdapter.STATE_TURNING_ON -> "BLUETOOTH_TURNING_ON"
            BluetoothAdapter.STATE_TURNING_OFF -> "BLUETOOTH_TURNING_OFF"
            else -> "BLUETOOTH_UNKNOWN"
        }
    }

    private fun getConnectedDevices(result: Result) {
        try {
            val connectedDevices = bluetoothAdapter?.bondedDevices?.filter { device ->
                // This is a simplified check - ideally we'd check actual connection state
                // but that requires more complex permission handling
                device.name != null
            }?.map { device ->
                mapOf(
                    "name" to (device.name ?: "Unknown Device"),
                    "address" to device.address,
                    "deviceType" to getDeviceType(device),
                    "batteryLevel" to -1,
                    "rssi" to -50
                )
            } ?: emptyList()
            
            result.success(connectedDevices)
        } catch (e: SecurityException) {
            result.success(emptyList<Map<String, Any>>())
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        bluetoothReceiver?.let { context.unregisterReceiver(it) }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }
}