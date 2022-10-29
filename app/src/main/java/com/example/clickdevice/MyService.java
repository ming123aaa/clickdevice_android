package com.example.clickdevice;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

public class MyService extends AccessibilityService {
    public static final String TAG = "MyService";
    private List<AccessibilityNodeInfo> nodeList = new ArrayList<>();
    public static MyService myService;

    public MyService() {
        Log.e(TAG, "MyService: ");
    }

    public MutableLiveData<String> pkgNameMutableLiveData = new MutableLiveData<>();




    @Override
    public void onCreate() {
        super.onCreate();
        myService = this;
        Log.e(TAG, "onCreate: ");
    }

    public static boolean isStart() {
        return myService != null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void dispatchGestureClick(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        path.lineTo(x + 1, y + 1);
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
                (path, 0, 20)).build(), null, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void dispatchGestureClick(float x, float y, int duration) {
        Path path = new Path();
        path.moveTo(x, y);
        path.lineTo(x + 1, y + 1);
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
                (path, 0, duration)).build(), null, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void dispatchGesture(float x1, float y1, float x2, float y2, int duration) {
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
                (path, 0, duration)).build(), null, null);
    }


    public void dispatchGesture(Path path, int duration) {
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
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null && event.getClassName() != null) {
                pkgNameMutableLiveData.setValue(event.getPackageName().toString());

//                ComponentName componentName = new ComponentName(
//                        event.getPackageName().toString(),
//                        event.getClassName().toString()
//                );
//
//                ActivityInfo activityInfo = tryGetActivity(componentName);
//                boolean isActivity = activityInfo != null;
//                if (isActivity) {
//                    String s=componentName.flattenToString();
//                    Log.d(TAG, "onAccessibilityEvent: "+s);
//                    pkgNameMutableLiveData.setValue(s);
//                }
            }
        }
    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "onInterrupt: ");
    }

    public void recycle(AccessibilityNodeInfo info) {
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


    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        setServiceInfo(config);
//        createWindow();
    }

//    public void createWindow() {
//        //悬浮窗类型
//        int type;
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
//            type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
//        } else {
//            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
//        }
//
//        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
//        lp.format= PixelFormat.TRANSLUCENT;
//        lp.type = type;
//        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
//                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
//                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
//                WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW;
//        LinearLayout linearLayout = new LinearLayout(this);
//        linearLayout.setBackgroundColor(0x80000000);
//        wm.addView(linearLayout,lp);
//
//    }

}
