package com.example.clickdevice.db;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase appDatabase;
    public abstract ScriptDao getScriptDao();

    public static AppDatabase getInstance(Context context){
        if (appDatabase==null){
          appDatabase=Room.databaseBuilder(context,AppDatabase.class,"person_info.db")
                    .build();
        }
        return appDatabase;
    }
}
