package com.wp.bt.adapter;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
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
 * 历史记录适配器 - 通用版本
 * 动态显示任意数量和名称的传感器数据
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    
    private List<SensorData> dataList;
    private SimpleDateFormat dateFormat;
    private OnItemClickListener listener;
    
    // 颜色数组
    private static final int[] SENSOR_COLORS = {
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
        private GridLayout gridData;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_history_time);
            gridData = itemView.findViewById(R.id.grid_history_data);
        }
        
        public void bind(SensorData data, int position) {
            // 格式化时间
            String timeStr = dateFormat.format(new Date(data.getTimestamp()));
            tvTime.setText(timeStr);
            
            // 清空并动态添加数据项
            gridData.removeAllViews();
            
            List<SensorData.SensorItem> items = data.getItemList();
            int colorIndex = 0;
            
            for (SensorData.SensorItem item : items) {
                View itemView = createDataItemView(
                        item.getKey(),
                        item.getValue(),
                        item.getUnit(),
                        SENSOR_COLORS[colorIndex % SENSOR_COLORS.length]
                );
                
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
                
                gridData.addView(itemView, params);
                colorIndex++;
            }
            
            // 点击事件
            this.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(data, position);
                }
            });
            
            this.itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onItemLongClick(data, position);
                    return true;
                }
                return false;
            });
        }
        
        private View createDataItemView(String name, String value, String unit, int valueColor) {
            LinearLayout layout = new LinearLayout(itemView.getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(android.view.Gravity.CENTER);
            layout.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            
            // 名称
            TextView tvName = new TextView(itemView.getContext());
            tvName.setText(name);
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvName.setTextColor(itemView.getContext().getResources().getColor(R.color.text_secondary, null));
            layout.addView(tvName);
            
            // 值
            TextView tvValue = new TextView(itemView.getContext());
            String displayValue = value + (unit != null && !unit.isEmpty() ? " " + unit : "");
            tvValue.setText(displayValue);
            tvValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvValue.setTextColor(valueColor);
            tvValue.setTypeface(null, android.graphics.Typeface.BOLD);
            layout.addView(tvValue);
            
            return layout;
        }
        
        private int dpToPx(int dp) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dp,
                    itemView.getResources().getDisplayMetrics()
            );
        }
    }
}
