package com.wp.bt;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.wp.bt.bluetooth.BluetoothManager;
import com.wp.bt.database.DatabaseHelper;
import com.wp.bt.fragment.HistoryFragment;
import com.wp.bt.fragment.HomeFragment;
import com.wp.bt.model.SensorData;
import com.wp.bt.model.ThresholdData;
import com.wp.bt.model.ThresholdItem;
import com.wp.bt.parser.DataParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 主Activity
 * 负责蓝牙连接管理、Fragment切换、数据分发
 */
public class MainActivity extends AppCompatActivity implements 
        BluetoothManager.BluetoothCallback,
        DataParser.ParseCallback,
        HomeFragment.OnThresholdChangeListener {
    
    private static final String TAG = "MainActivity";
    
    // 蓝牙管理器
    private BluetoothManager bluetoothManager;
    
    // 数据解析器
    private DataParser dataParser;
    
    // 数据库
    private DatabaseHelper databaseHelper;
    
    // Fragment
    private HomeFragment homeFragment;
    private HistoryFragment historyFragment;
    private Fragment currentFragment;
    
    // UI组件
    private BottomNavigationView bottomNav;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    
    // 设备列表对话框
    private AlertDialog deviceDialog;
    private List<BluetoothDevice> discoveredDevices;
    private ArrayAdapter<String> deviceAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        initComponents();
        initViews();
        initFragments();
        checkBluetoothAndPermissions();
    }
    
    /**
     * 初始化组件
     */
    private void initComponents() {
        // 蓝牙管理器
        bluetoothManager = new BluetoothManager(this);
        bluetoothManager.setCallback(this);
        
        // 数据解析器
        dataParser = new DataParser();
        dataParser.setCallback(this);
        
        // 数据库
        databaseHelper = DatabaseHelper.getInstance(this);
        
        // 设备列表
        discoveredDevices = new ArrayList<>();
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        progressBar = findViewById(R.id.progress_bar);
        
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                switchFragment(homeFragment);
                return true;
            } else if (itemId == R.id.nav_history) {
                switchFragment(historyFragment);
                return true;
            }
            return false;
        });
    }
    
    /**
     * 初始化Fragment
     */
    private void initFragments() {
        homeFragment = new HomeFragment();
        homeFragment.setOnThresholdChangeListener(this);
        
        historyFragment = new HistoryFragment();
        
        // 默认显示主页
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, homeFragment, "home")
                .add(R.id.fragment_container, historyFragment, "history")
                .hide(historyFragment)
                .commit();
        
        currentFragment = homeFragment;
    }
    
    /**
     * 切换Fragment
     */
    private void switchFragment(Fragment targetFragment) {
        if (targetFragment != currentFragment) {
            getSupportFragmentManager().beginTransaction()
                    .hide(currentFragment)
                    .show(targetFragment)
                    .commit();
            currentFragment = targetFragment;
        }
    }
    
    /**
     * 检查蓝牙和权限
     */
    private void checkBluetoothAndPermissions() {
        if (!bluetoothManager.isBluetoothSupported()) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (!bluetoothManager.checkAndRequestPermissions(this)) {
            return;
        }
        
        if (!bluetoothManager.isBluetoothEnabled()) {
            bluetoothManager.requestEnableBluetooth(this);
        } else {
            showDeviceSelectionDialog();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == BluetoothManager.REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                if (!bluetoothManager.isBluetoothEnabled()) {
                    bluetoothManager.requestEnableBluetooth(this);
                } else {
                    showDeviceSelectionDialog();
                }
            } else {
                Toast.makeText(this, "需要蓝牙权限才能使用此功能", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == BluetoothManager.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                showDeviceSelectionDialog();
            } else {
                Toast.makeText(this, "需要启用蓝牙才能使用此功能", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * 显示设备选择对话框
     */
    @SuppressLint("MissingPermission")
    private void showDeviceSelectionDialog() {
        discoveredDevices.clear();
        
        // 获取已配对设备
        List<BluetoothDevice> pairedDevices = bluetoothManager.getPairedDevices();
        discoveredDevices.addAll(pairedDevices);
        
        // 创建设备名称列表
        List<String> deviceNames = new ArrayList<>();
        for (BluetoothDevice device : discoveredDevices) {
            String name = device.getName();
            if (name == null || name.isEmpty()) {
                name = "未知设备";
            }
            deviceNames.add(name + "\n" + device.getAddress());
        }
        
        deviceAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_list_item_1, deviceNames);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择蓝牙设备");
        builder.setAdapter(deviceAdapter, (dialog, which) -> {
            BluetoothDevice device = discoveredDevices.get(which);
            connectToDevice(device);
        });
        builder.setNegativeButton("扫描新设备", (dialog, which) -> {
            startScan();
        });
        builder.setNeutralButton("取消", null);
        
        deviceDialog = builder.create();
        deviceDialog.show();
    }
    
    /**
     * 开始扫描设备
     */
    private void startScan() {
        showProgress(true);
        Toast.makeText(this, "正在扫描蓝牙设备...", Toast.LENGTH_SHORT).show();
        bluetoothManager.startScan();
        
        // 10秒后自动停止扫描
        new android.os.Handler().postDelayed(() -> {
            bluetoothManager.stopScan();
        }, 10000);
    }
    
    /**
     * 连接到设备
     */
    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        showProgress(true);
        String deviceName = device.getName();
        if (deviceName == null) deviceName = "未知设备";
        Toast.makeText(this, "正在连接: " + deviceName, Toast.LENGTH_SHORT).show();
        bluetoothManager.connect(device);
    }
    
    /**
     * 显示/隐藏进度条
     */
    private void showProgress(boolean show) {
        runOnUiThread(() -> {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        });
    }
    
    // ==================== 菜单 ====================
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_connect) {
            if (bluetoothManager.isConnected()) {
                showDisconnectDialog();
            } else {
                showDeviceSelectionDialog();
            }
            return true;
        } else if (itemId == R.id.action_scan) {
            startScan();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 显示断开连接对话框
     */
    @SuppressLint("MissingPermission")
    private void showDisconnectDialog() {
        BluetoothDevice device = bluetoothManager.getConnectedDevice();
        String deviceName = device != null ? device.getName() : "未知设备";
        
        new AlertDialog.Builder(this)
                .setTitle("断开连接")
                .setMessage("确定要断开与 " + deviceName + " 的连接吗？")
                .setPositiveButton("断开", (dialog, which) -> {
                    bluetoothManager.disconnect();
                    homeFragment.updateConnectionStatus(false, null);
                    Toast.makeText(this, "已断开连接", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    // ==================== 蓝牙回调 ====================
    
    @SuppressLint("MissingPermission")
    @Override
    public void onDeviceFound(BluetoothDevice device) {
        // 检查是否已存在
        for (BluetoothDevice d : discoveredDevices) {
            if (d.getAddress().equals(device.getAddress())) {
                return;
            }
        }
        
        discoveredDevices.add(device);
        
        String name = device.getName();
        if (name == null || name.isEmpty()) {
            name = "未知设备";
        }
        
        if (deviceAdapter != null) {
            final String displayName = name + "\n" + device.getAddress();
            runOnUiThread(() -> {
                deviceAdapter.add(displayName);
                deviceAdapter.notifyDataSetChanged();
            });
        }
    }
    
    @SuppressLint("MissingPermission")
    @Override
    public void onDeviceConnected(BluetoothDevice device) {
        showProgress(false);
        String deviceName = device.getName();
        if (deviceName == null) deviceName = "未知设备";
        
        Toast.makeText(this, "已连接: " + deviceName, Toast.LENGTH_SHORT).show();
        homeFragment.updateConnectionStatus(true, deviceName);
        
        Log.d(TAG, "设备已连接: " + deviceName);
    }
    
    @Override
    public void onDeviceDisconnected() {
        showProgress(false);
        Toast.makeText(this, "设备已断开连接", Toast.LENGTH_SHORT).show();
        homeFragment.updateConnectionStatus(false, null);
    }
    
    @Override
    public void onDataReceived(String data) {
        Log.d(TAG, "收到数据: " + data);
        // 解析数据
        dataParser.parse(data);
    }
    
    @Override
    public void onError(String message) {
        showProgress(false);
        Toast.makeText(this, "错误: " + message, Toast.LENGTH_SHORT).show();
        Log.e(TAG, "蓝牙错误: " + message);
    }
    
    @Override
    public void onScanFinished() {
        showProgress(false);
        Toast.makeText(this, "扫描完成", Toast.LENGTH_SHORT).show();
        
        // 显示设备选择对话框
        if (!discoveredDevices.isEmpty()) {
            showDeviceSelectionDialog();
        }
    }
    
    // ==================== 数据解析回调 ====================
    
    @Override
    public void onSensorDataParsed(SensorData data) {
        Log.d(TAG, "传感器数据解析成功: " + data);
        
        // 更新主页显示
        homeFragment.updateSensorData(data);
        
        // 保存到数据库
        new Thread(() -> {
            databaseHelper.insertSensorData(data);
        }).start();
        
        // 通知历史页面
        historyFragment.addNewData(data);
    }
    
    @Override
    public void onThresholdDataParsed(ThresholdData data) {
        Log.d(TAG, "阈值数据解析成功: " + data);
        
        // 更新主页阈值控制
        homeFragment.updateThresholdData(data);
    }
    
    @Override
    public void onParseError(String message) {
        Log.e(TAG, "数据解析错误: " + message);
    }
    
    // ==================== 阈值变化回调 ====================
    
    @Override
    public void onThresholdChanged(ThresholdItem item, int newValue) {
        Log.d(TAG, "阈值变化: " + item.getKey() + " = " + newValue);
        
        // 发送控制指令到设备
        String command = item.generateCommand();
        boolean success = bluetoothManager.sendData(command);
        
        if (success) {
            Toast.makeText(this, "已发送: " + item.getName() + " = " + newValue, 
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "发送失败，请检查连接", Toast.LENGTH_SHORT).show();
        }
    }
    
    // ==================== 生命周期 ====================
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothManager != null) {
            bluetoothManager.release();
        }
    }
}