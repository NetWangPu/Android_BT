package com.wp.bt.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wp.bt.R;
import com.wp.bt.model.ThresholdItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 阈值控制适配器
 * 用于显示和控制各个阈值项的滑动条
 */
public class ThresholdAdapter extends RecyclerView.Adapter<ThresholdAdapter.ViewHolder> {
    
    private List<ThresholdItem> thresholdItems;
    private OnThresholdChangeListener listener;
    
    /**
     * 阈值变化监听器
     */
    public interface OnThresholdChangeListener {
        void onThresholdChanged(ThresholdItem item, int newValue);
    }
    
    public ThresholdAdapter() {
        this.thresholdItems = new ArrayList<>();
    }
    
    public void setOnThresholdChangeListener(OnThresholdChangeListener listener) {
        this.listener = listener;
    }
    
    public void setThresholdItems(List<ThresholdItem> items) {
        this.thresholdItems = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void updateItem(String key, int newValue) {
        for (int i = 0; i < thresholdItems.size(); i++) {
            if (thresholdItems.get(i).getKey().equals(key)) {
                thresholdItems.get(i).setValue(newValue);
                notifyItemChanged(i);
                break;
            }
        }
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_threshold, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ThresholdItem item = thresholdItems.get(position);
        holder.bind(item);
    }
    
    @Override
    public int getItemCount() {
        return thresholdItems.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        
        private TextView tvName;
        private TextView tvValue;
        private TextView tvRange;
        private SeekBar seekBar;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_threshold_name);
            tvValue = itemView.findViewById(R.id.tv_threshold_value);
            tvRange = itemView.findViewById(R.id.tv_threshold_range);
            seekBar = itemView.findViewById(R.id.seekbar_threshold);
        }
        
        public void bind(ThresholdItem item) {
            tvName.setText(item.getName());
            tvValue.setText(item.getValue() + " " + item.getUnit());
            tvRange.setText(item.getMin() + " ~ " + item.getMax());
            
            // 设置SeekBar范围
            // SeekBar的progress从0开始，需要转换
            int range = item.getMax() - item.getMin();
            seekBar.setMax(range / item.getStep());
            
            // 设置当前值
            int progress = (item.getValue() - item.getMin()) / item.getStep();
            seekBar.setProgress(progress);
            
            // 设置监听器
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        int newValue = item.getMin() + progress * item.getStep();
                        tvValue.setText(newValue + " " + item.getUnit());
                    }
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int newValue = item.getMin() + seekBar.getProgress() * item.getStep();
                    item.setValue(newValue);
                    
                    if (listener != null) {
                        listener.onThresholdChanged(item, newValue);
                    }
                }
            });
        }
    }
}
