package com.example.clickdevice.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.clickdevice.R;
import com.example.clickdevice.bean.ScriptCmdBean;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;

import java.util.List;

public class DialogHelper {
    private static int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;
    public static void showMenuDialog(final String[] items, Activity activity, final DialogInterface.OnClickListener callBack) {

        new QMUIDialog.MenuDialogBuilder(activity)
//                .addItems(items, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        Toast.makeText(activity, "你选择了 " + items[which], Toast.LENGTH_SHORT).show();
//                        dialog.dismiss();
//                    }
//                })
                .addItems(items,callBack)
                .show();

    }

    public static Dialog showMessagePositiveDialog(Activity activity,String title,String msg,QMUIDialogAction.ActionListener listener) {
       return new QMUIDialog.MessageDialogBuilder(activity)
                .setTitle(title)
                .setMessage(msg)
               .setCancelable(false)
                .addAction("取消", new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        dialog.dismiss();
                    }
                })
                .addAction(0, "确定", QMUIDialogAction.ACTION_PROP_POSITIVE,listener)
                .show();
    }
    public static void showMessagePositiveDialog(Activity activity,String title,String msg,QMUIDialogAction.ActionListener listener,View.OnClickListener quxiao) {
        new QMUIDialog.MessageDialogBuilder(activity)
                .setTitle(title)
                .setMessage(msg)
                .setCancelable(false)
                .addAction("取消", new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        dialog.dismiss();
                        if (quxiao!=null){
                            quxiao.onClick(null);
                        }
                    }
                })
                .addAction(0, "确定", QMUIDialogAction.ACTION_PROP_POSITIVE,listener)
                .show();
    }


    public static Dialog DeleteDialogShow(Context context, String title, String content, View.OnClickListener confirmListener){
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_delete, null);
        final Dialog dialog = new AlertDialog.Builder(context, R.style.MyDialog).setView(view).create();
        TextView text_title = view.findViewById(R.id.txt_title_dialog_delete);
        TextView text_content = view.findViewById(R.id.txt_content_dialog_delete);
        Button quxiao = view.findViewById(R.id.dialog_btn_delete_no);
        Button queding = view.findViewById(R.id.dialog_btn_delete);
        text_title.setText(title);
        text_content.setText(content);
        quxiao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        queding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmListener.onClick(v);
                dialog.dismiss();
            }
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


    public static Dialog showDialog(Context context,View view){
        final Dialog dialog = new AlertDialog.Builder(context, R.style.MyDialog).setView(view).create();
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

    public static Dialog EditDialogShow(Context context, String title,String textContent,DialogCallback<String> callback) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_json, null);
        final Dialog dialog = new AlertDialog.Builder(context, R.style.MyDialog).setView(view).setCancelable(false).create();
        EditText ed_delayed = view.findViewById(R.id.edit_delayed);
        Button btn_cancel = view.findViewById(R.id.btn_cancel);
        Button btn_determine = view.findViewById(R.id.btn_determine);
        TextView tv_title = view.findViewById(R.id.tv_title);
        tv_title.setText(title);
        ed_delayed.setText(textContent);

        btn_cancel.setOnClickListener(v -> {
            dialog.dismiss();
        });
        btn_determine.setOnClickListener(v -> {
            String json = ed_delayed.getText().toString();
            callback.onCall(json);
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


    public interface DialogCallback<T>{
        void onCall(T t);
    }

}
