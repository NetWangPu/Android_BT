package com.wp.bt.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 阈值数据集合模型
 * 用于存储所有阈值配置
 * 数据格式: {"Threshold":{"TempMax":{"val":40,"min":-10,"max":100,"step":2},...}} FFDD
 */
public class ThresholdData {
    
    private List<ThresholdItem> thresholdItems;
    
    public ThresholdData() {
        thresholdItems = new ArrayList<>();
    }
    
    public List<ThresholdItem> getThresholdItems() {
        return thresholdItems;
    }
    
    public void setThresholdItems(List<ThresholdItem> thresholdItems) {
        this.thresholdItems = thresholdItems;
    }
    
    public void addThresholdItem(ThresholdItem item) {
        thresholdItems.add(item);
    }
    
    /**
     * 根据key获取阈值项
     */
    public ThresholdItem getItemByKey(String key) {
        for (ThresholdItem item : thresholdItems) {
            if (item.getKey().equals(key)) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * 更新指定key的阈值
     */
    public boolean updateValue(String key, int newValue) {
        ThresholdItem item = getItemByKey(key);
        if (item != null) {
            item.setValue(newValue);
            return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ThresholdData{\n");
        for (ThresholdItem item : thresholdItems) {
            sb.append("  ").append(item.toString()).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
