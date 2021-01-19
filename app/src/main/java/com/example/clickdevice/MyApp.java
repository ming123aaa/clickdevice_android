package com.example.clickdevice;

import android.app.Application;
import android.content.Context;

import com.example.clickdevice.db.AppDatabase;

public class MyApp extends Application {
    private AppDatabase appDatabase;
    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context=this;
        appDatabase=AppDatabase.getInstance(this);
    }

    public AppDatabase getAppDatabase(){
        return appDatabase;
    }
}
