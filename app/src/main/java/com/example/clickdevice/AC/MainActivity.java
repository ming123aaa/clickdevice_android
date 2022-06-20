package com.example.clickdevice.AC;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.clickdevice.PowerKeyObserver;
import com.example.clickdevice.bean.Bean;
import com.example.clickdevice.MyService;
import com.example.clickdevice.SmallWindowView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.clickdevice.R;
import com.example.clickdevice.Util;
import com.example.clickdevice.dialog.DialogHelper;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private int OVERLAY_PERMISSION_REQ_CODE = 2;
    private WindowManager.LayoutParams btn_layoutParams;
    private LinearLayout btn_windowView;
    /* access modifiers changed from: private */
    public EditText editText_number;
    /* access modifiers changed from: private */
    public EditText editText_time;
    /* access modifiers changed from: private */
    public boolean isRun = false;
    private boolean isShow = false;
    private WindowManager.LayoutParams mLayoutParams;
    private Button btn_main;
    /* access modifiers changed from: private */
    @SuppressLint("HandlerLeak")
    public Handler myHandler = new Handler() {

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = msg.what;
            if (i == 0) {
                windowView.setwmParamsFlags(8);
                MainActivity.this.textView.setText("开始");
                MainActivity.this.isRun = false;
            } else if (i == 1) {
                Bean bean = (Bean) msg.obj;
                MainActivity.this.setMouseClick(bean.getX(), bean.getY());
            }else if (i==2){
                if (isShow){
                    windowView.setwmParamsFlags(8);
                }
                MainActivity.this.textView.setText("开始");
                MainActivity.this.isRun = false;
            }
        }
    };
    /* access modifiers changed from: private */
    public ExecutorService singleThreadExecutor;
    /* access modifiers changed from: private */
    public TextView textView;
    /* access modifiers changed from: private */
    public SmallWindowView windowView;
    private WindowManager wm;
    private long stopTime=0;
    private PowerKeyObserver powerKeyObserver;//检测电源键是否被按下

    /* access modifiers changed from: protected */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_main);
        initSmallViewLayout();
        this.editText_time = (EditText) findViewById(R.id.edit_time);
        this.editText_number = (EditText) findViewById(R.id.edit_number);
        btn_main=findViewById(R.id.btn_main);
        this.singleThreadExecutor = Executors.newSingleThreadExecutor();
        if (this.btn_windowView != null) {
            initBtnWindowsView();
        }
        DialogHelper.showMessagePositiveDialog(this, "辅助功能", "使用连点器需要开启(无障碍)辅助功能，是否现在去开启？"
                , new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        try {
                            startActivity(new Intent("android.settings.ACCESSIBILITY_SETTINGS"));
                        } catch (Exception e) {
                            startActivity(new Intent("android.settings.SETTINGS"));
                            e.printStackTrace();
                        }
                    }
                });

        powerKeyObserver=new PowerKeyObserver(this);
        powerKeyObserver.startListen();//开始注册广播
        powerKeyObserver.setHomeKeyListener(new PowerKeyObserver.OnPowerKeyListener() {
            @Override
            public void onPowerKeyPressed() {
                myHandler.sendEmptyMessage(2);
            }
        });
    }

    private void initBtnWindowsView() {
        TextView textView2 = (TextView) this.btn_windowView.findViewById(R.id.tv_win_b);
        this.textView = textView2;
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            @SuppressLint("WrongConstant")
            public void onClick(View v) {
                int num;
                int time;
                if (!MainActivity.this.isRun) {
                    if (stopTime+2000>System.currentTimeMillis()){
                        Toast.makeText(MainActivity.this,"点太快了,休息一下吧",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    MainActivity.this.isRun = true;
                    DisplayMetrics dm = MainActivity.this.getResources().getDisplayMetrics();
                    int screenHeight = dm.heightPixels;
                    int screenWidth = dm.widthPixels;
                    int statusHeight = Util.getStatusHeight(MainActivity.this);
                    int x = MainActivity.this.windowView.getActionUpX();
                    int y = MainActivity.this.windowView.getActionUpY();
                    if (((x < 0) | (x > Math.max(screenHeight, screenWidth)) | (y < 0)) || (y > Math.max(screenHeight, screenWidth))) {
                        MainActivity.this.isRun = false;
                    } else if (!MyService.isStart()) {
                        MainActivity.this.isRun = false;
                        Toast.makeText(MainActivity.this, "请手动开启辅助功能，若已开启请重启应用再试一次。", Toast.LENGTH_LONG).show();
                    } else {
                        MainActivity.this.textView.setText("停止");
                        String s1=editText_number.getText().toString();
                        String s2=editText_time.getText().toString();
                        int num2 = Integer.parseInt(TextUtils.isEmpty(s1)?"0":s1);
                        int time2 = Integer.parseInt(TextUtils.isEmpty(s2)?"1000":s2);
                        if (num2 < 0) {
                            num = 0;
                        } else {
                            num = num2;
                        }
                        if (time2 < 30) {
                            time = 30;
                        } else {
                            time = time2;
                        }

                        final int finalNum = num;



                        final int x2 = x;
                        final int y2 = y;

                        windowView.setwmParamsFlags(24);

                        singleThreadExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                for (int i = 0; i < finalNum && MainActivity.this.isRun; i++) {
                                    Message message = new Message();
                                    message.obj = new Bean(x2, y2);
                                    message.what = 1;
                                    MainActivity.this.myHandler.sendMessage(message);
                                    for (int t = 0; t < time / 10 && MainActivity.this.isRun; t++) {
                                        try {
                                            Thread.sleep(10);
                                        } catch (InterruptedException e2) {
                                            e2.printStackTrace();
                                        }
                                    }
                                    try {
                                        Thread.sleep(time % 10);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                MainActivity.this.myHandler.sendEmptyMessageDelayed(0, 200);
                            }
                        });

                    }
                } else {
                    stopTime=System.currentTimeMillis();
                    MainActivity.this.isRun = false;
                }
            }
        });

        textView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
               if (isRun){
                   stopTime=System.currentTimeMillis();
                   MainActivity.this.isRun = false;
                   return true;
               }
                return false;
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void setMouseClick(int x, int y) {
        MyService myService = MyService.myService;
        if (myService != null) {
            myService.dispatchGestureClick((float) x, (float) y);
        }
    }

    @SuppressLint("WrongConstant")
    public void initSmallViewLayout() {
        this.windowView = (SmallWindowView) LayoutInflater.from(this).inflate(R.layout.window_a, (ViewGroup) null);
        this.btn_windowView = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.window_b, (ViewGroup) null);
        this.wm = (WindowManager) getSystemService("window");
        this.mLayoutParams = new WindowManager.LayoutParams(-2, -2, 2003, 8, -3);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-2, -2, 2003, 8, -3);
        this.btn_layoutParams = layoutParams;
        layoutParams.gravity = 49;
        this.mLayoutParams.gravity = 0;
        this.windowView.setWm(this.wm);
        this.windowView.setWmParams(this.mLayoutParams);
    }

    public void alertWindow() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (Build.VERSION.SDK_INT >= 26) {
                this.mLayoutParams.type = 2038;
                this.btn_layoutParams.type = 2038;
            }
            requestDrawOverLays();
        } else if (Build.VERSION.SDK_INT >= 21) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.SYSTEM_ALERT_WINDOW"}, 1);
        }
    }

    public void dismissWindow() {
        LinearLayout linearLayout;
        SmallWindowView smallWindowView;
        if (!(this.wm == null || (smallWindowView = this.windowView) == null || smallWindowView.getWindowId() == null)) {
            this.wm.removeView(this.windowView);
        }
        if (this.wm != null && (linearLayout = this.btn_windowView) != null && linearLayout.getWindowId() != null) {
            this.wm.removeView(this.btn_windowView);
        }
    }

    public void showWindow() {
        if (this.wm != null && this.windowView.getWindowId() == null) {
            this.wm.addView(this.windowView, this.mLayoutParams);
        }
        if (this.wm != null && this.btn_windowView.getWindowId() == null) {
            this.wm.addView(this.btn_windowView, this.btn_layoutParams);
        }
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestDrawOverLays() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "can not DrawOverlays", 0).show();
            startActivityForResult(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + getPackageName())), this.OVERLAY_PERMISSION_REQ_CODE);
            return;
        }
        showWindow();

    }

    /* access modifiers changed from: protected */
    @SuppressLint("WrongConstant")
    @Override
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != this.OVERLAY_PERMISSION_REQ_CODE) {
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "设置权限拒绝", 0).show();
        } else {
            Toast.makeText(this, "设置权限成功", 0).show();
        }
    }

    public void startClickDevice(View view) {

        if (!this.isShow) {
            showFloatWindows(btn_main);
        } else {
            hideFloatWindows(btn_main);
        }
    }

    private void showFloatWindows(Button button){
        alertWindow();
        this.isShow = true;
        button.setText("隐藏悬浮窗");
    }

    private void hideFloatWindows(Button button){
        this.isShow = false;
        button.setText("打开连点器");
        dismissWindow();
    }

    public void openAccessibility(View view) {
        try {
            startActivity(new Intent("android.settings.ACCESSIBILITY_SETTINGS"));
        } catch (Exception e) {
            startActivity(new Intent("android.settings.SETTINGS"));
            e.printStackTrace();
        }
    }

    public void startScriptAc(View view) {
        startActivity(new Intent(this, ScriptActivity.class));
        hideFloatWindows(btn_main);
    }


    public void startRecordAc(View view) {
        startActivity(new Intent(this, RecordScriptListActivity.class));
        hideFloatWindows(btn_main);
    }
}