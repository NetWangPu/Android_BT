package com.wp.bt.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wp.bt.R;
import com.wp.bt.model.SensorData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 历史记录适配器
 * 用于显示历史传感器数据列表
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    
    private List<SensorData> dataList;
    private SimpleDateFormat dateFormat;
    private OnItemClickListener listener;
    
    /**
     * 点击监听器
     */
    public interface OnItemClickListener {
        void onItemClick(SensorData data, int position);
        void onItemLongClick(SensorData data, int position);
    }
    
    public HistoryAdapter() {
        this.dataList = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    public void setDataList(List<SensorData> dataList) {
        this.dataList = dataList != null ? dataList : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void addData(SensorData data) {
        dataList.add(0, data);
        notifyItemInserted(0);
    }
    
    public void removeData(int position) {
        if (position >= 0 && position < dataList.size()) {
            dataList.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    public void clearData() {
        dataList.clear();
        notifyDataSetChanged();
    }
    
    public SensorData getItem(int position) {
        if (position >= 0 && position < dataList.size()) {
            return dataList.get(position);
        }
        return null;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SensorData data = dataList.get(position);
        holder.bind(data, position);
    }
    
    @Override
    public int getItemCount() {
        return dataList.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        
        private TextView tvTime;
        private TextView tvTemp;
        private TextView tvHum;
        private TextView tvLight;
        private TextView tvWater;
        private TextView tvCo2;
        private TextView tvPh;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_history_time);
            tvTemp = itemView.findViewById(R.id.tv_history_temp);
            tvHum = itemView.findViewById(R.id.tv_history_hum);
            tvLight = itemView.findViewById(R.id.tv_history_light);
            tvWater = itemView.findViewById(R.id.tv_history_water);
            tvCo2 = itemView.findViewById(R.id.tv_history_co2);
            tvPh = itemView.findViewById(R.id.tv_history_ph);
        }
        
        public void bind(SensorData data, int position) {
            // 格式化时间
            String timeStr = dateFormat.format(new Date(data.getTimestamp()));
            tvTime.setText(timeStr);
            
            // 设置各项数据
            tvTemp.setText(String.format(Locale.getDefault(), "%.1f%s", 
                    data.getTemperature(), data.getTempUnit() != null ? data.getTempUnit() : "°C"));
            tvHum.setText(String.format(Locale.getDefault(), "%.1f%s", 
                    data.getHumidity(), data.getHumUnit() != null ? data.getHumUnit() : "%"));
            tvLight.setText(String.format(Locale.getDefault(), "%.1f%s", 
                    data.getLight(), data.getLightUnit() != null ? data.getLightUnit() : "%"));
            tvWater.setText(String.format(Locale.getDefault(), "%.1f%s", 
                    data.getWater(), data.getWaterUnit() != null ? data.getWaterUnit() : "%"));
            tvCo2.setText(String.format(Locale.getDefault(), "%.0f%s", 
                    data.getCo2(), data.getCo2Unit() != null ? data.getCo2Unit() : "ppm"));
            tvPh.setText(String.format(Locale.getDefault(), "%.1f%s", 
                    data.getPh(), data.getPhUnit() != null ? data.getPhUnit() : ""));
            
            // 点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(data, position);
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onItemLongClick(data, position);
                    return true;
                }
                return false;
            });
        }
    }
}
