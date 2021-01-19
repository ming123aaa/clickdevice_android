package com.example.clickdevice.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "script")
public class ScriptDataBean {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;

    private String createTime;

    private String updateTime;

    private String scriptJson;

    public ScriptDataBean() {
    }

    @Ignore
    public ScriptDataBean(String name, String createTime, String updateTime, String scriptJson) {
        this.name = name;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.scriptJson = scriptJson;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStringId(){
        return id+"";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getScriptJson() {
        return scriptJson;
    }

    public void setScriptJson(String scriptJson) {
        this.scriptJson = scriptJson;
    }
}
