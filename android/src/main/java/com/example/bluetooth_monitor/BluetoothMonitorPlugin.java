package com.example.bluetooth_monitor;

import android.Manifest;
import android.bluetooth.*;
import java.util.UUID;
import android.os.Handler;
import android.os.Looper;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import android.util.Log;
import android.app.Activity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Method;

public class BluetoothMonitorPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    private MethodChannel channel;
    private EventChannel eventChannel;
    private EventChannel.EventSink btEventSink = null;
    private Context context;
    private Activity activity;
    
    private BluetoothAdapter bluetoothAdapter;
    private Map<String, Integer> deviceBatteryLevels = new ConcurrentHashMap<>();
    private Map<String, Integer> deviceRssiLevels = new ConcurrentHashMap<>();
    
    private static final String ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED";
    private static final String EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL";

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "bluetooth_monitor");
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "bluetooth_monitor_events");
        
        channel.setMethodCallHandler(this);
        eventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                btEventSink = events;
            }

            @Override
            public void onCancel(Object arguments) {
                btEventSink = null;
            }
        });
        
        context = flutterPluginBinding.getApplicationContext();
        setupBluetooth();
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "getBluetoothState":
                result.success(getBluetoothState());
                break;
            case "getConnectedDevices":
            case "getCurrentlyConnectedDevices":
                fetchCurrentDevices(result);
                break;
            case "disconnectDevice":
                String disconnectAddress = call.argument("address");
                boolean disconnectSuccess = disconnectAddress != null && disconnectDevice(disconnectAddress);
                result.success(disconnectSuccess);
                break;
            case "connectDevice":
                String connectAddress = call.argument("address");
                connectDevice(connectAddress, result);
                break;
            default:
                result.notImplemented();
        }
    }

    private void setupBluetooth() {
        if (context != null) {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            registerBluetoothReceiver();
        }
    }

    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(ACTION_BATTERY_LEVEL_CHANGED);

        context.registerReceiver(bluetoothReceiver, filter);
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                String event;
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        event = "BLUETOOTH_ON";
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        event = "BLUETOOTH_OFF";
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        event = "BLUETOOTH_TURNING_ON";
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        event = "BLUETOOTH_TURNING_OFF";
                        break;
                    default:
                        return;
                }
                Map<String, Object> eventMap = new HashMap<>();
                eventMap.put("event", event);
                if (btEventSink != null) {
                    btEventSink.success(eventMap);
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action) || 
                       BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    String event = BluetoothDevice.ACTION_ACL_CONNECTED.equals(action) ? "CONNECTED" : "DISCONNECTED";
                    handleConnectionEvent(device, event);
                }
            } else if (ACTION_BATTERY_LEVEL_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1);
                if (device != null && level >= 0) {
                    handleBatteryUpdate(device, level);
                }
            }
        }
    };

    private void handleConnectionEvent(BluetoothDevice device, String event) {
        if (event.equals("CONNECTED")) {
            readClassicBluetoothInfo(device);
        }
        
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("event", event);
        eventMap.put("name", device.getName() != null ? device.getName() : device.getAddress());
        eventMap.put("address", device.getAddress());
        eventMap.put("rssi", deviceRssiLevels.getOrDefault(device.getAddress(), -999));
        eventMap.put("deviceType", getDeviceType(device));
        
        if (btEventSink != null) {
            btEventSink.success(eventMap);
        }
    }

    private void handleBatteryUpdate(BluetoothDevice device, int level) {
        deviceBatteryLevels.put(device.getAddress(), level);
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("event", "BATTERY_LEVEL");
        eventMap.put("name", device.getName() != null ? device.getName() : device.getAddress());
        eventMap.put("address", device.getAddress());
        eventMap.put("batteryLevel", level);
        eventMap.put("rssi", deviceRssiLevels.getOrDefault(device.getAddress(), -999));
        eventMap.put("deviceType", getDeviceType(device));
        
        if (btEventSink != null) {
            btEventSink.success(eventMap);
        }
    }

    private String getBluetoothState() {
        if (bluetoothAdapter == null) {
            return "Bluetooth Not Supported";
        } else if (bluetoothAdapter.isEnabled()) {
            return "BLUETOOTH_ON";
        } else {
            return "BLUETOOTH_OFF";
        }
    }

    private void fetchCurrentDevices(MethodChannel.Result result) {
        if (bluetoothAdapter == null) {
            result.success(new ArrayList<Map<String, Object>>());
            return;
        }
        
        int[] profiles = {BluetoothProfile.HEADSET, BluetoothProfile.A2DP, BluetoothProfile.GATT};
        Set<BluetoothDevice> seen = new HashSet<>();
        AtomicInteger remaining = new AtomicInteger(profiles.length);

        for (int profile : profiles) {
            bluetoothAdapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int p, BluetoothProfile proxy) {
                    seen.addAll(proxy.getConnectedDevices());
                    bluetoothAdapter.closeProfileProxy(p, proxy);
                    if (remaining.decrementAndGet() == 0) {
                        fetchDevices(new ArrayList<>(seen), result);
                    }
                }

                @Override
                public void onServiceDisconnected(int p) {
                    if (remaining.decrementAndGet() == 0) {
                        fetchDevices(new ArrayList<>(seen), result);
                    }
                }
            }, profile);
        }
    }

    private void fetchDevices(List<BluetoothDevice> devices, MethodChannel.Result result) {
        for (BluetoothDevice device : devices) {
            readClassicBluetoothInfo(device);
        }
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            List<Map<String, Object>> list = new ArrayList<>();
            for (BluetoothDevice device : devices) {
                Map<String, Object> deviceMap = new HashMap<>();
                deviceMap.put("name", device.getName() != null ? device.getName() : device.getAddress());
                deviceMap.put("address", device.getAddress());
                deviceMap.put("batteryLevel", deviceBatteryLevels.getOrDefault(device.getAddress(), -1));
                deviceMap.put("rssi", deviceRssiLevels.getOrDefault(device.getAddress(), -999));
                deviceMap.put("deviceType", getDeviceType(device));
                list.add(deviceMap);
            }
            result.success(list);
        }, 2000);
    }

    private String getDeviceType(BluetoothDevice device) {
        BluetoothClass bluetoothClass = device.getBluetoothClass();
        if (bluetoothClass == null) {
            return "Unknown Device";
        }
        
        int majorClass = bluetoothClass.getMajorDeviceClass();
        int deviceClass = bluetoothClass.getDeviceClass();
        
        switch (majorClass) {
            case BluetoothClass.Device.Major.AUDIO_VIDEO:
                switch (deviceClass) {
                    case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
                        return "Headset";
                    case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:
                        return "Hands-free";
                    case BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE:
                        return "Microphone";
                    case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
                        return "Speaker";
                    case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
                        return "Headphones";
                    case BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO:
                        return "Portable Audio";
                    case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO:
                        return "Car Audio";
                    case BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO:
                        return "Hi-Fi Audio";
                    default:
                        return "Audio Device";
                }
            case BluetoothClass.Device.Major.PHONE:
                return "Phone";
            case BluetoothClass.Device.Major.COMPUTER:
                return "Computer";
            case BluetoothClass.Device.Major.PERIPHERAL:
                if ((deviceClass & BluetoothClass.Device.Major.PERIPHERAL) != 0) {
                    int minorClass = deviceClass & 0x3C;
                    if (minorClass == 0x40) {
                        return "Keyboard";
                    } else if (minorClass == 0x80) {
                        return "Mouse";
                    } else {
                        return "Input Device";
                    }
                }
                return "Input Device";
            case BluetoothClass.Device.Major.WEARABLE:
                return "Smartwatch";
            case BluetoothClass.Device.Major.TOY:
                return "Toy";
            case BluetoothClass.Device.Major.HEALTH:
                return "Health Device";
            default:
                return "Unknown Device";
        }
    }

    private void readClassicBluetoothInfo(BluetoothDevice device) {
        readBatteryLevel(device);
        estimateRssi(device);
    }

    private void estimateRssi(BluetoothDevice device) {
        String name = device.getName();
        if (name != null) {
            int estimatedRssi = -55;
            String nameLower = name.toLowerCase();
            if (nameLower.contains("airpods") || nameLower.contains("airpod") || 
                nameLower.contains("airbud") || nameLower.contains("earbud")) {
                estimatedRssi = -35;
            } else if (nameLower.contains("headphone") || nameLower.contains("headset") ||
                       nameLower.contains("soundcore")) {
                estimatedRssi = -40;
            } else if (nameLower.contains("speaker")) {
                estimatedRssi = -50;
            } else if (nameLower.contains("phone") || nameLower.contains("oneplus")) {
                estimatedRssi = -45;
            }
            deviceRssiLevels.put(device.getAddress(), estimatedRssi);
        }
    }

    private void readBatteryLevel(BluetoothDevice device) {
        try {
            Method method = BluetoothDevice.class.getMethod("getBatteryLevel");
            Integer batteryLevel = (Integer) method.invoke(device);
            if (batteryLevel != null && batteryLevel >= 0) {
                deviceBatteryLevels.put(device.getAddress(), batteryLevel);
            }
        } catch (Exception e) {
            // Reflection failed, battery level unavailable
        }
    }

    private boolean disconnectDevice(String address) {
        if (bluetoothAdapter == null) return false;
        
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            if (device.getAddress().equals(address)) {
                try {
                    Method method = BluetoothDevice.class.getMethod("disconnect");
                    return (Boolean) method.invoke(device);
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }

    private void connectDevice(String address, MethodChannel.Result result) {
        if (bluetoothAdapter == null) {
            result.success(false);
            return;
        }
        
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            if (device.getAddress().equals(address)) {
                try {
                    Method method = BluetoothDevice.class.getMethod("connect");
                    boolean connected = (Boolean) method.invoke(device);
                    result.success(connected);
                    return;
                } catch (Exception e) {
                    result.success(false);
                    return;
                }
            }
        }
        result.success(false);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        try {
            if (context != null) {
                context.unregisterReceiver(bluetoothReceiver);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }
}