package com.example.clickdevice.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.clickdevice.MyService
import com.example.clickdevice.PowerKeyObserver
import com.example.clickdevice.R
import com.example.clickdevice.SmallWindowView
import com.example.clickdevice.Util
import com.example.clickdevice.helper.KeyFloatWindowManager
import com.example.clickdevice.helper.onClick
import com.example.clickdevice.helper.setOnTouchClick
import com.example.clickdevice.helper.smallWindowManager
import com.example.clickdevice.ui.theme.ClickDeviceTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivityCompose : ComponentActivity() {

    private var isRun = false
    private var stopTime = 0L
    private val singleThreadExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var powerKeyObserver: PowerKeyObserver? = null
    private var windowView: SmallWindowView? = null
    private var btnWindowView: SmallWindowView? = null
    private var tvWinB: TextView? = null
    private var wm: WindowManager? = null
    private var mLayoutParams: WindowManager.LayoutParams? = null
    private var btnLayoutParams: WindowManager.LayoutParams? = null
    private var isShow = false

    // 主线程 Handler，用于更新 UI
    private val mainHandler = Handler(Looper.getMainLooper())

    // Compose 状态：让悬浮窗按钮文字可以实时更新
    var isRunning by mutableStateOf(false)
        private set

    var clickCount by mutableStateOf("0")
        private set

    var clickInterval by mutableStateOf("1000")
        private set

    var showAccessibilityDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClickDeviceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                isFloatingWindowShow = isShow,
                clickCount = clickCount,
                clickInterval = clickInterval,
                showAccessibilityDialog = showAccessibilityDialog,
                onDismissAccessibilityDialog = { showAccessibilityDialog = false },
                onOpenAccessibility = {
                    showAccessibilityDialog = false
                    openAccessibility()
                },
                onStartClickDevice = { startClickDevice() },
                onOpenScriptList = { startScriptList() },
                onOpenRecordScript = { startRecordScript() },
                onOpenScriptGroup = { startScriptGroup() },
                onOpenKeyBinding = { startKeyBinding() },
                onCountChange = { clickCount = it },
                onIntervalChange = { clickInterval = it }
            )
                }
            }
        }

        powerKeyObserver = PowerKeyObserver(this).apply {
            startListen()
            setHomeKeyListener {
                isRun = false
                isRunning = false
                resetBtnText()
            }
        }
    }

    private fun openAccessibility() {
        try {
            startActivity(Intent("android.settings.ACCESSIBILITY_SETTINGS"))
        } catch (e: Exception) {
            startActivity(Intent("android.settings.SETTINGS"))
        }
    }

    private fun startClickDevice() {
        if (!isShow) {
            showFloatWindows()
        } else {
            if (isRun) {
                onBtnWinBClick()
            }
            hideFloatWindows()
        }
    }

    // region 悬浮窗初始化

    @SuppressLint("WrongConstant")
    private fun initSmallViewLayout() {
        wm = smallWindowManager()

        // 获取屏幕尺寸
        val dm = resources.displayMetrics
        val screenWidth = dm.widthPixels
        val screenHeight = dm.heightPixels
        val density = dm.density
        val iconSizePx = (50 * density).toInt() // 50dp 转 px

        // 初始化位置选择悬浮窗 (window_a) - 初始位置居中
        windowView = LayoutInflater.from(this).inflate(R.layout.window_a, null) as SmallWindowView
        mLayoutParams = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, 8, PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mLayoutParams?.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        mLayoutParams?.gravity = Gravity.NO_GRAVITY
        windowView?.setWm(wm)
        windowView?.setWmParams(mLayoutParams)
        // setWmParams 会重置 x=0,y=0，之后再设置初始位置居中
        // 注意：不能在 addView 之前调用 updateViewLayout，否则会报 not attached to window manager
        mLayoutParams?.x = 0
        mLayoutParams?.y = 0

        // 初始化启动/停止按钮悬浮窗 (window_b) - SmallWindowView 自带拖动，初始位置屏幕顶部居中
        btnWindowView =
            LayoutInflater.from(this).inflate(R.layout.window_b, null) as SmallWindowView
        btnLayoutParams = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, 8, PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnLayoutParams?.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        btnWindowView?.enableMove=false
        btnWindowView?.setWm(wm)
        btnWindowView?.setWmParams(btnLayoutParams)
        btnLayoutParams?.x = 0
        btnLayoutParams?.y = -(btnWindowView?.screenHeight ?: 0) / 2

        // 设置启动/停止按钮点击事件
        tvWinB = btnWindowView?.findViewById(R.id.tv_win_b)
        tvWinB?.setOnTouchClick({ onBtnWinBClick() }, {
            if (isRun) {
                onBtnWinBClick()
                return@setOnTouchClick false
            }
            return@setOnTouchClick true
        })
    }

    @SuppressLint("WrongConstant")
    private fun alertWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
            } else {
                showWindow()
            }
        } else {
            showWindow()
        }
    }

    private fun showWindow() {

        if (windowView?.windowId == null) {
            wm?.addView(windowView, mLayoutParams)
        }
        if (btnWindowView?.windowId == null) {
            wm?.addView(btnWindowView, btnLayoutParams)
        }
        resetBtnText()
        windowView?.setwmParamsFlags(8)
    }

    private fun dismissWindow() {
        if (windowView?.windowId != null) {
            try {
                wm?.removeView(windowView)
            } catch (_: Exception) {
            }
        }
        if (btnWindowView?.windowId != null) {
            try {
                wm?.removeView(btnWindowView)
            } catch (_: Exception) {
            }
        }
    }

    private fun showFloatWindows() {
        if (!MyService.isStart()) {
            Toast.makeText(this, "请先开启辅助功能", Toast.LENGTH_LONG).show()
            return
        }
        if (windowView == null) {
            initSmallViewLayout()
        }
        alertWindow()
        isShow = true
    }

    private fun hideFloatWindows() {
        isShow = false
        isRun = false
        isRunning = false
        dismissWindow()
    }

    // endregion

    // region 悬浮窗按钮点击逻辑

    @SuppressLint("WrongConstant")
    private fun onBtnWinBClick() {
        if (!isRun) {
            if (stopTime + 2000 > System.currentTimeMillis()) {
                Toast.makeText(this, "点太快了,休息一下吧", Toast.LENGTH_SHORT).show()
                return
            }
            isRun = true
            isRunning = true

            val x = windowView?.actionUpX ?: 0
            val y = windowView?.actionUpY ?: 0
            val dm = resources.displayMetrics
            val maxSide = maxOf(dm.heightPixels, dm.widthPixels)
            if (x < 0 || x > maxSide || y < 0 || y > maxSide) {
                isRun = false
                isRunning = false
                Toast.makeText(this, "请先拖动选择点击位置", Toast.LENGTH_SHORT).show()
                return
            }
            if (!MyService.isStart()) {
                isRun = false
                isRunning = false
                Toast.makeText(
                    this,
                    "请手动开启辅助功能，若已开启请重启应用再试一次",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            tvWinB?.text = "停止"

            // 使位置选择悬浮窗不可触摸，防止误操作
            windowView?.setwmParamsFlags(24)

            val count = clickCount.toIntOrNull() ?: 0
            val interval = (clickInterval.toIntOrNull() ?: 1000).coerceAtLeast(10)

            singleThreadExecutor.execute {
                try {
                    Thread.sleep(50)
                } catch (_: InterruptedException) {
                }
                if (count > 0) {
                    for (i in 0 until count) {
                        if (!isRun) break
                        mainHandler.post {
                            MyService.myService?.dispatchGestureClick(x.toFloat(), y.toFloat())
                        }
                        var elapsed = 0
                        while (elapsed < interval && isRun) {
                            try {
                                Thread.sleep(10)
                            } catch (_: InterruptedException) {
                                break
                            }
                            elapsed += 10
                        }
                    }
                } else {
                    while (isRun) {
                        mainHandler.post {
                            MyService.myService?.dispatchGestureClick(x.toFloat(), y.toFloat())
                        }
                        var elapsed = 0
                        while (elapsed < interval && isRun) {
                            try {
                                Thread.sleep(10)
                            } catch (_: InterruptedException) {
                                break
                            }
                            elapsed += 10
                        }
                    }
                }
                mainHandler.post {
                    isRunning = false
                    resetBtnText()
                    // 恢复位置选择悬浮窗触摸
                    if (isShow) {
                        windowView?.setwmParamsFlags(8)
                    }
                }
            }
        } else {
            stopTime = System.currentTimeMillis()
            isRun = false
            isRunning = false
            resetBtnText()
        }
    }

    private fun resetBtnText() {
        tvWinB?.text = "开始"
    }

    // endregion

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "悬浮窗权限被拒绝", Toast.LENGTH_SHORT).show()
            } else {
                showWindow()
            }
        }
    }

    private fun startScriptList() {
        hideFloatWindows()
        startActivity(Intent(this, ScriptListActivityCompose::class.java))
    }

    private fun startRecordScript() {
        hideFloatWindows()
        startActivity(Intent(this, RecordScriptListActivityCompose::class.java))
    }

    private fun startScriptGroup() {
        hideFloatWindows()
        startActivity(Intent(this, ScriptGroupListActivityCompose::class.java))
    }

    private fun startKeyBinding() {
        hideFloatWindows()
        startActivity(Intent(this, KeyBindingListActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        isRun = false
        hideFloatWindows()
        powerKeyObserver?.stopListen()
        singleThreadExecutor.shutdownNow()
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQ_CODE = 2
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isFloatingWindowShow: Boolean,
    clickCount: String,
    clickInterval: String,
    showAccessibilityDialog: Boolean = false,
    onDismissAccessibilityDialog: () -> Unit = {},
    onOpenAccessibility: () -> Unit,
    onStartClickDevice: () -> Unit,
    onOpenScriptList: () -> Unit,
    onOpenRecordScript: () -> Unit,
    onOpenScriptGroup: () -> Unit,
    onOpenKeyBinding: () -> Unit,
    onCountChange: (String) -> Unit,
    onIntervalChange: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ClickDevice") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onOpenAccessibility,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("开启(无障碍)辅助功能")
            }

            val context = LocalContext.current

            Text(
                text = "通过adb命令授予权限后可自动开启无障碍模式：\nadb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        Util.copyText(
                            "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS",
                            context
                        )
                        Toast.makeText(context, "已复制命令", Toast.LENGTH_SHORT).show()
                    }
            )

            Divider()

            Text("连点器", style = MaterialTheme.typography.titleMedium)

            Text(
                text = "点击下方按钮打开悬浮窗，拖动位置图标选择点击位置，再点击\"开始\"按钮执行连点",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = clickCount,
                onValueChange = { onCountChange(it.filter { c -> c.isDigit() }) },
                label = { Text("点击次数") },
                placeholder = { Text("0为无限次") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = clickInterval,
                onValueChange = { onIntervalChange(it.filter { c -> c.isDigit() }) },
                label = { Text("时间间隔(ms)") },
                placeholder = { Text("最小10ms") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onStartClickDevice,
                modifier = Modifier.fillMaxWidth(),
                colors = if (isFloatingWindowShow) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ) else ButtonDefaults.buttonColors()
            ) {
                Text(if (isFloatingWindowShow) "关闭悬浮窗" else "打开连点器")
            }

            Divider()

            Button(
                onClick = onOpenScriptList,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("普通脚本")
            }

            Button(
                onClick = onOpenRecordScript,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("录制脚本")
            }

            Button(
                onClick = onOpenScriptGroup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("自定义脚本")
            }

            Divider()

            Button(
                onClick = onOpenKeyBinding,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("按键设置")
            }
        }
    }

    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = onDismissAccessibilityDialog,
            title = { Text("辅助功能") },
            text = { Text("使用连点器需要开启(无障碍)辅助功能，是否现在去开启？") },
            confirmButton = {
                TextButton(onClick = onOpenAccessibility) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissAccessibilityDialog) {
                    Text("取消")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ClickDeviceTheme {
        MainScreen(
            isFloatingWindowShow = false,
            clickCount = "",
            clickInterval = "1000",
            onOpenAccessibility = {},
            onStartClickDevice = {},
            onOpenScriptList = {},
            onOpenRecordScript = {},
            onOpenScriptGroup = {},
            onOpenKeyBinding = {},
            onCountChange = {},
            onIntervalChange = {}
        )
    }
}
