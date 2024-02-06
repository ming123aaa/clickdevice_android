package com.example.clickdevice.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "script_group")
public class ScriptGroupBean {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;

    public String createTime;

    public String updateTime;

    public String scriptJson;
}
