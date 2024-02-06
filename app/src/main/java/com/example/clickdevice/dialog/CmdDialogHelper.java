package com.example.clickdevice.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.example.clickdevice.R;
import com.example.clickdevice.SmallWindowView;
import com.example.clickdevice.bean.ScriptCmdBean;

public class CmdDialogHelper {

    private WindowManager.LayoutParams mLayoutParams;
    public SmallWindowView windowView;
    private WindowManager wm;
    private EditText editText_x, editText_y;

    private WindowManager.LayoutParams btn_layoutParams;
    private LinearLayout btn_windowView;
    private TextView tv_btnWv;

    Activity context;

    public CmdDialogHelper(Activity context) {
        this.context = context;
        initSmallViewLayout();
        initbtnWindows();
    }

    @SuppressLint("WrongConstant")
    private void initSmallViewLayout() {
        this.windowView = (SmallWindowView) LayoutInflater.from(context).inflate(R.layout.window_a, (ViewGroup) null);
        this.btn_windowView = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.window_b, (ViewGroup) null);
        this.wm = (WindowManager) context.getApplicationContext().getSystemService("window");
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
            ActivityCompat.requestPermissions(context, new String[]{"android.permission.SYSTEM_ALERT_WINDOW"}, 1);
        }
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestDrawOverLays() {
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "can not DrawOverlays", 0).show();
            context.startActivityForResult(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + context.getPackageName())), 2);
            return;
        }
        showWindow();
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


    private void initbtnWindows() {
        if (btn_windowView != null) {

            tv_btnWv = btn_windowView.findViewById(R.id.tv_win_b);
            tv_btnWv.setText("完成");
            tv_btnWv.setOnClickListener(v -> {
                if (editText_x != null && editText_y != null) {
                    if (windowView != null) {
                        int x = windowView.getActionUpX();
                        int y = windowView.getActionUpY();
                        editText_x.setText(x + "");
                        editText_y.setText(y + "");
                    }
                }
                dismissWindow();
            });
        }
    }


    private Dialog ClickDialogShow(ScriptCmdBean scriptCmdBean, CmdDialogCallback callback) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_click, null);
        final Dialog dialog = new AlertDialog.Builder(context, R.style.MyDialog).setView(view).setCancelable(false).create();
        EditText edit_X = view.findViewById(R.id.edit_X);
        EditText edit_Y = view.findViewById(R.id.edit_Y);
        EditText edit_duration = view.findViewById(R.id.edit_duration);
        EditText ed_delayed = view.findViewById(R.id.edit_delayed);
        Button btn_getXY = view.findViewById(R.id.btn_getXY);
        Button btn_cancel = view.findViewById(R.id.btn_cancel);
        Button btn_determine = view.findViewById(R.id.btn_determine);
        if (scriptCmdBean != null) {
            edit_X.setText(scriptCmdBean.getX0() + "");
            edit_Y.setText(scriptCmdBean.getY0() + "");
            edit_duration.setText(scriptCmdBean.getDuration() + "");
            ed_delayed.setText(scriptCmdBean.getDelayed() + "");
        }
        btn_cancel.setOnClickListener(v -> {
            dismissWindow();
            dialog.dismiss();
        });
        btn_determine.setOnClickListener(v -> {
            String sx = edit_X.getText().toString();
            String sy = edit_Y.getText().toString();
            String duration = edit_duration.getText().toString();
            String delayed = ed_delayed.getText().toString();
            int x = Integer.parseInt(TextUtils.isEmpty(sx) ? "0" : sx);
            int y = Integer.parseInt(TextUtils.isEmpty(sy) ? "0" : sy);
            int d = Integer.parseInt(TextUtils.isEmpty(duration) ? "0" : duration);
            int delay = Integer.parseInt(TextUtils.isEmpty(delayed) ? "0" : delayed);
            callback.onCall(getClickCmd(x, y, d, delay));
            dialog.dismiss();
            dismissWindow();
        });
        btn_getXY.setOnClickListener(v -> {
            editText_x = edit_X;
            editText_y = edit_Y;
            alertWindow();
        });

        dialog.show();
        //需要先显示再设置大小
        Window window = dialog.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();//获取屏幕分辨率
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;
        params.width = (int) (0.7 * screenWidth);
        window.setAttributes(params);
        return dialog;
    }


    private ScriptCmdBean getClickCmd(int x, int y, int duration, int delayed) {
        return ScriptCmdBean.BuildClickCMD(x, y, duration, delayed);
    }

    private Dialog GestureDialogShow( ScriptCmdBean scriptCmdBean, CmdDialogCallback callback) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_gesture, null);
        final Dialog dialog = new AlertDialog.Builder(context, R.style.MyDialog).setView(view).setCancelable(false).create();
        EditText edit_X = view.findViewById(R.id.edit_X);
        EditText edit_Y = view.findViewById(R.id.edit_Y);
        EditText edit_duration = view.findViewById(R.id.edit_duration);
        EditText edit_X2 = view.findViewById(R.id.edit_X2);
        EditText edit_Y2 = view.findViewById(R.id.edit_Y2);
        EditText ed_delayed = view.findViewById(R.id.edit_delayed);
        Button btn_getXY = view.findViewById(R.id.btn_getXY);
        Button btn_getXY2 = view.findViewById(R.id.btn_getXY2);
        Button btn_cancel = view.findViewById(R.id.btn_cancel);
        Button btn_determine = view.findViewById(R.id.btn_determine);
        if (scriptCmdBean != null) {
            edit_X.setText(scriptCmdBean.getX0() + "");
            edit_Y.setText(scriptCmdBean.getY0() + "");
            edit_X2.setText(scriptCmdBean.getX1() + "");
            edit_Y2.setText(scriptCmdBean.getY1() + "");
            edit_duration.setText(scriptCmdBean.getDuration() + "");
            ed_delayed.setText(scriptCmdBean.getDelayed() + "");
        }
        btn_cancel.setOnClickListener(v -> {
            dismissWindow();
            dialog.dismiss();
        });
        btn_determine.setOnClickListener(v -> {
            String sx = edit_X.getText().toString();
            String sy = edit_Y.getText().toString();
            String sx2 = edit_X2.getText().toString();
            String sy2 = edit_Y2.getText().toString();
            String duration = edit_duration.getText().toString();
            String delayed = ed_delayed.getText().toString();
            int x = Integer.parseInt(TextUtils.isEmpty(sx) ? "0" : sx);
            int y = Integer.parseInt(TextUtils.isEmpty(sy) ? "0" : sy);
            int x2 = Integer.parseInt(TextUtils.isEmpty(sx2) ? "0" : sx2);
            int y2 = Integer.parseInt(TextUtils.isEmpty(sy2) ? "0" : sy2);
            int d = Integer.parseInt(TextUtils.isEmpty(duration) ? "0" : duration);
            int delay = Integer.parseInt(TextUtils.isEmpty(delayed) ? "0" : delayed);
            callback.onCall(getGesture(x, y, x2, y2, d, delay));
            dialog.dismiss();
            dismissWindow();
        });
        btn_getXY.setOnClickListener(v -> {
            editText_x = edit_X;
            editText_y = edit_Y;
            alertWindow();
        });
        btn_getXY2.setOnClickListener(v -> {
            editText_x = edit_X2;
            editText_y = edit_Y2;
            alertWindow();
        });

        dialog.show();
        //需要先显示再设置大小
        Window window = dialog.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();//获取屏幕分辨率
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;
        params.width = (int) (0.8 * screenWidth);
        window.setAttributes(params);
        return dialog;
    }

    private ScriptCmdBean getGesture(int x0, int y0, int x1, int y1, int duration, int delay) {
        return ScriptCmdBean.BuildGestureCMD(x0, y0, x1, y1, duration, delay);

    }


    public void showSelectCmdDialog(ScriptCmdBean scriptCmdBean, CmdDialogCallback callback){

        final String[] items = new String[]{"点击命令", "延时命令", "滑屏命令"};
        DialogHelper.showMenuDialog(items, context, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    ClickDialogShow(scriptCmdBean, callback);
                }
                if (which == 1) {
                    DelayedDialogShow(scriptCmdBean, callback);
                }
                if (which == 2) {
                    GestureDialogShow(scriptCmdBean, callback);
                }
                dialog.dismiss();
            }
        });
    }


    private Dialog DelayedDialogShow(ScriptCmdBean scriptCmdBean, CmdDialogCallback callback) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_delayed, null);
        final Dialog dialog = new AlertDialog.Builder(context, R.style.MyDialog).setView(view).setCancelable(false).create();
        EditText ed_delayed = view.findViewById(R.id.edit_delayed);
        Button btn_cancel = view.findViewById(R.id.btn_cancel);
        Button btn_determine = view.findViewById(R.id.btn_determine);
        if (scriptCmdBean != null) {
            ed_delayed.setText(scriptCmdBean.getDelayed() + "");

        }
        btn_cancel.setOnClickListener(v -> {
            dialog.dismiss();
        });
        btn_determine.setOnClickListener(v -> {
            String s_delayed = ed_delayed.getText().toString();
            int d = Integer.parseInt(TextUtils.isEmpty(s_delayed) ? "0" : s_delayed);
            callback.onCall(getDelayedCmd(d));
            dialog.dismiss();

        });
        dialog.show();
        //需要先显示再设置大小
        Window window = dialog.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();//获取屏幕分辨率
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;
        params.width = (int) (0.7 * screenWidth);
        window.setAttributes(params);
        return dialog;
    }


    private ScriptCmdBean getDelayedCmd(int delayed) {
        return ScriptCmdBean.BuildDelayedCMD(delayed);

    }


    public interface CmdDialogCallback {
        void onCall(ScriptCmdBean scriptCmdBean);

    }

}
