package com.wp.bt.model;

/**
 * 传感器数据模型类
 * 用于存储从蓝牙串口接收的传感器数据
 * 数据格式: {"Date":{"Temp":{"val":27,"unit":"C"},...}} FFDD
 */
public class SensorData {
    
    private long id;           // 数据库ID
    private long timestamp;    // 时间戳
    
    // 温度
    private float temperature;
    private String tempUnit;
    
    // 湿度
    private float humidity;
    private String humUnit;
    
    // 光照
    private float light;
    private String lightUnit;
    
    // 水分
    private float water;
    private String waterUnit;
    
    // CO2浓度
    private float co2;
    private String co2Unit;
    
    // PH值
    private float ph;
    private String phUnit;
    
    public SensorData() {
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public float getTemperature() {
        return temperature;
    }
    
    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }
    
    public String getTempUnit() {
        return tempUnit;
    }
    
    public void setTempUnit(String tempUnit) {
        this.tempUnit = tempUnit;
    }
    
    public float getHumidity() {
        return humidity;
    }
    
    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }
    
    public String getHumUnit() {
        return humUnit;
    }
    
    public void setHumUnit(String humUnit) {
        this.humUnit = humUnit;
    }
    
    public float getLight() {
        return light;
    }
    
    public void setLight(float light) {
        this.light = light;
    }
    
    public String getLightUnit() {
        return lightUnit;
    }
    
    public void setLightUnit(String lightUnit) {
        this.lightUnit = lightUnit;
    }
    
    public float getWater() {
        return water;
    }
    
    public void setWater(float water) {
        this.water = water;
    }
    
    public String getWaterUnit() {
        return waterUnit;
    }
    
    public void setWaterUnit(String waterUnit) {
        this.waterUnit = waterUnit;
    }
    
    public float getCo2() {
        return co2;
    }
    
    public void setCo2(float co2) {
        this.co2 = co2;
    }
    
    public String getCo2Unit() {
        return co2Unit;
    }
    
    public void setCo2Unit(String co2Unit) {
        this.co2Unit = co2Unit;
    }
    
    public float getPh() {
        return ph;
    }
    
    public void setPh(float ph) {
        this.ph = ph;
    }
    
    public String getPhUnit() {
        return phUnit;
    }
    
    public void setPhUnit(String phUnit) {
        this.phUnit = phUnit;
    }
    
    @Override
    public String toString() {
        return "SensorData{" +
                "温度=" + temperature + tempUnit +
                ", 湿度=" + humidity + humUnit +
                ", 光照=" + light + lightUnit +
                ", 水分=" + water + waterUnit +
                ", CO2=" + co2 + co2Unit +
                ", PH=" + ph + phUnit +
                '}';
    }
}
