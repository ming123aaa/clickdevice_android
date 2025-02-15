package com.example.clickdevice;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.example.clickdevice.db.AppDatabase;

public class MyApp extends Application {
    public static final String TAG="MyApp";
    private AppDatabase appDatabase;
    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context=this;
        appDatabase=AppDatabase.getInstance(this);
        try {
            setMyServiceEnable();
        }catch (Exception ignored){
            ignored.printStackTrace();
        }

    }

    /**
     * 需要授予权限 android.permission.WRITE_SECURE_SETTINGS
     * adb shell pm grant 包名 android.permission.WRITE_SECURE_SETTINGS
     */
    private void setMyServiceEnable() {
        String name = getPackageName()+"/"+ MyService.class.getName();

        String string = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (string==null){
            string="";
        }
        StringBuffer stringBuffer=new StringBuffer(string);
        if (!string.contains(name)){
            String s = stringBuffer.append(":").append(name).toString();
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,s);
        }
    }


    public AppDatabase getAppDatabase(){
        return appDatabase;
    }
}
