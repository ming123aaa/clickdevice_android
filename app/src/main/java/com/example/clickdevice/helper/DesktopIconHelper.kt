package com.example.clickdevice.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Process
import android.text.TextUtils
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.clickdevice.IconAddCallbackReceiver
import com.example.clickdevice.R
import com.example.clickdevice.activity.LauncherScriptActivity
import com.example.clickdevice.db.RecordScriptBean
import com.example.clickdevice.db.ScriptDataBean


object DesktopIconHelper {


    fun addShortcut(context: Activity,data: RecordScriptBean){
        var addShortcut = addShortcut(
            context,
            "${data.id + 20000}",
            data.name+"脚本",
            LauncherScriptActivity.TYPE_RECORD_SCRIPT,
            data.id
        )
        if (!TextUtils.isEmpty(addShortcut)){
            Toast.makeText(context,"添加完成",Toast.LENGTH_LONG).show()
        }
    }

    fun addShortcut(context: Activity,data: ScriptDataBean){
        var addShortcut = addShortcut(
            context,
            "${data.id + 10000}",
            data.name+"脚本",
            LauncherScriptActivity.TYPE_SCRIPT,
            data.id
        )
        if (!TextUtils.isEmpty(addShortcut)){
            Toast.makeText(context,"添加完成",Toast.LENGTH_LONG).show()
        }
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    fun addShortcut(context: Activity, id: String, iconName: String,type:String,scriptID:Int): String {


        if (context.checkPermission(
                "com.android.launcher.permission.INSTALL_SHORTCUT",
                Process.myPid(),
                Process.myUid()
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context,"需要获取创建桌面快捷方式权限",Toast.LENGTH_LONG).show()
            context.requestPermissions(
                arrayOf<String>("com.android.launcher.permission.INSTALL_SHORTCUT"),
                1234
            )
            return ""
        }

        var packageName = context.packageName
        val uuid = id

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                //构建点击intent
                val shortcutInfoIntent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    setClassName(packageName, LauncherScriptActivity::class.java.name)
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(LauncherScriptActivity.TYPE,type)
                    putExtra(LauncherScriptActivity.ID,scriptID)

                }
                //构建快捷方式信息
                val pinShortcutInfo = ShortcutInfoCompat.Builder(context, uuid)
                    .setShortLabel(iconName)
                    .setActivity(ComponentName(context, LauncherScriptActivity::class.java))
                    .setIcon(
                        IconCompat.createWithBitmap(
                            drawableToBitmap(
                                context.getDrawable(R.mipmap.icon_app)!!,
                                200,
                                200
                            )
                        )
                    )
                    .setIntent(shortcutInfoIntent)
                    .build()
                val successCallback = PendingIntent.getBroadcast(
                    context, 1000, Intent(context, IconAddCallbackReceiver::class.java),
                    PendingIntent.FLAG_MUTABLE
                )

                //添加
                ShortcutManagerCompat.requestPinShortcut(
                    context,
                    pinShortcutInfo, successCallback.intentSender
                )
            }
        } else {
            //构建点击intent
            val shortcutInfoIntent = Intent().apply {
                action = Intent.ACTION_VIEW
                setClassName(packageName, LauncherScriptActivity::class.java.name)
                addCategory(Intent.CATEGORY_LAUNCHER)
                putExtra(LauncherScriptActivity.TYPE,type)
                putExtra(LauncherScriptActivity.ID,scriptID)
                //设置点击快捷方式，进入指定的Activity
                //注意：因为是从Lanucher中启动，所以这里用到了ComponentName
                //其中new ComponentName这里的第二个参数，是Activity的全路径名，也就是包名类名要写全。
                component = ComponentName(context, LauncherScriptActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            //给Intent添加 对应的flag
            val resultIntent = Intent().apply {
                // Intent.ShortcutIconResource.fromContext 这个就是设置快捷方式的图标
                putExtra(
                    Intent.EXTRA_SHORTCUT_ICON,
                    drawableToBitmap(context.getDrawable(R.mipmap.icon_app)!!, 200, 200)
                )
                //启动的Intent
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutInfoIntent)
                //这里可以设置快捷方式的名称
                putExtra(Intent.EXTRA_SHORTCUT_NAME, iconName)
                //设置Action
                action = "com.android.launcher.action.INSTALL_SHORTCUT"
            }

            //发送广播、通知系统创建桌面快捷方式
            context.sendBroadcast(resultIntent)
        }
        return "1"
    }

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap? {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

}