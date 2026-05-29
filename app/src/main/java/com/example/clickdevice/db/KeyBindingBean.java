package com.example.clickdevice.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "key_binding")
public class KeyBindingBean {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String keyName;

    public String keyDescription;

    public String scriptType;

    public int scriptId;

    public String scriptName;

    public String scriptParams;

    public int textColor;

    public int textSize;

    public int windowX=0;

    public int windowY=0;

    public boolean windowLocked=false;

    public String createTime;

    public String updateTime;


}