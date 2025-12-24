package com.wp.bt.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wp.bt.R;
import com.wp.bt.adapter.ThresholdAdapter;
import com.wp.bt.model.SensorData;
import com.wp.bt.model.ThresholdData;
import com.wp.bt.model.ThresholdItem;

/**
 * 主页Fragment
 * 显示实时传感器数据和阈值控制
 */
public class HomeFragment extends Fragment {
    
    // 传感器数据显示控件
    private TextView tvTemp, tvHum, tvLight, tvWater, tvCo2, tvPh;
    private TextView tvTempUnit, tvHumUnit, tvLightUnit, tvWaterUnit, tvCo2Unit, tvPhUnit;
    
    // 阈值控制
    private RecyclerView rvThreshold;
    private ThresholdAdapter thresholdAdapter;
    
    // 连接状态
    private TextView tvConnectionStatus;
    private CardView cardConnectionStatus;
    
    // 阈值变化回调
    private OnThresholdChangeListener thresholdChangeListener;
    
    /**
     * 阈值变化监听器接口
     */
    public interface OnThresholdChangeListener {
        void onThresholdChanged(ThresholdItem item, int newValue);
    }
    
    public void setOnThresholdChangeListener(OnThresholdChangeListener listener) {
        this.thresholdChangeListener = listener;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupThresholdRecyclerView();
    }
    
    /**
     * 初始化视图
     */
    private void initViews(View view) {
        // 连接状态
        tvConnectionStatus = view.findViewById(R.id.tv_connection_status);
        cardConnectionStatus = view.findViewById(R.id.card_connection_status);
        
        // 传感器数据显示
        tvTemp = view.findViewById(R.id.tv_temp_value);
        tvHum = view.findViewById(R.id.tv_hum_value);
        tvLight = view.findViewById(R.id.tv_light_value);
        tvWater = view.findViewById(R.id.tv_water_value);
        tvCo2 = view.findViewById(R.id.tv_co2_value);
        tvPh = view.findViewById(R.id.tv_ph_value);
        
        tvTempUnit = view.findViewById(R.id.tv_temp_unit);
        tvHumUnit = view.findViewById(R.id.tv_hum_unit);
        tvLightUnit = view.findViewById(R.id.tv_light_unit);
        tvWaterUnit = view.findViewById(R.id.tv_water_unit);
        tvCo2Unit = view.findViewById(R.id.tv_co2_unit);
        tvPhUnit = view.findViewById(R.id.tv_ph_unit);
        
        // 阈值控制列表
        rvThreshold = view.findViewById(R.id.rv_threshold);
    }
    
    /**
     * 设置阈值RecyclerView
     */
    private void setupThresholdRecyclerView() {
        thresholdAdapter = new ThresholdAdapter();
        thresholdAdapter.setOnThresholdChangeListener((item, newValue) -> {
            if (thresholdChangeListener != null) {
                thresholdChangeListener.onThresholdChanged(item, newValue);
            }
        });
        
        rvThreshold.setLayoutManager(new LinearLayoutManager(getContext()));
        rvThreshold.setAdapter(thresholdAdapter);
    }
    
    /**
     * 更新连接状态显示
     */
    public void updateConnectionStatus(boolean connected, String deviceName) {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            if (connected) {
                tvConnectionStatus.setText("已连接: " + (deviceName != null ? deviceName : "未知设备"));
                cardConnectionStatus.setCardBackgroundColor(
                        getResources().getColor(R.color.status_connected, null));
            } else {
                tvConnectionStatus.setText("未连接设备");
                cardConnectionStatus.setCardBackgroundColor(
                        getResources().getColor(R.color.status_disconnected, null));
            }
        });
    }
    
    /**
     * 更新传感器数据显示
     */
    public void updateSensorData(SensorData data) {
        if (getActivity() == null || data == null) return;
        
        getActivity().runOnUiThread(() -> {
            // 温度
            tvTemp.setText(String.format("%.1f", data.getTemperature()));
            if (data.getTempUnit() != null) {
                tvTempUnit.setText("°" + data.getTempUnit());
            }
            
            // 湿度
            tvHum.setText(String.format("%.1f", data.getHumidity()));
            if (data.getHumUnit() != null) {
                tvHumUnit.setText(data.getHumUnit());
            }
            
            // 光照
            tvLight.setText(String.format("%.1f", data.getLight()));
            if (data.getLightUnit() != null) {
                tvLightUnit.setText(data.getLightUnit());
            }
            
            // 水分
            tvWater.setText(String.format("%.1f", data.getWater()));
            if (data.getWaterUnit() != null) {
                tvWaterUnit.setText(data.getWaterUnit());
            }
            
            // CO2
            tvCo2.setText(String.format("%.0f", data.getCo2()));
            if (data.getCo2Unit() != null) {
                tvCo2Unit.setText(data.getCo2Unit());
            }
            
            // PH
            tvPh.setText(String.format("%.1f", data.getPh()));
            if (data.getPhUnit() != null) {
                tvPhUnit.setText(data.getPhUnit());
            }
        });
    }
    
    /**
     * 更新阈值数据
     */
    public void updateThresholdData(ThresholdData data) {
        if (getActivity() == null || data == null) return;
        
        getActivity().runOnUiThread(() -> {
            thresholdAdapter.setThresholdItems(data.getThresholdItems());
        });
    }
    
    /**
     * 显示提示消息
     */
    public void showMessage(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }
}
