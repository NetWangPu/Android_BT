package com.wp.bt.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.wp.bt.model.SensorData;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite数据库帮助类
 * 负责传感器数据的存储和查询
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String TAG = "DatabaseHelper";
    
    // 数据库信息
    private static final String DATABASE_NAME = "bt_sensor.db";
    private static final int DATABASE_VERSION = 1;
    
    // 表名
    private static final String TABLE_SENSOR_DATA = "sensor_data";
    
    // 列名
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_TEMPERATURE = "temperature";
    private static final String COLUMN_TEMP_UNIT = "temp_unit";
    private static final String COLUMN_HUMIDITY = "humidity";
    private static final String COLUMN_HUM_UNIT = "hum_unit";
    private static final String COLUMN_LIGHT = "light";
    private static final String COLUMN_LIGHT_UNIT = "light_unit";
    private static final String COLUMN_WATER = "water";
    private static final String COLUMN_WATER_UNIT = "water_unit";
    private static final String COLUMN_CO2 = "co2";
    private static final String COLUMN_CO2_UNIT = "co2_unit";
    private static final String COLUMN_PH = "ph";
    private static final String COLUMN_PH_UNIT = "ph_unit";
    
    // 创建表SQL
    private static final String CREATE_TABLE_SENSOR_DATA = 
            "CREATE TABLE " + TABLE_SENSOR_DATA + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
            COLUMN_TEMPERATURE + " REAL, " +
            COLUMN_TEMP_UNIT + " TEXT, " +
            COLUMN_HUMIDITY + " REAL, " +
            COLUMN_HUM_UNIT + " TEXT, " +
            COLUMN_LIGHT + " REAL, " +
            COLUMN_LIGHT_UNIT + " TEXT, " +
            COLUMN_WATER + " REAL, " +
            COLUMN_WATER_UNIT + " TEXT, " +
            COLUMN_CO2 + " REAL, " +
            COLUMN_CO2_UNIT + " TEXT, " +
            COLUMN_PH + " REAL, " +
            COLUMN_PH_UNIT + " TEXT" +
            ")";
    
    // 单例模式
    private static DatabaseHelper instance;
    
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SENSOR_DATA);
        Log.d(TAG, "数据库表创建成功");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 简单处理：删除旧表，创建新表
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SENSOR_DATA);
        onCreate(db);
    }
    
    /**
     * 插入传感器数据
     */
    public long insertSensorData(SensorData data) {
        SQLiteDatabase db = getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_TIMESTAMP, data.getTimestamp());
        values.put(COLUMN_TEMPERATURE, data.getTemperature());
        values.put(COLUMN_TEMP_UNIT, data.getTempUnit());
        values.put(COLUMN_HUMIDITY, data.getHumidity());
        values.put(COLUMN_HUM_UNIT, data.getHumUnit());
        values.put(COLUMN_LIGHT, data.getLight());
        values.put(COLUMN_LIGHT_UNIT, data.getLightUnit());
        values.put(COLUMN_WATER, data.getWater());
        values.put(COLUMN_WATER_UNIT, data.getWaterUnit());
        values.put(COLUMN_CO2, data.getCo2());
        values.put(COLUMN_CO2_UNIT, data.getCo2Unit());
        values.put(COLUMN_PH, data.getPh());
        values.put(COLUMN_PH_UNIT, data.getPhUnit());
        
        long id = db.insert(TABLE_SENSOR_DATA, null, values);
        Log.d(TAG, "插入数据成功, ID: " + id);
        return id;
    }
    
    /**
     * 查询所有传感器数据
     */
    public List<SensorData> getAllSensorData() {
        List<SensorData> dataList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        String query = "SELECT * FROM " + TABLE_SENSOR_DATA + 
                " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                SensorData data = cursorToSensorData(cursor);
                dataList.add(data);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return dataList;
    }
    
    /**
     * 查询指定时间范围内的数据
     */
    public List<SensorData> getSensorDataByTimeRange(long startTime, long endTime) {
        List<SensorData> dataList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        String query = "SELECT * FROM " + TABLE_SENSOR_DATA + 
                " WHERE " + COLUMN_TIMESTAMP + " >= ? AND " + COLUMN_TIMESTAMP + " <= ?" +
                " ORDER BY " + COLUMN_TIMESTAMP + " ASC";
        
        Cursor cursor = db.rawQuery(query, new String[]{
                String.valueOf(startTime), 
                String.valueOf(endTime)
        });
        
        if (cursor.moveToFirst()) {
            do {
                SensorData data = cursorToSensorData(cursor);
                dataList.add(data);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return dataList;
    }
    
    /**
     * 查询最近N条数据
     */
    public List<SensorData> getRecentSensorData(int limit) {
        List<SensorData> dataList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        String query = "SELECT * FROM " + TABLE_SENSOR_DATA + 
                " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT ?";
        
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(limit)});
        
        if (cursor.moveToFirst()) {
            do {
                SensorData data = cursorToSensorData(cursor);
                dataList.add(data);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        
        // 反转列表，使时间顺序为升序
        List<SensorData> reversedList = new ArrayList<>();
        for (int i = dataList.size() - 1; i >= 0; i--) {
            reversedList.add(dataList.get(i));
        }
        
        return reversedList;
    }
    
    /**
     * 查询今天的数据
     */
    public List<SensorData> getTodaySensorData() {
        long now = System.currentTimeMillis();
        // 获取今天0点的时间戳
        long todayStart = now - (now % (24 * 60 * 60 * 1000));
        return getSensorDataByTimeRange(todayStart, now);
    }
    
    /**
     * 删除指定ID的数据
     */
    public int deleteSensorData(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_SENSOR_DATA, COLUMN_ID + " = ?", 
                new String[]{String.valueOf(id)});
    }
    
    /**
     * 删除所有数据
     */
    public int deleteAllSensorData() {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_SENSOR_DATA, null, null);
    }
    
    /**
     * 删除指定时间之前的数据
     */
    public int deleteOldData(long beforeTimestamp) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_SENSOR_DATA, 
                COLUMN_TIMESTAMP + " < ?", 
                new String[]{String.valueOf(beforeTimestamp)});
    }
    
    /**
     * 获取数据总数
     */
    public int getDataCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SENSOR_DATA, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
    
    /**
     * 将Cursor转换为SensorData对象
     */
    private SensorData cursorToSensorData(Cursor cursor) {
        SensorData data = new SensorData();
        
        data.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        data.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
        data.setTemperature(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_TEMPERATURE)));
        data.setTempUnit(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEMP_UNIT)));
        data.setHumidity(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_HUMIDITY)));
        data.setHumUnit(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HUM_UNIT)));
        data.setLight(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_LIGHT)));
        data.setLightUnit(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LIGHT_UNIT)));
        data.setWater(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_WATER)));
        data.setWaterUnit(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WATER_UNIT)));
        data.setCo2(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_CO2)));
        data.setCo2Unit(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CO2_UNIT)));
        data.setPh(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_PH)));
        data.setPhUnit(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PH_UNIT)));
        
        return data;
    }
}
