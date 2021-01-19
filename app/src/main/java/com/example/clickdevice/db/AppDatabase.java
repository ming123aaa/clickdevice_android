package com.example.clickdevice.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
@Database(entities = {ScriptDataBean.class} ,version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase appDatabase;
    public abstract ScriptDao getScriptDao();

    public static AppDatabase getInstance(Context context){
        if (appDatabase==null){
          appDatabase=Room.databaseBuilder(context,AppDatabase.class,"script_info.db")
                    .build();
        }
        return appDatabase;
    }
}
