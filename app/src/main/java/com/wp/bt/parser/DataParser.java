package com.wp.bt.parser;

import android.util.Log;

import com.wp.bt.model.SensorData;
import com.wp.bt.model.ThresholdData;
import com.wp.bt.model.ThresholdItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * 数据解析器 - 通用版本
 * 负责解析从蓝牙串口接收的JSON数据
 * 支持动态字段，不限制字段数量和名称
 * 
 * 传感器数据格式:
 * {"Date":{"XXX":{"val":xx,"unit":"xx"},...}}
 * 
 * 阈值数据格式:
 * {"Threshold":{"XXX":{"val":xx,"min":xx,"max":xx,"step":xx},...}}
 */
public class DataParser {
    
    private static final String TAG = "DataParser";
    
    // JSON键名常量
    private static final String KEY_DATE = "Date";
    private static final String KEY_THRESHOLD = "Threshold";
    
    // 值字段
    private static final String FIELD_VAL = "val";
    private static final String FIELD_UNIT = "unit";
    private static final String FIELD_MIN = "min";
    private static final String FIELD_MAX = "max";
    private static final String FIELD_STEP = "step";
    
    /**
     * 解析数据回调接口
     */
    public interface ParseCallback {
        void onSensorDataParsed(SensorData data);
        void onThresholdDataParsed(ThresholdData data);
        void onParseError(String message);
    }
    
    private ParseCallback callback;
    
    public DataParser() {
    }
    
    public void setCallback(ParseCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 解析接收到的数据
     * 自动判断数据类型并调用相应的解析方法
     */
    public void parse(String rawData) {
        if (rawData == null || rawData.isEmpty()) {
            notifyError("数据为空");
            return;
        }
        
        try {
            // 清理数据，移除可能的空白字符
            String cleanData = rawData.trim();
            
            // 尝试解析为JSON
            JSONObject json = new JSONObject(cleanData);
            
            // 判断数据类型
            if (json.has(KEY_DATE)) {
                // 传感器数据
                SensorData sensorData = parseSensorData(json, cleanData);
                if (sensorData != null && callback != null) {
                    callback.onSensorDataParsed(sensorData);
                }
            } else if (json.has(KEY_THRESHOLD)) {
                // 阈值数据
                ThresholdData thresholdData = parseThresholdData(json);
                if (thresholdData != null && callback != null) {
                    callback.onThresholdDataParsed(thresholdData);
                }
            } else {
                notifyError("未知的数据格式");
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "JSON解析失败: " + rawData, e);
            notifyError("JSON解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析传感器数据 - 通用版本
     * 动态遍历所有字段，不限制字段名称和数量
     * 格式: {"Date":{"XXX":{"val":xx,"unit":"xx"},...}}
     */
    private SensorData parseSensorData(JSONObject json, String rawJson) {
        try {
            JSONObject dateObj = json.getJSONObject(KEY_DATE);
            SensorData data = new SensorData();
            data.setRawJson(rawJson);
            
            // 动态遍历所有字段
            Iterator<String> keys = dateObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object valueObj = dateObj.get(key);
                
                if (valueObj instanceof JSONObject) {
                    // 标准格式: {"val":xx,"unit":"xx"}
                    JSONObject itemObj = (JSONObject) valueObj;
                    String value = String.valueOf(itemObj.opt(FIELD_VAL));
                    String unit = itemObj.optString(FIELD_UNIT, "");
                    data.addItem(key, value, unit);
                } else {
                    // 简单格式: 直接是值
                    data.addItem(key, String.valueOf(valueObj), "");
                }
            }
            
            Log.d(TAG, "传感器数据解析成功: " + data);
            return data;
            
        } catch (JSONException e) {
            Log.e(TAG, "传感器数据解析失败", e);
            notifyError("传感器数据解析失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析阈值数据 - 通用版本
     * 动态遍历所有字段，不限制字段名称和数量
     * 格式: {"Threshold":{"XXX":{"val":xx,"min":xx,"max":xx,"step":xx},...}}
     */
    private ThresholdData parseThresholdData(JSONObject json) {
        try {
            JSONObject thresholdObj = json.getJSONObject(KEY_THRESHOLD);
            ThresholdData data = new ThresholdData();
            
            // 动态遍历所有阈值项
            Iterator<String> keys = thresholdObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object valueObj = thresholdObj.get(key);
                
                ThresholdItem item = new ThresholdItem();
                item.setKey(key);
                item.setName(key); // 直接使用原始键名作为显示名称
                
                if (valueObj instanceof JSONObject) {
                    JSONObject itemObj = (JSONObject) valueObj;
                    item.setValue(itemObj.optInt(FIELD_VAL, 0));
                    item.setMin(itemObj.optInt(FIELD_MIN, 0));
                    item.setMax(itemObj.optInt(FIELD_MAX, 100));
                    item.setStep(itemObj.optInt(FIELD_STEP, 1));
                    item.setUnit(itemObj.optString(FIELD_UNIT, ""));
                } else {
                    // 简单格式
                    try {
                        item.setValue(Integer.parseInt(String.valueOf(valueObj)));
                    } catch (NumberFormatException e) {
                        item.setValue(0);
                    }
                    item.setMin(0);
                    item.setMax(100);
                    item.setStep(1);
                    item.setUnit("");
                }
                
                data.addThresholdItem(item);
            }
            
            Log.d(TAG, "阈值数据解析成功: " + data);
            return data;
            
        } catch (JSONException e) {
            Log.e(TAG, "阈值数据解析失败", e);
            notifyError("阈值数据解析失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 通知解析错误
     */
    private void notifyError(String message) {
        Log.e(TAG, message);
        if (callback != null) {
            callback.onParseError(message);
        }
    }
    
    /**
     * 判断数据是否为传感器数据
     */
    public static boolean isSensorData(String data) {
        return data != null && data.contains(KEY_DATE);
    }
    
    /**
     * 判断数据是否为阈值数据
     */
    public static boolean isThresholdData(String data) {
        return data != null && data.contains(KEY_THRESHOLD);
    }
}
