package com.wp.bt.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 蓝牙管理类
 * 负责蓝牙设备的扫描、连接、数据收发
 * 使用经典蓝牙SPP协议进行串口通信
 */
public class BluetoothManager {
    
    private static final String TAG = "BluetoothManager";
    
    // SPP UUID - 标准串口服务UUID
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    // 权限请求码
    public static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;
    public static final int REQUEST_ENABLE_BT = 1002;
    
    // 数据结束标记
    private static final String DATA_END_MARKER = "FFDD";
    
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice connectedDevice;
    
    private InputStream inputStream;
    private OutputStream outputStream;
    
    private ConnectThread connectThread;
    private ReadThread readThread;
    
    private Handler mainHandler;
    private StringBuilder dataBuffer;
    
    private BluetoothCallback callback;
    
    private boolean isConnected = false;
    private boolean isReading = false;
    
    /**
     * 蓝牙回调接口
     */
    public interface BluetoothCallback {
        void onDeviceFound(BluetoothDevice device);
        void onDeviceConnected(BluetoothDevice device);
        void onDeviceDisconnected();
        void onDataReceived(String data);
        void onError(String message);
        void onScanFinished();
    }
    
    public BluetoothManager(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.dataBuffer = new StringBuilder();
    }
    
    public void setCallback(BluetoothCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 检查设备是否支持蓝牙
     */
    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }
    
