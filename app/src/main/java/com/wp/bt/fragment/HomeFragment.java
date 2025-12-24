package com.wp.bt.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
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

import java.util.List;

/**
 * 主页Fragment - 通用版本
 * 动态显示任意数量和名称的传感器数据
 */
public class HomeFragment extends Fragment {
    
    // 动态传感器数据容器
    private GridLayout gridSensorData;
    
    // 阈值控制
    private RecyclerView rvThreshold;
    private ThresholdAdapter thresholdAdapter;
    
    // 连接状态
    private TextView tvConnectionStatus;
    private CardView cardConnectionStatus;
    
    // 阈值变化回调
    private OnThresholdChangeListener thresholdChangeListener;
    
    // 颜色数组，用于不同传感器项
    private static final int[] SENSOR_COLORS = {
            Color.parseColor("#FF5722"), // 橙红
            Color.parseColor("#2196F3"), // 蓝色
            Color.parseColor("#FFC107"), // 黄色
            Color.parseColor("#00BCD4"), // 青色
            Color.parseColor("#9C27B0"), // 紫色
            Color.parseColor("#4CAF50"), // 绿色
            Color.parseColor("#E91E63"), // 粉色
            Color.parseColor("#3F51B5"), // 靛蓝
            Color.parseColor("#009688"), // 蓝绿
            Color.parseColor("#795548"), // 棕色
    };
    
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
        
        // 动态传感器数据容器
        gridSensorData = view.findViewById(R.id.grid_sensor_data);
        
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
     * 更新传感器数据显示 - 动态版本
     * 根据数据项动态创建显示卡片
     */
    public void updateSensorData(SensorData data) {
        if (getActivity() == null || data == null) return;
        
        getActivity().runOnUiThread(() -> {
            // 清空现有视图
            gridSensorData.removeAllViews();
            
            List<SensorData.SensorItem> items = data.getItemList();
            int colorIndex = 0;
            
            for (SensorData.SensorItem item : items) {
                // 创建卡片
                CardView card = createSensorCard(
                        item.getKey(),
                        item.getValue(),
                        item.getUnit(),
                        SENSOR_COLORS[colorIndex % SENSOR_COLORS.length]
                );
                
                // 设置GridLayout参数
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
                
                gridSensorData.addView(card, params);
                colorIndex++;
            }
        });
    }
    
    /**
     * 创建传感器数据卡片
     */
    private CardView createSensorCard(String name, String value, String unit, int valueColor) {
        // 创建CardView
        CardView cardView = new CardView(requireContext());
        cardView.setRadius(dpToPx(8));
        cardView.setCardElevation(dpToPx(2));
        cardView.setContentPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        cardView.setCardBackgroundColor(Color.WHITE);
        
        // 创建内容布局
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        
        // 名称
        TextView tvName = new TextView(requireContext());
        tvName.setText(name);
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvName.setTextColor(getResources().getColor(R.color.text_secondary, null));
        layout.addView(tvName);
        
        // 值和单位的水平布局
        LinearLayout valueLayout = new LinearLayout(requireContext());
        valueLayout.setOrientation(LinearLayout.HORIZONTAL);
        valueLayout.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams valueLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        valueLayoutParams.topMargin = dpToPx(8);
        
        // 值
        TextView tvValue = new TextView(requireContext());
        tvValue.setText(value);
        tvValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        tvValue.setTextColor(valueColor);
        tvValue.setTypeface(null, android.graphics.Typeface.BOLD);
        valueLayout.addView(tvValue);
        
        // 单位
        if (unit != null && !unit.isEmpty()) {
            TextView tvUnit = new TextView(requireContext());
            tvUnit.setText(" " + unit);
            tvUnit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tvUnit.setTextColor(getResources().getColor(R.color.text_secondary, null));
            LinearLayout.LayoutParams unitParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            unitParams.bottomMargin = dpToPx(4);
            tvUnit.setLayoutParams(unitParams);
            valueLayout.addView(tvUnit);
        }
        
        layout.addView(valueLayout, valueLayoutParams);
        cardView.addView(layout);
        
        return cardView;
    }
    
    /**
     * dp转px
     */
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
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
