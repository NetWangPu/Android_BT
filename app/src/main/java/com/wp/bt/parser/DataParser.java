package com.wp.bt.parser;

import android.util.Log;

import com.wp.bt.model.SensorData;
import com.wp.bt.model.ThresholdData;
import com.wp.bt.model.ThresholdItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * 数据解析器
 * 负责解析从蓝牙串口接收的JSON数据
 * 
 * 传感器数据格式:
 * {"Date":{"Temp":{"val":27,"unit":"C"},"Hum":{"val":23,"unit":"%"},...}}
 * 
 * 阈值数据格式:
 * {"Threshold":{"TempMax":{"val":40,"min":-10,"max":100,"step":2},...}}
 */
public class DataParser {
    
    private static final String TAG = "DataParser";
    
    // JSON键名常量
    private static final String KEY_DATE = "Date";
    private static final String KEY_THRESHOLD = "Threshold";
    
    // 传感器数据键名
    private static final String KEY_TEMP = "Temp";
    private static final String KEY_HUM = "Hum";
    private static final String KEY_LIGHT = "Light";
    private static final String KEY_WATER = "Water";
    private static final String KEY_CO2 = "Co2";
    private static final String KEY_PH = "PH";
    
    // 值字段
    private static final String FIELD_VAL = "val";
    private static final String FIELD_UNIT = "unit";
    private static final String FIELD_MIN = "min";
    private static final String FIELD_MAX = "max";
    private static final String FIELD_STEP = "step";
    
    // 阈值键名到中文名称的映射
    private static final String[][] THRESHOLD_NAME_MAP = {
            {"TempMax", "温度上限", "°C"},
            {"HumMax", "湿度上限", "%"},
            {"Lightmin", "光照下限", "%"},
            {"Lightmax", "光照上限", "%"},
            {"Watermin", "水分下限", "%"},
            {"Co2Max", "CO2上限", "ppm"}
    };
    
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
                SensorData sensorData = parseSensorData(json);
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
     * 解析传感器数据
     * 格式: {"Date":{"Temp":{"val":27,"unit":"C"},...}}
     */
    private SensorData parseSensorData(JSONObject json) {
        try {
            JSONObject dateObj = json.getJSONObject(KEY_DATE);
            SensorData data = new SensorData();
            
            // 解析温度
            if (dateObj.has(KEY_TEMP)) {
                JSONObject tempObj = dateObj.getJSONObject(KEY_TEMP);
                data.setTemperature((float) tempObj.optDouble(FIELD_VAL, 0));
                data.setTempUnit(tempObj.optString(FIELD_UNIT, "°C"));
            }
            
            // 解析湿度
            if (dateObj.has(KEY_HUM)) {
                JSONObject humObj = dateObj.getJSONObject(KEY_HUM);
                data.setHumidity((float) humObj.optDouble(FIELD_VAL, 0));
                data.setHumUnit(humObj.optString(FIELD_UNIT, "%"));
            }
            
            // 解析光照
            if (dateObj.has(KEY_LIGHT)) {
                JSONObject lightObj = dateObj.getJSONObject(KEY_LIGHT);
                data.setLight((float) lightObj.optDouble(FIELD_VAL, 0));
                data.setLightUnit(lightObj.optString(FIELD_UNIT, "%"));
            }
            
            // 解析水分
            if (dateObj.has(KEY_WATER)) {
                JSONObject waterObj = dateObj.getJSONObject(KEY_WATER);
                data.setWater((float) waterObj.optDouble(FIELD_VAL, 0));
                data.setWaterUnit(waterObj.optString(FIELD_UNIT, "%"));
            }
            
            // 解析CO2
            if (dateObj.has(KEY_CO2)) {
                JSONObject co2Obj = dateObj.getJSONObject(KEY_CO2);
                data.setCo2((float) co2Obj.optDouble(FIELD_VAL, 0));
                data.setCo2Unit(co2Obj.optString(FIELD_UNIT, "ppm"));
            }
            
            // 解析PH
            if (dateObj.has(KEY_PH)) {
                JSONObject phObj = dateObj.getJSONObject(KEY_PH);
                data.setPh((float) phObj.optDouble(FIELD_VAL, 0));
                data.setPhUnit(phObj.optString(FIELD_UNIT, ""));
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
     * 解析阈值数据
     * 格式: {"Threshold":{"TempMax":{"val":40,"min":-10,"max":100,"step":2},...}}
     */
    private ThresholdData parseThresholdData(JSONObject json) {
        try {
            JSONObject thresholdObj = json.getJSONObject(KEY_THRESHOLD);
            ThresholdData data = new ThresholdData();
            
            // 遍历所有阈值项
            Iterator<String> keys = thresholdObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject itemObj = thresholdObj.getJSONObject(key);
                
                ThresholdItem item = new ThresholdItem();
                item.setKey(key);
                item.setName(getThresholdName(key));
                item.setValue(itemObj.optInt(FIELD_VAL, 0));
                item.setMin(itemObj.optInt(FIELD_MIN, 0));
                item.setMax(itemObj.optInt(FIELD_MAX, 100));
                item.setStep(itemObj.optInt(FIELD_STEP, 1));
                item.setUnit(getThresholdUnit(key));
                
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
     * 获取阈值项的中文名称
     */
    private String getThresholdName(String key) {
        for (String[] mapping : THRESHOLD_NAME_MAP) {
            if (mapping[0].equals(key)) {
                return mapping[1];
            }
        }
        return key;
    }
    
    /**
     * 获取阈值项的单位
     */
    private String getThresholdUnit(String key) {
        for (String[] mapping : THRESHOLD_NAME_MAP) {
            if (mapping[0].equals(key)) {
                return mapping[2];
            }
        }
        return "";
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
