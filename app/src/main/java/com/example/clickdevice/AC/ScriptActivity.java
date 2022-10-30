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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;

import com.Ohuang.ilivedata.LiveDataBus;
import com.example.clickdevice.MyService;
import com.example.clickdevice.PowerKeyObserver;
import com.example.clickdevice.Util;
import com.example.clickdevice.bean.ScriptCmdBean;
import com.example.clickdevice.ScriptExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.clickdevice.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ScriptActivity extends AppCompatActivity implements ScriptExecutor.ScriptInterFace {
    private int OVERLAY_PERMISSION_REQ_CODE = 2;
    private WindowManager.LayoutParams btn_layoutParams;
    private LinearLayout btn_windowView;
    private EditText editText_num;
    private EditText editText_time;
    /* access modifiers changed from: private */
    @SuppressLint("HandlerLeak")
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                ScriptActivity.this.isRun = false;
                ScriptActivity.this.tv_bw.setText("开始");
            }
        }
    };
    /* access modifiers changed from: private */
    public volatile boolean isRun = false;
    private boolean isShow = false;
    /* access modifiers changed from: private */
    public List<ScriptCmdBean> mData;
    private MyService myService;
    /* access modifiers changed from: private */
    public int num;
    private String thisPkgName = "";
    private String pkgNameNow = "";
    private boolean checkAppChange = false;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            for (int j = 0; j < ScriptActivity.this.num; j++) {

                scriptExecutor.run(mData);

                int i2 = 0;
                while (i2 < ScriptActivity.this.time / 100) {
                    if (ScriptActivity.this.isRun) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e2) {
                            e2.printStackTrace();
                        }
                        i2++;
                    } else {
                        return;
                    }
                }
                try {
                    Thread.sleep((long) (ScriptActivity.this.time % 100));
                } catch (InterruptedException e3) {
                    e3.printStackTrace();
                }
            }
            ScriptActivity.this.handler.sendEmptyMessage(0);
        }
    };
    /* access modifiers changed from: private */
    public ScriptExecutor scriptExecutor;
    private ExecutorService singleThreadExecutor;
    /* access modifiers changed from: private */
    public int time;
    /* access modifiers changed from: private */
    public TextView tv_bw;
    private WindowManager wm;
    private TextView textView;
    private TextView tv_name;
    private TextView tv_copy;
    private CheckBox checkBox;
    private String json;
    private Button btn_script_openScript;
    private PowerKeyObserver powerKeyObserver;//检测电源键是否被按下
    private Observer<String> observer;

    /* access modifiers changed from: protected */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_script);
        initSmallViewLayout();
        this.editText_num = (EditText) findViewById(R.id.edit_script_number);
        this.editText_time = (EditText) findViewById(R.id.edit_script_time);
        btn_script_openScript = findViewById(R.id.btn_script_openScript);
        textView = findViewById(R.id.tv_script_code);
        tv_name = findViewById(R.id.tv_name);
        tv_copy = findViewById(R.id.tv_copy);
        checkBox = findViewById(R.id.cb_reChange_app);
        tv_copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Util.copyText(textView.getText(), ScriptActivity.this);
                    Toast.makeText(ScriptActivity.this, "复制成功", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(ScriptActivity.this, "复制失败" + e.toString(), Toast.LENGTH_LONG).show();
                }

            }
        });
        this.singleThreadExecutor = Executors.newSingleThreadExecutor();
        if (this.btn_windowView != null) {
            initBtnWindowsView();
        }
        this.scriptExecutor = new ScriptExecutor(this);
        initEvent();

        powerKeyObserver = new PowerKeyObserver(this);
        powerKeyObserver.startListen();//h开始注册广播
        powerKeyObserver.setHomeKeyListener(new PowerKeyObserver.OnPowerKeyListener() {
            @Override
            public void onPowerKeyPressed() {
                handler.sendEmptyMessage(0);
            }
        });
    }

    private void initEvent() {
        LiveDataBus.get().with("json", String.class).observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                json = s;
                textView.setText(json);
                Gson gson = new Gson();
                List<ScriptCmdBean> list = gson.fromJson(json, new TypeToken<List<ScriptCmdBean>>() {
                }.getType());
                if (list == null || list.size() == 0) {
                    tv_copy.setVisibility(View.GONE);
                    Toast.makeText(ScriptActivity.this, "脚本为空或json格式有问题", Toast.LENGTH_SHORT).show();
                }
                tv_copy.setVisibility(View.VISIBLE);
                mData = list;
            }
        });
        LiveDataBus.get().with("scriptName", String.class).observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if (TextUtils.isEmpty(s)) {
                    tv_name.setText("");
                } else {
                    tv_name.setText("脚本名称:" + s);
                }
            }
        });
        if (MyService.isStart()) {
            observer= s -> {
                Log.d("TAG123", "onChanged: " + s);
                pkgNameNow = s;
                if (checkAppChange) {
                    if (!pkgNameNow.equals(thisPkgName)) {
                        if (isRun) {
                            stopTime = System.currentTimeMillis();
                            isRun = false;
                            handler.sendEmptyMessage(0);
                        }
                    }
                }
            };
            MyService.myService.pkgNameMutableLiveData.observeForever(observer);
        }
    }

    public void startScriptWindow(View view) {

        if (!this.isShow) {
            showFloatWindows(btn_script_openScript);
        } else {
            hideFloatWindows(btn_script_openScript);
        }

    }

    private void showFloatWindows(Button button) {
        alertWindow();
        this.isShow = true;
        button.setText("关闭悬浮窗");
    }

    private void hideFloatWindows(Button button) {
        this.isShow = false;
        button.setText("打开脚本");
        dismissWindow();
    }

    public void selectScript(View view) {
        startActivity(new Intent(this, ScriptListActivity.class));
        hideFloatWindows(btn_script_openScript);
    }

    private long stopTime = 0;

    @SuppressLint("WrongConstant")
    private void initBtnWindowsView() {
        TextView textView = (TextView) this.btn_windowView.findViewById(R.id.tv_win_b);
        this.tv_bw = textView;
        textView.setOnClickListener(v -> {
                    if (isRun) {
                        stopTime = System.currentTimeMillis();
                        isRun = false;
                        handler.sendEmptyMessage(0);

                    } else if (!MyService.isStart()) {
                        Toast.makeText(ScriptActivity.this, "请手动开启辅助功能，若已开启请重启应用再试一次。", Toast.LENGTH_LONG).show();
                    } else if (mData == null) {
                        Toast.makeText(ScriptActivity.this, "请选择要执行的脚本", 0).show();
                    } else if (stopTime + 2000 > System.currentTimeMillis()) {
                        Toast.makeText(ScriptActivity.this, "点太快了,休息一下吧", Toast.LENGTH_SHORT).show();
                    } else {
                        String s1 = editText_time.getText().toString();
                        String s2 = editText_num.getText().toString();
                        checkAppChange = checkBox.isChecked();
                        thisPkgName = pkgNameNow;
                        time = Integer.parseInt(TextUtils.isEmpty(s1) ? "1000" : s1);
                        num = Integer.parseInt(TextUtils.isEmpty(s2) ? "0" : s2);
                        tv_bw.setText("停止");
                        isRun = true;
                        singleThreadExecutor.execute(runnable);
                    }
                }
        );
        textView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isRun) {
                    stopTime = System.currentTimeMillis();
                    isRun = false;
                    handler.sendEmptyMessage(0);
                    return true;
                }
                return false;
            }
        });
    }


    public void alertWindow() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (Build.VERSION.SDK_INT >= 26) {
                this.btn_layoutParams.type = 2038;
            }
            requestDrawOverLays();
        } else if (Build.VERSION.SDK_INT >= 21) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.SYSTEM_ALERT_WINDOW"}, 1);
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

    public void showWindow() {
        if (this.wm != null && this.btn_windowView.getWindowId() == null) {
            this.wm.addView(this.btn_windowView, this.btn_layoutParams);
        }
    }

    public void dismissWindow() {
        LinearLayout linearLayout;
        if (this.wm != null && (linearLayout = this.btn_windowView) != null && linearLayout.getWindowId() != null) {
            this.wm.removeView(this.btn_windowView);
        }
    }

    @SuppressLint("WrongConstant")
    public void initSmallViewLayout() {
        this.btn_windowView = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.window_b, (ViewGroup) null);
        this.wm = (WindowManager) getSystemService("window");
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-2, -2, 2003, 8, -3);
        this.btn_layoutParams = layoutParams;
        layoutParams.gravity = 49;
    }

    @Override
    public void delayedCmd(int delayed) throws InterruptedException {
        for (int i = 0; i < delayed / 10 && this.isRun; i++) {
            Thread.sleep(10);
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void clickCMD(int x0, int y0, int duration) throws InterruptedException {
        if (this.isRun) {
            if (this.myService == null) {
                this.myService = MyService.myService;
            }
            MyService myService2 = this.myService;
            if (myService2 == null) {
                return;
            }
            if (duration < 50) {
                myService2.dispatchGestureClick((float) x0, (float) y0);
                Thread.sleep(50);
            } else if (duration < 30000) {
                myService2.dispatchGestureClick((float) x0, (float) y0, duration);
                Thread.sleep(duration);
            } else {
                myService2.dispatchGestureClick((float) x0, (float) y0, 30000);
                Thread.sleep(30000);
            }
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void gestureCMD(int x0, int y0, int x1, int y1, int duration) throws InterruptedException {
        if (this.isRun) {
            if (this.myService == null) {
                this.myService = MyService.myService;
            }
            MyService myService2 = this.myService;
            if (myService2 == null) {
                return;
            }
            if (duration > 30000) {
                myService2.dispatchGesture((float) x0, (float) y0, (float) x1, (float) y1, 30000);
                Thread.sleep(30000);
                return;
            }
            if (duration < 200) {
                myService2.dispatchGesture((float) x0, (float) y0, (float) x1, (float) y1, 200);
                Thread.sleep(200);
                return;
            }
            myService2.dispatchGesture((float) x0, (float) y0, (float) x1, (float) y1, duration);
            Thread.sleep(duration);
        }
    }

    /* access modifiers changed from: protected */
    @Override
    public void onDestroy() {
        hideFloatWindows(btn_script_openScript);
        if(MyService.isStart()) {
            MyService.myService.pkgNameMutableLiveData.removeObserver(observer);
        }
        powerKeyObserver.stopListen();
        super.onDestroy();
    }
}