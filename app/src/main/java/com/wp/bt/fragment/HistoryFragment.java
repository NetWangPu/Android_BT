package com.wp.bt.fragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.wp.bt.R;
import com.wp.bt.adapter.HistoryAdapter;
import com.wp.bt.database.DatabaseHelper;
import com.wp.bt.model.SensorData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 历史记录Fragment - 通用版本
 * 动态显示任意数量和名称的传感器数据图表
 */
public class HistoryFragment extends Fragment {
    
    // 图表
    private LineChart lineChart;
    
    // 历史数据列表
    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;
    
    // 数据库
    private DatabaseHelper databaseHelper;
    
    // 数据统计
    private TextView tvDataCount;
    
    // 按钮
    private Button btnRefresh, btnClearAll;
    private LinearLayout layoutFilterButtons;
    
    // 当前选中的数据键名 (null表示显示全部)
    private String currentSelectedKey = null;
    
    // 图表颜色数组
    private static final int[] CHART_COLORS = {
            Color.parseColor("#FF5722"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#FFC107"),
            Color.parseColor("#00BCD4"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#3F51B5"),
            Color.parseColor("#009688"),
            Color.parseColor("#795548"),
    };
    
    // 已知的数据键名集合
    private Set<String> knownKeys = new HashSet<>();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        databaseHelper = DatabaseHelper.getInstance(requireContext());
        
        initViews(view);
        setupChart();
        setupRecyclerView();
        setupButtons();
        
        loadData();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
    
    /**
     * 初始化视图
     */
    private void initViews(View view) {
        lineChart = view.findViewById(R.id.line_chart);
        rvHistory = view.findViewById(R.id.rv_history);
        tvDataCount = view.findViewById(R.id.tv_data_count);
        
        btnRefresh = view.findViewById(R.id.btn_refresh);
        btnClearAll = view.findViewById(R.id.btn_clear_all);
        layoutFilterButtons = view.findViewById(R.id.layout_filter_buttons);
    }
    
    /**
     * 设置图表
     */
    private void setupChart() {
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.WHITE);
        
        Description description = new Description();
        description.setText("传感器数据曲线");
        description.setTextSize(12f);
        lineChart.setDescription(description);
        
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            @Override
            public String getFormattedValue(float value) {
                return sdf.format(new Date((long) value));
            }
        });
        
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
        
