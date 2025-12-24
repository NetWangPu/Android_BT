package com.wp.bt.model;

/**
 * 阈值项数据模型
 * 用于存储单个阈值配置项
 * 例如: TempMax, HumMax, Lightmin 等
 */
public class ThresholdItem {
    
    private String key;      // 阈值键名 (如 TempMax)
    private String name;     // 显示名称 (如 温度上限)
    private int value;       // 当前值
    private int min;         // 最小值
    private int max;         // 最大值
    private int step;        // 步长
    private String unit;     // 单位
    
    public ThresholdItem() {
    }
    
    public ThresholdItem(String key, String name, int value, int min, int max, int step, String unit) {
        this.key = key;
        this.name = name;
        this.value = value;
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit;
    }
    
    // Getters and Setters
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getValue() {
        return value;
    }
    
    public void setValue(int value) {
        this.value = value;
    }
    
    public int getMin() {
        return min;
    }
    
    public void setMin(int min) {
        this.min = min;
    }
    
    public int getMax() {
        return max;
    }
    
    public void setMax(int max) {
        this.max = max;
    }
    
    public int getStep() {
        return step;
    }
    
    public void setStep(int step) {
        this.step = step;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    /**
     * 生成下发到设备的控制指令
     * 格式: TODEVICEDATA##KEY##VALUE##
     */
    public String generateCommand() {
        return "TODEVICEDATA##" + key + "##" + value + "##";
    }
    
    @Override
    public String toString() {
        return "ThresholdItem{" +
                "key='" + key + '\'' +
                ", name='" + name + '\'' +
                ", value=" + value +
                ", min=" + min +
                ", max=" + max +
                ", step=" + step +
                ", unit='" + unit + '\'' +
                '}';
    }
}
