package com.example.clickdevice.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "record")
public class RecordScriptBean {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;

    public String createTime;

    public String updateTime;

    public String scriptJson;

}
