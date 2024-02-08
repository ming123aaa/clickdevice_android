package com.example.clickdevice.activity;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;

import com.Ohuang.ilivedata.MyLiveData;
import com.example.clickdevice.MyService;
import com.example.clickdevice.PowerKeyObserver;
import com.example.clickdevice.R;
import com.example.clickdevice.ScriptExecutor;
import com.example.clickdevice.Util;
import com.example.clickdevice.bean.ActionScript;
import com.example.clickdevice.bean.ScriptCmdBean;
import com.example.clickdevice.bean.ScriptGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScriptGroupPlayActivity extends AppCompatActivity implements ScriptExecutor.ScriptInterFace {
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
                ScriptGroupPlayActivity.this.isRun = false;
                ScriptGroupPlayActivity.this.tv_bw.setText("开始");
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
            for (int j = 0; j < ScriptGroupPlayActivity.this.num; j++) {

                scriptExecutor.run(mData);

                int i2 = 0;
                while (i2 < ScriptGroupPlayActivity.this.time / 100) {
                    if (ScriptGroupPlayActivity.this.isRun) {
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
                    Thread.sleep((long) (ScriptGroupPlayActivity.this.time % 100));
                } catch (InterruptedException e3) {
                    e3.printStackTrace();
                }
            }
            ScriptGroupPlayActivity.this.handler.sendEmptyMessage(0);
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

    private Spinner spinner_script;

    //延迟系数
    private double delayCoefficient=1d;

    /* access modifiers changed from: protected */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_script_group_play);
        initSmallViewLayout();
        this.editText_num = (EditText) findViewById(R.id.edit_script_number);
        this.editText_time = (EditText) findViewById(R.id.edit_script_time);
        btn_script_openScript = findViewById(R.id.btn_script_openScript);
        textView = findViewById(R.id.tv_script_code);
        tv_name = findViewById(R.id.tv_name);
        tv_copy = findViewById(R.id.tv_copy);
        checkBox = findViewById(R.id.cb_reChange_app);
        spinner_script = findViewById(R.id.spinner_script);
        tv_copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Util.copyText(textView.getText(), ScriptGroupPlayActivity.this);
                    Toast.makeText(ScriptGroupPlayActivity.this, "复制成功", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(ScriptGroupPlayActivity.this, "复制失败" + e.toString(), Toast.LENGTH_LONG).show();
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

    ScriptGroup mScriptGroup;

    private void initScriptGroup(ScriptGroup scriptGroup) {
        tv_name.setText("脚本名称:" + scriptGroup.getName());

        List<String> stringStream = scriptGroup.getActionScript()
                .stream()
                .map(actionScript -> actionScript.getName())
                .collect(Collectors.toList());
        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, stringStream);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_script.setAdapter(arrayAdapter);
        spinner_script.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ActionScript actionScript = scriptGroup.getActionScript().get(position);
                mData = scriptGroup.getListScriptCmdBean(actionScript);
                textView.setText(actionScript.getScript().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initEvent() {
        MyLiveData.getInstance().with("ScriptGroup", ScriptGroup.class).observe(this,
                new Observer<ScriptGroup>() {
                    @Override
                    public void onChanged(ScriptGroup scriptGroup) {
                        mScriptGroup = scriptGroup;
                        initScriptGroup(scriptGroup);


                    }
                }
        );


        if (MyService.isStart()) {
            observer = s -> {
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

    private void setDelayCoefficient(){
        EditText viewById = findViewById(R.id.edit_speed);
        String s = viewById.getText().toString();
        try {
            Double aDouble = Double.valueOf(s);
            if (aDouble<0.25){
                aDouble=0.25;
            }
            if (aDouble>5){
                aDouble=5.0;
            }
            delayCoefficient=1.0/aDouble;
        }catch (Throwable e){
            delayCoefficient=1;
        }
    }

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
                        Toast.makeText(ScriptGroupPlayActivity.this, "请手动开启辅助功能，若已开启请重启应用再试一次。", Toast.LENGTH_LONG).show();
                    } else if (mData == null) {
                        Toast.makeText(ScriptGroupPlayActivity.this, "请选择要执行的脚本", 0).show();
                    } else if (stopTime + 2000 > System.currentTimeMillis()) {
                        Toast.makeText(ScriptGroupPlayActivity.this, "点太快了,休息一下吧", Toast.LENGTH_SHORT).show();
                    } else {
                        String s1 = editText_time.getText().toString();
                        String s2 = editText_num.getText().toString();
                        checkAppChange = checkBox.isChecked();
                        thisPkgName = pkgNameNow;
                        time = Integer.parseInt(TextUtils.isEmpty(s1) ? "1000" : s1);
                        num = Integer.parseInt(TextUtils.isEmpty(s2) ? "0" : s2);
                        tv_bw.setText("停止");
                        setDelayCoefficient();
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
        this.wm = (WindowManager) getApplicationContext().getSystemService("window");
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-2, -2, 2003, 8, -3);
        this.btn_layoutParams = layoutParams;
        layoutParams.gravity = 49;
    }

    @Override
    public void delayedCmd(int delayed) throws InterruptedException {
        int time= (int) (delayCoefficient*delayed);
        for (int i = 0; i < time / 10 && this.isRun; i++) {
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
            if (duration < 30) {
                myService2.dispatchGestureClick((float) x0, (float) y0);
                Thread.sleep(30);
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
            if (duration < 100) {
                myService2.dispatchGesture((float) x0, (float) y0, (float) x1, (float) y1, 200);
                Thread.sleep(100);
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
        if (MyService.isStart()) {
            MyService.myService.pkgNameMutableLiveData.removeObserver(observer);
        }
        powerKeyObserver.stopListen();
        super.onDestroy();
    }
}