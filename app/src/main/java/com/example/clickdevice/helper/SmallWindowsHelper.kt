package com.example.clickdevice.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.clickdevice.MyService
import com.example.clickdevice.helper.KeyFloatWindowManager.FloatWindowInfo
import java.lang.Exception
/**
 *  用于显示小窗的context
 */
fun Context.smallWindowsContext(): Context{
    return  (MyService.myService?:this.applicationContext)
}

/**
 *  用于显示小窗的windowManager
 */
fun Context.smallWindowManager(): WindowManager{
    return  smallWindowsContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
}
class SmallWindowsHelper(val context: Context) {
    var mWindowManager: WindowManager =
        context.smallWindowManager()
        private set

    var mLayoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        gravity = Gravity.NO_GRAVITY
    }
        set(value) {
            field = value
            updateLayoutParams {}
        }

    fun updateLayoutParams(call:WindowManager.LayoutParams.()->Unit) {
        if (isShow && root != null) {
            try {
                call(mLayoutParams)
                mWindowManager.updateViewLayout(root, mLayoutParams)
            } catch (e: Throwable) {
            }
        }
    }


    var root: View? = null

    var isShow = false



    fun attach(view: View) {
        root = view
        show()
    }


    fun show() {
        if (!isShow)
            mWindowManager.addView(root, mLayoutParams)
        isShow = true
    }

    fun hide() {
        try {
            mWindowManager.removeView(root)
        } catch (e: Exception) {
        }

        isShow = false
    }

    /**
     *  悬浮窗是否可交互
     */
    fun setTouchEnable(enable: Boolean) {
        updateLayoutParams{
            flags = if (enable) {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            } else {
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
        }
    }


    companion object {
        private const val OVERLAY_PERMISSION_REQ_CODE = 2
        fun requestPermission(activity: Activity): Boolean {

            return if (!Settings.canDrawOverlays(activity)) {
                Toast.makeText(activity, "can not DrawOverlays", Toast.LENGTH_LONG).show()
                activity.startActivityForResult(
                    Intent(
                        "android.settings.action.MANAGE_OVERLAY_PERMISSION",
                        Uri.parse("package:" + activity.getPackageName())
                    ), OVERLAY_PERMISSION_REQ_CODE
                )
                false
            } else {
                true
            }
            return false
        }

        fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode != OVERLAY_PERMISSION_REQ_CODE) {
                return
            }
            if (!Settings.canDrawOverlays(activity)) {
                Toast.makeText(activity, "设置权限拒绝", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(activity, "设置权限成功", Toast.LENGTH_LONG).show()
            }
        }


    }


}