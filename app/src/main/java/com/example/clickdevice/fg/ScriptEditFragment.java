package com.example.clickdevice.fg;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Message;
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

import com.Ohuang.ilivedata.MyLiveData;
import com.example.clickdevice.AC.ScriptEditActivity;
import com.example.clickdevice.MyApp;
import com.example.clickdevice.R;
import com.example.clickdevice.SmallWindowView;
import com.example.clickdevice.adapter.CMDAdapter;
import com.example.clickdevice.adapter.SimpleCallbackHelp;
import com.example.clickdevice.bean.ScriptCmdBean;
import com.example.clickdevice.db.AppDatabase;
import com.example.clickdevice.db.ScriptDataBean;
import com.example.clickdevice.dialog.DialogHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class ScriptEditFragment extends Fragment {
    private ScriptEditActivity scriptEditActivity;
    private View view;
    private Button btn_insert, btn_back, btn_complete;
    private EditText editText_name;
    private RecyclerView recyclerView;

    private WindowManager.LayoutParams btn_layoutParams;
    private LinearLayout btn_windowView;
    private TextView tv_btnWv;
    private boolean isShow = false;
    private WindowManager.LayoutParams mLayoutParams;
    public SmallWindowView windowView;
    private WindowManager wm;
    private EditText editText_x, editText_y;
    private CMDAdapter cmdAdapter;
    private List<ScriptCmdBean> mData;
    private ScriptDataBean scriptDataBean;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        scriptEditActivity = (ScriptEditActivity) context;
    }

    public ScriptEditFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_script_edit, container, false);
        initView();
        initSmallViewLayout();
        initbtnWindows();
        initData();
        initEvent();
        return view;
    }

    private void initEvent() {
        MyLiveData.getInstance().with("ScriptDataBean", ScriptDataBean.class).observe(this, new Observer<ScriptDataBean>() {
            @Override
            public void onChanged(ScriptDataBean s) {
                if (scriptEditActivity.getIsNew()) {
                    return;
                }
                scriptDataBean = s;
                editText_name.setText(scriptDataBean.getName());
                Gson gson = new Gson();
                List<ScriptCmdBean> list = gson.fromJson(scriptDataBean.getScriptJson(), new TypeToken<List<ScriptCmdBean>>() {
                }.getType());
                if (list != null) {
                    mData = list;
                    cmdAdapter.setmData(mData);
                    ItemTouchHelper.SimpleCallback simpleCallback = SimpleCallbackHelp.get(mData, cmdAdapter);
                    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
                    itemTouchHelper.attachToRecyclerView(recyclerView);
                }


            }
        });
    }

    private void initView() {
        btn_insert = view.findViewById(R.id.btn_insert_cmd);
        btn_back = view.findViewById(R.id.btn_back);
        btn_complete = view.findViewById(R.id.btn_complete);
        editText_name = view.findViewById(R.id.edit_name);
        recyclerView = view.findViewById(R.id.rv_script_edit);
        btn_insert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] items = new String[]{"点击命令", "延时命令", "滑屏命令"};
                DialogHelper.showMenuDialog(items, getActivity(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            ClickDialogShow(getContext());
                        }
                        if (which == 1) {
                            DelayedDialogShow(getContext());
                        }
                        if (which == 2) {
                            GestureDialogShow(getContext());
                        }

                        dialog.dismiss();
                    }
                });
            }
        });

        btn_back.setOnClickListener(v -> {
            scriptEditActivity.finish();
        });

        btn_complete.setOnClickListener(v -> {
            String name = editText_name.getText().toString();
            String format = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            String time = sdf.format(new Date(System.currentTimeMillis()));
            Gson gson = new Gson();
            String scriptJson = gson.toJson(mData);
            if (scriptEditActivity.getIsNew()) {

                scriptDataBean = new ScriptDataBean(name, time, time, scriptJson);
            } else {
                if (scriptDataBean == null) {
                    scriptDataBean = new ScriptDataBean();
                }
                scriptDataBean.setUpdateTime(time);
                scriptDataBean.setName(name);
                scriptDataBean.setScriptJson(scriptJson);
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    MyApp myApp = (MyApp) getActivity().getApplication();
                    myApp.getAppDatabase().getScriptDao().insertScriptDataBean(scriptDataBean);
                    handler.sendEmptyMessage(0);
                }
            }).start();
        });
    }

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            getActivity().finish();
        }
    };

    private void initData() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        if (mData == null) {
            mData = new ArrayList<>();
        }
        cmdAdapter = new CMDAdapter(mData, getContext());
        cmdAdapter.setCmdListener(new CMDAdapter.CmdListener() {
            @Override
            public void insert(int position) {
                final String[] items = new String[]{"点击命令", "延时命令", "滑屏命令"};
                DialogHelper.showMenuDialog(items, getActivity(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            ClickDialogShow(getContext(),position,null);
                        }
                        if (which == 1) {
                            DelayedDialogShow(getContext(),position,null);
                        }
                        if (which == 2) {
                            GestureDialogShow(getContext(),position,null);
                        }

                        dialog.dismiss();
                    }
                });
            }

            @Override
            public void edit(int position, ScriptCmdBean scriptCmdBean) {
                if (scriptCmdBean.getAction() == ScriptCmdBean.ACTION_CLICK) {
                    ClickDialogShow(getContext(), position, scriptCmdBean);
                } else if (scriptCmdBean.getAction() == ScriptCmdBean.ACTION_DELAYED) {
                    DelayedDialogShow(getContext(), position, scriptCmdBean);
                } else if (scriptCmdBean.getAction() == ScriptCmdBean.ACTION_GESTURE) {
                    GestureDialogShow(getContext(), position, scriptCmdBean);
                }
            }
        });
        recyclerView.setAdapter(cmdAdapter);
        if (scriptEditActivity.getIsNew()) {
            ItemTouchHelper.SimpleCallback simpleCallback = SimpleCallbackHelp.get(mData, cmdAdapter);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
            itemTouchHelper.attachToRecyclerView(recyclerView);
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


    private void addDelayedCmd(int delayed) {
        ScriptCmdBean scriptCmdBean = ScriptCmdBean.BuildDelayedCMD(delayed);
        if (mData != null) {
            mData.add(scriptCmdBean);
            cmdAdapter.notifyDataSetChanged();
        }
    }

    private void setDelayedCmd(int delayed, int position) {
        ScriptCmdBean scriptCmdBean = ScriptCmdBean.BuildDelayedCMD(delayed);
        if (mData != null) {
            mData.set(position, scriptCmdBean);
            cmdAdapter.notifyDataSetChanged();
        }
    }

    private void addDelayedCmd(int delayed, int position) {
        ScriptCmdBean scriptCmdBean = ScriptCmdBean.BuildDelayedCMD(delayed);
        if (mData != null) {
            mData.add(position, scriptCmdBean);
            cmdAdapter.notifyDataSetChanged();
        }
    }

    private void addClickCmd(int x, int y, int duration) {
        ScriptCmdBean scriptCmdBean = ScriptCmdBean.BuildClickCMD(x, y, duration);
        if (mData != null) {
            mData.add(scriptCmdBean);
            cmdAdapter.notifyDataSetChanged();
        }
    }

    private void addClickCmd(int x, int y, int duration, int position) {
        ScriptCmdBean scriptCmdBean = ScriptCmdBean.BuildClickCMD(x, y, duration);
        if (mData != null) {
            mData.add(position, scriptCmdBean);
            cmdAdapter.notifyDataSetChanged();
        }
    }

    private void setClickCmd(int x, int y, int duration, int position) {
        ScriptCmdBean scriptCmdBean = ScriptCmdBean.BuildClickCMD(x, y, duration);
        if (mData != null) {
            mData.set(position, scriptCmdBean);
            cmdAdapter.notifyDataSetChanged();
        }
    }

    private void addGesture(int x0, int y0, int x1, int y1, int duration) {
        ScriptCmdBean scriptCmdBean = ScriptCmdBean.BuildGestureCMD(x0, y0, x1, y1, duration);
        if (mData != null) {
            mData.add(scriptCmdBean);
            cmdAdapter.notifyDataSetChanged();
        }
    }

    private void addGesture(int x0, int y0, int x1, int y1, int duration, int position) {
        ScriptCmdBean scriptCmdBean = ScriptCmdBean.BuildGestureCMD(x0, y0, x1, y1, duration);
        if (mData != null) {
            mData.add(position, scriptCmdBean);
            cmdAdapter.notifyDataSetChanged();
        }
    }

    private void setGesture(int x0, int y0, int x1, int y1, int duration, int position) {
        ScriptCmdBean scriptCmdBean = ScriptCmdBean.BuildGestureCMD(x0, y0, x1, y1, duration);
        if (mData != null) {
            mData.set(position, scriptCmdBean);
            cmdAdapter.notifyDataSetChanged();
        }
    }

    private Dialog DelayedDialogShow(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_delayed, null);
        final Dialog dialog = new AlertDialog.Builder(context, R.style.MyDialog).setView(view).setCancelable(false).create();
        EditText ed_delayed = view.findViewById(R.id.edit_delayed);
        Button btn_cancel = view.findViewById(R.id.btn_cancel);
        Button btn_determine = view.findViewById(R.id.btn_determine);
        btn_cancel.setOnClickListener(v -> {
            dialog.dismiss();
        });
        btn_determine.setOnClickListener(v -> {
            String s_delayed = ed_delayed.getText().toString();
            int d = Integer.parseInt(TextUtils.isEmpty(s_delayed) ? "0" : s_delayed);
            addDelayedCmd(d);
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

    private Dialog DelayedDialogShow(Context context, int position, ScriptCmdBean scriptCmdBean) {
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
            if (scriptCmdBean != null) {
                setDelayedCmd(d, position);
            } else {
                addDelayedCmd(d, position);
            }
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

    private Dialog ClickDialogShow(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_click, null);
        final Dialog dialog = new AlertDialog.Builder(context, R.style.MyDialog).setView(view).setCancelable(false).create();
        EditText edit_X = view.findViewById(R.id.edit_X);
        EditText edit_Y = view.findViewById(R.id.edit_Y);
        EditText edit_duration = view.findViewById(R.id.edit_duration);
        Button btn_getXY = view.findViewById(R.id.btn_getXY);
        Button btn_cancel = view.findViewById(R.id.btn_cancel);
        Button btn_determine = view.findViewById(R.id.btn_determine);
        btn_cancel.setOnClickListener(v -> {
            dismissWindow();
            dialog.dismiss();
        });
        btn_determine.setOnClickListener(v -> {
            String sx = edit_X.getText().toString();
            String sy = edit_Y.getText().toString();
            String duration = edit_duration.getText().toString();
            int x = Integer.parseInt(TextUtils.isEmpty(sx) ? "0" : sx);
            int y = Integer.parseInt(TextUtils.isEmpty(sy) ? "0" : sy);
            int d = Integer.parseInt(TextUtils.isEmpty(duration) ? "0" : duration);
            addClickCmd(x, y, d);
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

    private Dialog ClickDialogShow(Context context, int position, ScriptCmdBean scriptCmdBean) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_click, null);
        final Dialog dialog = new AlertDialog.Builder(context, R.style.MyDialog).setView(view).setCancelable(false).create();
        EditText edit_X = view.findViewById(R.id.edit_X);
        EditText edit_Y = view.findViewById(R.id.edit_Y);
        EditText edit_duration = view.findViewById(R.id.edit_duration);
        Button btn_getXY = view.findViewById(R.id.btn_getXY);
        Button btn_cancel = view.findViewById(R.id.btn_cancel);
        Button btn_determine = view.findViewById(R.id.btn_determine);
        if (scriptCmdBean != null) {
            edit_X.setText(scriptCmdBean.getX0() + "");
            edit_Y.setText(scriptCmdBean.getY0() + "");
            edit_duration.setText(scriptCmdBean.getDuration() + "");
        }
        btn_cancel.setOnClickListener(v -> {
            dismissWindow();
            dialog.dismiss();
        });
        btn_determine.setOnClickListener(v -> {
            String sx = edit_X.getText().toString();
            String sy = edit_Y.getText().toString();
            String duration = edit_duration.getText().toString();
            int x = Integer.parseInt(TextUtils.isEmpty(sx) ? "0" : sx);
            int y = Integer.parseInt(TextUtils.isEmpty(sy) ? "0" : sy);
            int d = Integer.parseInt(TextUtils.isEmpty(duration) ? "0" : duration);
            if (scriptCmdBean != null) {
                setClickCmd(x, y, d, position);
            } else {
                addClickCmd(x, y, d, position);
            }
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

    private Dialog GestureDialogShow(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_gesture, null);
        final Dialog dialog = new AlertDialog.Builder(context, R.style.MyDialog).setView(view).setCancelable(false).create();
        EditText edit_X = view.findViewById(R.id.edit_X);
        EditText edit_Y = view.findViewById(R.id.edit_Y);
        EditText edit_duration = view.findViewById(R.id.edit_duration);
        EditText edit_X2 = view.findViewById(R.id.edit_X2);
        EditText edit_Y2 = view.findViewById(R.id.edit_Y2);
        Button btn_getXY = view.findViewById(R.id.btn_getXY);
        Button btn_getXY2 = view.findViewById(R.id.btn_getXY2);
        Button btn_cancel = view.findViewById(R.id.btn_cancel);
        Button btn_determine = view.findViewById(R.id.btn_determine);
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
            int x = Integer.parseInt(TextUtils.isEmpty(sx) ? "0" : sx);
            int y = Integer.parseInt(TextUtils.isEmpty(sy) ? "0" : sy);
            int x2 = Integer.parseInt(TextUtils.isEmpty(sx2) ? "0" : sx2);
            int y2 = Integer.parseInt(TextUtils.isEmpty(sy2) ? "0" : sy2);
            int d = Integer.parseInt(TextUtils.isEmpty(duration) ? "0" : duration);

            addGesture(x, y, x2, y2, d);
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

    private Dialog GestureDialogShow(Context context, int position, ScriptCmdBean scriptCmdBean) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_gesture, null);
        final Dialog dialog = new AlertDialog.Builder(context, R.style.MyDialog).setView(view).setCancelable(false).create();
        EditText edit_X = view.findViewById(R.id.edit_X);
        EditText edit_Y = view.findViewById(R.id.edit_Y);
        EditText edit_duration = view.findViewById(R.id.edit_duration);
        EditText edit_X2 = view.findViewById(R.id.edit_X2);
        EditText edit_Y2 = view.findViewById(R.id.edit_Y2);
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
            int x = Integer.parseInt(TextUtils.isEmpty(sx) ? "0" : sx);
            int y = Integer.parseInt(TextUtils.isEmpty(sy) ? "0" : sy);
            int x2 = Integer.parseInt(TextUtils.isEmpty(sx2) ? "0" : sx2);
            int y2 = Integer.parseInt(TextUtils.isEmpty(sy2) ? "0" : sy2);
            int d = Integer.parseInt(TextUtils.isEmpty(duration) ? "0" : duration);
            if (scriptCmdBean != null) {
                setGesture(x, y, x2, y2, d, position);
            } else {
                addGesture(x, y, x2, y2, d, position);
            }
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

    @SuppressLint("WrongConstant")
    public void initSmallViewLayout() {
        this.windowView = (SmallWindowView) LayoutInflater.from(getContext()).inflate(R.layout.window_a, (ViewGroup) null);
        this.btn_windowView = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.window_b, (ViewGroup) null);
        this.wm = (WindowManager) getActivity().getSystemService("window");
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
            ActivityCompat.requestPermissions(getActivity(), new String[]{"android.permission.SYSTEM_ALERT_WINDOW"}, 1);
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
        if (!Settings.canDrawOverlays(getContext())) {
            Toast.makeText(getContext(), "can not DrawOverlays", 0).show();
            startActivityForResult(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + getActivity().getPackageName())), 2);
            return;
        }
        showWindow();

    }

    @Override
    public void onDestroy() {
        dismissWindow();
        super.onDestroy();
    }
}