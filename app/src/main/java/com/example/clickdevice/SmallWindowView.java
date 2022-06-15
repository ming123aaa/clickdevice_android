package com.example.clickdevice;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;


public class SmallWindowView extends LinearLayout {
    private static final String TAG = "SmallWindowView";
    private final int screenHeight;
    private final int screenWidth;
    private int statusHeight;//状态栏高度
    //MotionEvent.ACTION_DOWN的开始坐标
    private float mTouchStartX;
    private float mTouchStartY;
    //onTouchEvent实时坐标获取
    private float x;
    private float y;


    private int actionUpX = 0, actionUpY = 0;
    private WindowManager wm;
    public WindowManager.LayoutParams wmParams;


    public SmallWindowView(Context context) {
        this(context, null);
    }

    public WindowManager getWm() {
        return wm;
    }

    public void setWm(WindowManager wm) {
        this.wm = wm;
    }

    public WindowManager.LayoutParams getWmParams() {
        return wmParams;
    }

    public void setWmParams(WindowManager.LayoutParams wmParams) {
        this.wmParams = wmParams;
        this.wmParams.x = 0;
        this.wmParams.y = 0;

    }

    public SmallWindowView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmallWindowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        statusHeight = getStatusHeight(context);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenHeight = dm.heightPixels;
        screenWidth = dm.widthPixels;
    }

    public void setwmParamsFlags(int flags) {
        wmParams.flags = flags;
        wm.updateViewLayout(this, wmParams);
    }


    /**
     * 获得状态栏的高度
     *
     * @param context
     * @return
     */
    public static int getStatusHeight(Context context) {
        int statusHeight = -1;
        try {
            Class clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height")
                    .get(object).toString());
            statusHeight = context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusHeight;
    }

    private int[] location = new int[2]; // 小窗口位置坐标

    private boolean calcPointRange(MotionEvent event) {
        this.getLocationOnScreen(location);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        float curX = event.getRawX();
        float curY = event.getRawY();
        if (curX >= location[0] && curX <= location[0] + width && curY >= location[1] && curY <= location[1] + height) {
            return true;
        }
        return false;
    }


    boolean isRange = false;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            isRange = calcPointRange(event);
            Log.e(TAG, "onTouchEvent: isRange = " + isRange);

        }
        if (!isRange) {
            return super.onTouchEvent(event);
        }
        x = event.getRawX();
        y = event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
//                if (wmParams.x > 0) {
//                    isRight = true;
//                }
//                if (wmParams.x < 0) {
//                    isRight = false;
//                }

                lastWmParamsX = wmParams.x;
                lastWmParamsY = wmParams.y;
                mTouchStartX = event.getRawX();
                mTouchStartY = event.getRawY();
                Log.i("startP", "startX" + mTouchStartX + "====startY" + mTouchStartY);
                Log.i("startP", "lastWmParamsX" + lastWmParamsX + "====lastWmParamsY" + lastWmParamsY);
                break;

            case MotionEvent.ACTION_MOVE:
                updateViewPosition();
                break;
            case MotionEvent.ACTION_UP:
                if (isHorizontalScreen(wm)) {

                    if (wmParams.y <= 0) {
                        wmParams.y = Math.abs(wmParams.y) <= screenWidth / 2-getWidth()/2 ? wmParams.y : -screenWidth / 2+getWidth()/2;
                    } else {
                        wmParams.y = Math.min(wmParams.y, (screenWidth / 2) - getWidth() / 2);
                    }
                    if (wmParams.x <= 0) {
                        wmParams.x = Math.abs(wmParams.x) <= screenHeight / 2-getHeight()/2 ? wmParams.x : -screenHeight / 2+getHeight()/2;
                    } else {
                        wmParams.x = Math.min(wmParams.x, (screenHeight / 2)-getHeight()/2);
                    }
                } else {
                    if (wmParams.x <= 0) {
                        wmParams.x = Math.abs(wmParams.x) <= screenWidth / 2-getWidth()/2 ? wmParams.x : -screenWidth / 2+getWidth()/2;
                    } else {
                        wmParams.x = Math.min(wmParams.x, (screenWidth / 2)-getWidth()/2);
                    }
                    if (wmParams.y <= 0) {
                        wmParams.y = Math.abs(wmParams.y) <= screenHeight / 2-getHeight()/2 ? wmParams.y : -screenHeight / 2+getHeight()/2;
                    } else {
                        wmParams.y = Math.min(wmParams.y, (screenHeight / 2)-getHeight()/2);
                    }

                }

                actionUpX = (int) (event.getRawX()+getWidth()/2-event.getX());
                actionUpY = (int) (event.getRawY()+getHeight()/2-event.getY());
//                wmParams.y = (int) (y - screenHeight / 2);
                wm.updateViewLayout(this, wmParams);
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private int lastWmParamsX, lastWmParamsY;

    private void updateViewPosition() {
//        wmParams.gravity = Gravity.NO_GRAVITY;
        //更新浮动窗口位置参数
        int dx = (int) (x - mTouchStartX);
        int dy = (int) (y - mTouchStartY);
//        if (isRight) {
//            wmParams.x = screenWidth / 2 - dx;
//        } else {
//            wmParams.x = -dx - screenWidth / 2;
//        }
        wmParams.x = lastWmParamsX + dx;
        wmParams.y = lastWmParamsY + dy;
        Log.i("winParams", "lastWmParamsX:" + lastWmParamsX + "x : " + wmParams.x + "y :" + wmParams.y + "  dx:" + dx + "  dy :" + dy);
        wm.updateViewLayout(this, wmParams);
        //刷新显示
    }

    private boolean isHorizontalScreen(WindowManager windowManager) {
        int angle = windowManager.getDefaultDisplay().getRotation();
        if (angle == Surface.ROTATION_90 || angle == Surface.ROTATION_270) {
            //如果屏幕旋转90°或者270°是判断为横屏，横屏规避不展示
            return true;
        }
        return false;
    }


    public int getActionUpX() {
        return actionUpX;
    }



    public int getActionUpY() {
        return actionUpY;
    }


}
