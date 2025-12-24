package com.wp.bt.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 传感器数据模型 - 通用版本
 * 支持动态字段，不限制字段数量和名称
 * 数据格式: {"Date":{"XXX":{"val":xx,"unit":"xx"},...}} FFDD
 */
public class SensorData {
    
    private long id;           // 数据库ID
    private long timestamp;    // 时间戳
    private String rawJson;    // 原始JSON字符串
    
    // 使用LinkedHashMap保持插入顺序
    private LinkedHashMap<String, SensorItem> items;
    
    /**
     * 单个传感器项
     */
    public static class SensorItem {
        private String key;    // 键名 (如 Temp, Hum, 或任意名称)
        private String value;  // 值 (字符串形式，保持原样)
        private String unit;   // 单位
        
        public SensorItem(String key, String value, String unit) {
            this.key = key;
            this.value = value;
            this.unit = unit != null ? unit : "";
        }
        
        public String getKey() { return key; }
        public String getValue() { return value; }
        public String getUnit() { return unit; }
        
        public String getDisplayValue() {
            return value + (unit.isEmpty() ? "" : " " + unit);
        }
        
        // 尝试获取数值，用于图表显示
        public float getFloatValue() {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                return 0f;
            }
        }
        
        @Override
        public String toString() {
            return key + ": " + value + unit;
        }
    }
    
    public SensorData() {
        this.timestamp = System.currentTimeMillis();
        this.items = new LinkedHashMap<>();
    }
    
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
    
    /**
     * 添加传感器项
     */
    public void addItem(String key, String value, String unit) {
        items.put(key, new SensorItem(key, value, unit));
    }
    
    /**
     * 获取所有传感器项
     */
    public LinkedHashMap<String, SensorItem> getItems() {
        return items;
    }
    
    /**
     * 获取传感器项列表
     */
    public List<SensorItem> getItemList() {
        return new ArrayList<>(items.values());
    }
    
    /**
     * 获取指定键的传感器项
     */
    public SensorItem getItem(String key) {
        return items.get(key);
    }
    
    /**
     * 获取传感器项数量
     */
    public int getItemCount() {
        return items.size();
    }
    
    /**
     * 获取所有键名
     */
    public List<String> getKeys() {
        return new ArrayList<>(items.keySet());
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SensorData{timestamp=");
        sb.append(timestamp);
        for (SensorItem item : items.values()) {
            sb.append(", ").append(item.toString());
        }
        sb.append("}");
        return sb.toString();
    }
}
