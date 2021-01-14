package com.example.clickdevice.AC;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.clickdevice.MyService;
import com.example.clickdevice.ScriptCmdBean;
import com.example.clickdevice.ScriptExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.clickdevice.R;

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
                boolean unused = ScriptActivity.this.isRun = false;
                ScriptActivity.this.tv_bw.setText("开始");
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean isRun = false;
    private boolean isShow = false;
    /* access modifiers changed from: private */
    public List<ScriptCmdBean> mData;
    private MyService myService;
    /* access modifiers changed from: private */
    public int num;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            for (int j = 0; j < ScriptActivity.this.num; j++) {
                for (int i = 0; i < ScriptActivity.this.mData.size(); i++) {
                    try {
                        ScriptActivity.this.scriptExecutor.Run((ScriptCmdBean) ScriptActivity.this.mData.get(i));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
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

    /* access modifiers changed from: protected */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_script);
        initSmallViewLayout();
        this.editText_num = (EditText) findViewById(R.id.edit_script_number);
        this.editText_time = (EditText) findViewById(R.id.edit_script_time);
        this.mData = new ArrayList();
        this.singleThreadExecutor = Executors.newSingleThreadExecutor();
        if (this.btn_windowView != null) {
            initBtnWindowsView();
        }
        this.scriptExecutor = new ScriptExecutor(this);
    }

    public void startScriptWindow(View view) {
        if (!this.isShow) {
            alertWindow();
            this.isShow = true;
            return;
        }
        this.isShow = false;
        dismissWindow();
    }

    public void selectScript(View view) {
        this.mData.add(ScriptCmdBean.BuildClickCMD(100, 500, 0));
        this.mData.add(ScriptCmdBean.BuildDelayedCMD(1000));
        this.mData.add(ScriptCmdBean.BuildGestureCMD(200, 200, 1000, 1000, 2000));
        this.mData.add(ScriptCmdBean.BuildGestureCMD(1000, 200, 200, 1000, 2000));
    }

    private void initBtnWindowsView() {
        TextView textView = (TextView) this.btn_windowView.findViewById(R.id.tv_win_b);
        this.tv_bw = textView;
        textView.setOnClickListener(v->  {
                if (isRun) {
                    isRun = false;
                    handler.sendEmptyMessage(0);
                } else if (!MyService.isStart()) {
                    Toast.makeText(ScriptActivity.this, "请打开辅助功能", 0).show();
                } else if (mData == null) {
                    Toast.makeText(ScriptActivity.this, "请选择要执行的脚本", 0).show();
                } else {
                    time = Integer.parseInt(editText_time.getText().toString());
                    num = Integer.parseInt(editText_num.getText().toString());
                    tv_bw.setText("停止");
                    isRun = true;
                    singleThreadExecutor.execute(runnable);
                }
            }
        );
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
        Toast.makeText(this, "权限已经授予", 0).show();
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
                for (int i = 0; i < duration / 100; i++) {
                    Thread.sleep(100);
                }
            } else {
                myService2.dispatchGestureClick((float) x0, (float) y0, 30000);
                for (int i2 = 0; i2 < 300; i2++) {
                    Thread.sleep(100);
                }
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
                for (int i = 0; i < 300; i++) {
                    Thread.sleep(100);
                }
                return;
            }
            myService2.dispatchGesture((float) x0, (float) y0, (float) x1, (float) y1, duration);
            for (int i2 = 0; i2 < duration / 100; i2++) {
                Thread.sleep(100);
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override
    public void onDestroy() {
        dismissWindow();
        super.onDestroy();
    }
}