package com.wp.bt.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.wp.bt.model.SensorData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * SQLite数据库帮助类 - 通用版本
 * 使用JSON字符串存储动态传感器数据
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String TAG = "DatabaseHelper";
    
    // 数据库信息
    private static final String DATABASE_NAME = "bt_sensor.db";
    private static final int DATABASE_VERSION = 2; // 版本升级
    
    // 表名
    private static final String TABLE_SENSOR_DATA = "sensor_data";
    
    // 列名 - 简化为只存储时间戳和原始JSON
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_RAW_JSON = "raw_json";
    
    // 创建表SQL
    private static final String CREATE_TABLE_SENSOR_DATA = 
            "CREATE TABLE " + TABLE_SENSOR_DATA + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
            COLUMN_RAW_JSON + " TEXT" +
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
        // 删除旧表，创建新表
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
        values.put(COLUMN_RAW_JSON, data.getRawJson());
        
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
                if (data != null) {
                    dataList.add(data);
                }
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
                if (data != null) {
                    dataList.add(data);
                }
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
                if (data != null) {
                    dataList.add(data);
                }
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
     * 从JSON字符串重建动态数据
     */
    private SensorData cursorToSensorData(Cursor cursor) {
        try {
            SensorData data = new SensorData();
            
            data.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            data.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
            
            String rawJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RAW_JSON));
            data.setRawJson(rawJson);
            
            // 从JSON重建数据项
            if (rawJson != null && !rawJson.isEmpty()) {
                JSONObject json = new JSONObject(rawJson);
                if (json.has("Date")) {
                    JSONObject dateObj = json.getJSONObject("Date");
                    Iterator<String> keys = dateObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        Object valueObj = dateObj.get(key);
                        
                        if (valueObj instanceof JSONObject) {
                            JSONObject itemObj = (JSONObject) valueObj;
                            String value = String.valueOf(itemObj.opt("val"));
                            String unit = itemObj.optString("unit", "");
                            data.addItem(key, value, unit);
                        } else {
                            data.addItem(key, String.valueOf(valueObj), "");
                        }
                    }
                }
            }
            
            return data;
        } catch (JSONException e) {
            Log.e(TAG, "解析数据库JSON失败", e);
            return null;
        }
    }
}
