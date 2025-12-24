package com.wp.bt.fragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import java.util.List;
import java.util.Locale;

/**
 * 历史记录Fragment
 * 显示历史数据列表和曲线图
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
    private Button btnShowTemp, btnShowHum, btnShowLight, btnShowWater, btnShowCo2, btnShowPh, btnShowAll;
    
    // 当前显示的数据类型
    private int currentDataType = DATA_TYPE_ALL;
    
    private static final int DATA_TYPE_ALL = 0;
    private static final int DATA_TYPE_TEMP = 1;
    private static final int DATA_TYPE_HUM = 2;
    private static final int DATA_TYPE_LIGHT = 3;
    private static final int DATA_TYPE_WATER = 4;
    private static final int DATA_TYPE_CO2 = 5;
    private static final int DATA_TYPE_PH = 6;
    
    // 图表颜色
    private static final int COLOR_TEMP = Color.parseColor("#FF5722");
    private static final int COLOR_HUM = Color.parseColor("#2196F3");
    private static final int COLOR_LIGHT = Color.parseColor("#FFC107");
    private static final int COLOR_WATER = Color.parseColor("#00BCD4");
    private static final int COLOR_CO2 = Color.parseColor("#9C27B0");
    private static final int COLOR_PH = Color.parseColor("#4CAF50");
    
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
        
        btnShowTemp = view.findViewById(R.id.btn_show_temp);
        btnShowHum = view.findViewById(R.id.btn_show_hum);
        btnShowLight = view.findViewById(R.id.btn_show_light);
        btnShowWater = view.findViewById(R.id.btn_show_water);
        btnShowCo2 = view.findViewById(R.id.btn_show_co2);
        btnShowPh = view.findViewById(R.id.btn_show_ph);
        btnShowAll = view.findViewById(R.id.btn_show_all);
    }
    
    /**
     * 设置图表
     */
    private void setupChart() {
        // 基本设置
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.WHITE);
        
        // 描述
        Description description = new Description();
        description.setText("传感器数据曲线");
        description.setTextSize(12f);
        lineChart.setDescription(description);
        
        // X轴设置
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
        
        // 左Y轴设置
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        
        // 右Y轴设置
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // 图例设置
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
        
        btnShowTemp.setOnClickListener(v -> {
            currentDataType = DATA_TYPE_TEMP;
            updateChart();
        });
        
        btnShowHum.setOnClickListener(v -> {
            currentDataType = DATA_TYPE_HUM;
            updateChart();
        });
        
        btnShowLight.setOnClickListener(v -> {
            currentDataType = DATA_TYPE_LIGHT;
            updateChart();
        });
        
        btnShowWater.setOnClickListener(v -> {
            currentDataType = DATA_TYPE_WATER;
            updateChart();
        });
        
        btnShowCo2.setOnClickListener(v -> {
            currentDataType = DATA_TYPE_CO2;
            updateChart();
        });
        
        btnShowPh.setOnClickListener(v -> {
            currentDataType = DATA_TYPE_PH;
            updateChart();
        });
        
        btnShowAll.setOnClickListener(v -> {
            currentDataType = DATA_TYPE_ALL;
            updateChart();
        });
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
                    updateChartWithData(dataList);
                });
            }
        }).start();
    }
    
    /**
     * 更新图表
     */
    private void updateChart() {
        new Thread(() -> {
            List<SensorData> dataList = databaseHelper.getRecentSensorData(100);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> updateChartWithData(dataList));
            }
        }).start();
    }
    
    /**
     * 使用数据更新图表
     */
    private void updateChartWithData(List<SensorData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            lineChart.clear();
            lineChart.invalidate();
            return;
        }
        
        List<ILineDataSet> dataSets = new ArrayList<>();
        
        if (currentDataType == DATA_TYPE_ALL || currentDataType == DATA_TYPE_TEMP) {
            dataSets.add(createLineDataSet(dataList, "温度", COLOR_TEMP, 
                    data -> data.getTemperature()));
        }
        
        if (currentDataType == DATA_TYPE_ALL || currentDataType == DATA_TYPE_HUM) {
            dataSets.add(createLineDataSet(dataList, "湿度", COLOR_HUM, 
                    data -> data.getHumidity()));
        }
        
        if (currentDataType == DATA_TYPE_ALL || currentDataType == DATA_TYPE_LIGHT) {
            dataSets.add(createLineDataSet(dataList, "光照", COLOR_LIGHT, 
                    data -> data.getLight()));
        }
        
        if (currentDataType == DATA_TYPE_ALL || currentDataType == DATA_TYPE_WATER) {
            dataSets.add(createLineDataSet(dataList, "水分", COLOR_WATER, 
                    data -> data.getWater()));
        }
        
        if (currentDataType == DATA_TYPE_ALL || currentDataType == DATA_TYPE_CO2) {
            // CO2数据需要归一化显示
            dataSets.add(createLineDataSet(dataList, "CO2/10", COLOR_CO2, 
                    data -> data.getCo2() / 10f));
        }
        
        if (currentDataType == DATA_TYPE_ALL || currentDataType == DATA_TYPE_PH) {
            dataSets.add(createLineDataSet(dataList, "PH", COLOR_PH, 
                    data -> data.getPh()));
        }
        
        LineData lineData = new LineData(dataSets);
        lineChart.setData(lineData);
        lineChart.invalidate();
        lineChart.animateX(500);
    }
    
    /**
     * 创建线条数据集
     */
    private LineDataSet createLineDataSet(List<SensorData> dataList, String label, int color, 
                                          ValueExtractor extractor) {
        List<Entry> entries = new ArrayList<>();
        
        for (SensorData data : dataList) {
            entries.add(new Entry(data.getTimestamp(), extractor.extract(data)));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);
        
        return dataSet;
    }
    
    /**
     * 值提取器接口
     */
    private interface ValueExtractor {
        float extract(SensorData data);
    }
    
    /**
     * 显示数据详情
     */
    private void showDataDetail(SensorData data) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String message = String.format(Locale.getDefault(),
                "时间: %s\n\n" +
                "温度: %.1f %s\n" +
                "湿度: %.1f %s\n" +
                "光照: %.1f %s\n" +
                "水分: %.1f %s\n" +
                "CO2: %.0f %s\n" +
                "PH: %.1f %s",
                sdf.format(new Date(data.getTimestamp())),
                data.getTemperature(), data.getTempUnit() != null ? data.getTempUnit() : "°C",
                data.getHumidity(), data.getHumUnit() != null ? data.getHumUnit() : "%",
                data.getLight(), data.getLightUnit() != null ? data.getLightUnit() : "%",
                data.getWater(), data.getWaterUnit() != null ? data.getWaterUnit() : "%",
                data.getCo2(), data.getCo2Unit() != null ? data.getCo2Unit() : "ppm",
                data.getPh(), data.getPhUnit() != null ? data.getPhUnit() : "");
        
        new AlertDialog.Builder(requireContext())
                .setTitle("数据详情")
                .setMessage(message)
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
}