        Legend legend = lineChart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextSize(11f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);
    }
    
    /**
     * 设置RecyclerView
     */
    private void setupRecyclerView() {
        historyAdapter = new HistoryAdapter();
        historyAdapter.setOnItemClickListener(new HistoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SensorData data, int position) {
                showDataDetail(data);
            }
            
            @Override
            public void onItemLongClick(SensorData data, int position) {
                showDeleteDialog(data, position);
            }
        });
        
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistory.setAdapter(historyAdapter);
    }
    
    /**
     * 设置按钮点击事件
     */
    private void setupButtons() {
        btnRefresh.setOnClickListener(v -> loadData());
        btnClearAll.setOnClickListener(v -> showClearAllDialog());
    }
    
    /**
     * 动态创建筛选按钮
     */
    private void updateFilterButtons(List<SensorData> dataList) {
        // 收集所有数据键名
        Set<String> allKeys = new HashSet<>();
        for (SensorData data : dataList) {
            allKeys.addAll(data.getKeys());
        }
        
        // 如果键名没有变化，不需要重建按钮
        if (allKeys.equals(knownKeys)) {
            return;
        }
        knownKeys = allKeys;
        
        // 清空现有按钮
        layoutFilterButtons.removeAllViews();
        
        // 添加"全部"按钮
        Button btnAll = createFilterButton("全部", null);
        layoutFilterButtons.addView(btnAll);
        
        // 为每个键名创建按钮
        for (String key : allKeys) {
            Button btn = createFilterButton(key, key);
            layoutFilterButtons.addView(btn);
        }
    }
    
    /**
     * 创建筛选按钮
     */
    private Button createFilterButton(String text, String key) {
        Button btn = new Button(requireContext(), null, 
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btn.setText(text);
        btn.setTextSize(12);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(36)
        );
        params.setMargins(0, 0, dpToPx(4), 0);
        btn.setLayoutParams(params);
        btn.setPadding(dpToPx(12), 0, dpToPx(12), 0);
        
        btn.setOnClickListener(v -> {
            currentSelectedKey = key;
            loadData();
        });
        
        return btn;
    }
    
    /**
     * 加载数据
     */
    public void loadData() {
        new Thread(() -> {
            List<SensorData> dataList = databaseHelper.getRecentSensorData(100);
            int totalCount = databaseHelper.getDataCount();
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    historyAdapter.setDataList(dataList);
                    tvDataCount.setText("共 " + totalCount + " 条记录");
                    updateFilterButtons(dataList);
                    updateChartWithData(dataList);
                });
            }
        }).start();
    }
    
    /**
     * 使用数据更新图表 - 动态版本
     */
    private void updateChartWithData(List<SensorData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            lineChart.clear();
            lineChart.invalidate();
            return;
        }
        
        // 收集所有数据键名
        Set<String> allKeys = new HashSet<>();
        for (SensorData data : dataList) {
            allKeys.addAll(data.getKeys());
        }
        
        List<ILineDataSet> dataSets = new ArrayList<>();
        int colorIndex = 0;
        
        for (String key : allKeys) {
            // 如果选择了特定键，只显示该键的数据
            if (currentSelectedKey != null && !currentSelectedKey.equals(key)) {
                continue;
            }
            
            List<Entry> entries = new ArrayList<>();
            
            for (SensorData data : dataList) {
                SensorData.SensorItem item = data.getItem(key);
                if (item != null) {
                    float value = item.getFloatValue();
                    entries.add(new Entry(data.getTimestamp(), value));
                }
            }
            
            if (!entries.isEmpty()) {
                LineDataSet dataSet = new LineDataSet(entries, key);
                int color = CHART_COLORS[colorIndex % CHART_COLORS.length];
                dataSet.setColor(color);
                dataSet.setCircleColor(color);
                dataSet.setLineWidth(2f);
                dataSet.setCircleRadius(3f);
                dataSet.setDrawCircleHole(false);
                dataSet.setValueTextSize(9f);
                dataSet.setDrawValues(false);
                dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                dataSet.setCubicIntensity(0.2f);
                
                dataSets.add(dataSet);
                colorIndex++;
            }
        }
        
        if (dataSets.isEmpty()) {
            lineChart.clear();
        } else {
            LineData lineData = new LineData(dataSets);
            lineChart.setData(lineData);
            lineChart.animateX(500);
        }
        lineChart.invalidate();
    }
    
    /**
     * 显示数据详情 - 动态版本
     */
    private void showDataDetail(SensorData data) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        StringBuilder sb = new StringBuilder();
        sb.append("时间: ").append(sdf.format(new Date(data.getTimestamp()))).append("\n\n");
        
        for (SensorData.SensorItem item : data.getItemList()) {
            sb.append(item.getKey()).append(": ")
              .append(item.getValue())
              .append(item.getUnit().isEmpty() ? "" : " " + item.getUnit())
              .append("\n");
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle("数据详情")
                .setMessage(sb.toString())
                .setPositiveButton("确定", null)
                .show();
    }
    
    /**
     * 显示删除确认对话框
     */
    private void showDeleteDialog(SensorData data, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除确认")
                .setMessage("确定要删除这条记录吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    new Thread(() -> {
                        databaseHelper.deleteSensorData(data.getId());
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                historyAdapter.removeData(position);
                                Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
                                loadData();
                            });
                        }
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 显示清空所有数据确认对话框
     */
    private void showClearAllDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("清空确认")
                .setMessage("确定要清空所有历史记录吗？此操作不可恢复！")
                .setPositiveButton("清空", (dialog, which) -> {
                    new Thread(() -> {
                        databaseHelper.deleteAllSensorData();
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                historyAdapter.clearData();
                                lineChart.clear();
                                lineChart.invalidate();
                                tvDataCount.setText("共 0 条记录");
                                knownKeys.clear();
                                layoutFilterButtons.removeAllViews();
                                Toast.makeText(getContext(), "已清空所有记录", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 添加新数据并刷新显示
     */
    public void addNewData(SensorData data) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                historyAdapter.addData(data);
                loadData();
            });
        }
    }
    
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
