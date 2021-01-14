package com.example.clickdevice;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

public class MyService extends AccessibilityService {
    public static final String TAG = "MyService";
    private  List<AccessibilityNodeInfo> nodeList = new ArrayList<>();
    public static MyService myService;
    public MyService() {
        Log.e(TAG, "MyService: ");
    }



    @Override
    public void onCreate() {
        super.onCreate();
         myService=this;
        Log.e(TAG, "onCreate: ");
    }

    public static boolean isStart(){
        return myService!=null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void dispatchGestureClick(float x, float y){
        Path path = new Path();
        path.moveTo(x,y);
        path.lineTo(x + 1, y + 1);
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
                (path, 0, 50)).build(), null, null);
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void dispatchGestureClick(float x, float y,int duration){
        Path path = new Path();
        path.moveTo(x,y);
        path.lineTo(x + 1, y + 1);
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
                (path, 0, duration)).build(), null, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void dispatchGesture(float x1,float y1,float x2,float y2,int duration){
        Path path = new Path();
        path.moveTo(x1,y1);
        path.lineTo(x2 , y2);
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
                (path, 0, duration)).build(), null, null);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.e(TAG, "onRebind: ");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: ");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand: ");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind: ");
        return super.onUnbind(intent);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        return super.onKeyEvent(event);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() != null) {

            String pkgName = event.getPackageName().toString();
            String className = event.getClassName().toString();
//
//            Log.e(TAG, "onAccessibilityEvent:  pkgName = " + pkgName);
//            Log.e(TAG, "onAccessibilityEvent:  className = " + className);
        }

    }


    @Override
    public void onInterrupt() {
        Log.e(TAG, "onInterrupt: ");
    }

    public  void recycle(AccessibilityNodeInfo info) {
        if (info.getChildCount() == 0) {
            Log.i(TAG, "child widget:" + info.getClassName());
            Log.i(TAG, "showDialog:" + info.canOpenPopup());
            Log.i(TAG, "Text：" + info.getText());
            Log.i(TAG, "windowId:" + info.getWindowId());
            //添加至节点列表
            nodeList.add(info);
        } else {
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    recycle(info.getChild(i));
                }
            }
        }
    }



}