    /**
     * 检查蓝牙是否已启用
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }
    
    /**
     * 请求启用蓝牙
     */
    @SuppressLint("MissingPermission")
    public void requestEnableBluetooth(Activity activity) {
        if (!isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }
    
    /**
     * 检查并请求蓝牙权限
     */
    public boolean checkAndRequestPermissions(Activity activity) {
        List<String> permissionsNeeded = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12及以上需要新的蓝牙权限
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        }
        
        // 位置权限 (蓝牙扫描需要)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_BLUETOOTH_PERMISSIONS);
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查是否有蓝牙权限
     */
    public boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                            == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 获取已配对的设备列表
     */
    @SuppressLint("MissingPermission")
    public List<BluetoothDevice> getPairedDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        if (bluetoothAdapter != null && hasBluetoothPermissions()) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices != null) {
                devices.addAll(pairedDevices);
            }
        }
        return devices;
    }
    
    /**
     * 开始扫描蓝牙设备
     */
    @SuppressLint("MissingPermission")
    public void startScan() {
        if (bluetoothAdapter == null || !hasBluetoothPermissions()) {
            if (callback != null) {
                callback.onError("蓝牙不可用或权限不足");
            }
            return;
        }
        
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(discoveryReceiver, filter);
        
        // 如果正在扫描，先取消
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        
        // 开始扫描
        bluetoothAdapter.startDiscovery();
        Log.d(TAG, "开始扫描蓝牙设备");
    }
    
    /**
     * 停止扫描
     */
    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        try {
            context.unregisterReceiver(discoveryReceiver);
        } catch (IllegalArgumentException e) {
            // 接收器未注册
        }
    }
    
    /**
     * 蓝牙设备发现广播接收器
     */
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && callback != null) {
                    callback.onDeviceFound(device);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (callback != null) {
                    callback.onScanFinished();
                }
            }
        }
    };
    
    /**
     * 连接到指定设备
     */
    @SuppressLint("MissingPermission")
    public void connect(BluetoothDevice device) {
        if (device == null) {
            if (callback != null) {
                callback.onError("设备为空");
            }
            return;
        }
        
        // 停止扫描
        stopScan();
        
        // 断开现有连接
        disconnect();
        
        // 启动连接线程
        connectThread = new ConnectThread(device);
        connectThread.start();
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        isReading = false;
        isConnected = false;
        
        if (readThread != null) {
            readThread.cancel();
            readThread = null;
        }
        
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "关闭输入流失败", e);
        }
        
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "关闭输出流失败", e);
        }
        
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "关闭Socket失败", e);
        }
        
        connectedDevice = null;
        dataBuffer.setLength(0);
    }
    
    /**
     * 发送数据
     */
    public boolean sendData(String data) {
        if (!isConnected || outputStream == null) {
            if (callback != null) {
                callback.onError("未连接设备");
            }
            return false;
        }
        
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
            Log.d(TAG, "发送数据: " + data);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "发送数据失败", e);
            if (callback != null) {
                mainHandler.post(() -> callback.onError("发送数据失败: " + e.getMessage()));
            }
            return false;
        }
    }
    
    /**
     * 发送控制指令
     * 格式: TODEVICEDATA##KEY##VALUE##
     */
    public boolean sendCommand(String key, int value) {
        String command = "TODEVICEDATA##" + key + "##" + value + "##";
        return sendData(command);
    }
    
    /**
     * 是否已连接
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * 获取已连接的设备
     */
    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stopScan();
        disconnect();
    }
    
    /**
     * 连接线程
     */
    @SuppressLint("MissingPermission")
    private class ConnectThread extends Thread {
        private final BluetoothDevice device;
        private BluetoothSocket socket;
        
        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;
            
            try {
                // 创建SPP Socket
                tmp = device.createRfcommSocketToServiceRecord(SPP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "创建Socket失败", e);
            }
            socket = tmp;
        }
        
        @Override
        public void run() {
            // 取消扫描以提高连接速度
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            
            try {
                // 连接到设备
                socket.connect();
                
                // 连接成功
                bluetoothSocket = socket;
                connectedDevice = device;
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                isConnected = true;
                
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onDeviceConnected(device);
                    }
                });
                
                // 启动读取线程
                readThread = new ReadThread();
                readThread.start();
                
            } catch (IOException e) {
                Log.e(TAG, "连接失败", e);
                
                // 尝试使用反射方法连接 (兼容某些设备)
                try {
                    socket = (BluetoothSocket) device.getClass()
                            .getMethod("createRfcommSocket", int.class)
                            .invoke(device, 1);
                    socket.connect();
                    
                    bluetoothSocket = socket;
                    connectedDevice = device;
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                    isConnected = true;
                    
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onDeviceConnected(device);
                        }
                    });
                    
                    readThread = new ReadThread();
                    readThread.start();
                    
                } catch (Exception e2) {
                    Log.e(TAG, "备用连接方式也失败", e2);
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onError("连接失败: " + e.getMessage());
                        }
                    });
                    cancel();
                }
            }
        }
        
        public void cancel() {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "关闭Socket失败", e);
            }
        }
    }
    
    /**
     * 数据读取线程
     */
    private class ReadThread extends Thread {
        private volatile boolean running = true;
        
        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            isReading = true;
            
            while (running && isConnected) {
                try {
                    if (inputStream != null && inputStream.available() > 0) {
                        bytes = inputStream.read(buffer);
                        if (bytes > 0) {
                            String receivedData = new String(buffer, 0, bytes);
                            processReceivedData(receivedData);
                        }
                    } else {
                        // 短暂休眠避免CPU占用过高
                        Thread.sleep(50);
                    }
                } catch (IOException e) {
                    if (running) {
                        Log.e(TAG, "读取数据失败", e);
                        mainHandler.post(() -> {
                            isConnected = false;
                            if (callback != null) {
                                callback.onDeviceDisconnected();
                            }
                        });
                    }
                    break;
                } catch (InterruptedException e) {
                    break;
                }
            }
            isReading = false;
        }
        
        public void cancel() {
            running = false;
        }
    }
    
    /**
     * 处理接收到的数据
     * 数据以 FFDD 结尾表示一条完整消息
     */
    private void processReceivedData(String data) {
        dataBuffer.append(data);
        
        // 检查是否包含结束标记
        String bufferStr = dataBuffer.toString();
        int endIndex;
        
        while ((endIndex = bufferStr.indexOf(DATA_END_MARKER)) != -1) {
            // 提取完整的数据包
            String completeData = bufferStr.substring(0, endIndex).trim();
            
            // 移除已处理的数据
            bufferStr = bufferStr.substring(endIndex + DATA_END_MARKER.length());
            dataBuffer.setLength(0);
            dataBuffer.append(bufferStr);
            
            // 回调通知
            if (!completeData.isEmpty() && callback != null) {
                final String finalData = completeData;
                mainHandler.post(() -> callback.onDataReceived(finalData));
            }
        }
    }
}
