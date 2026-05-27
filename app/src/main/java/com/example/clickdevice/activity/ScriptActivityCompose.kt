package com.example.clickdevice.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.Ohuang.ilivedata.MyLiveData
import com.example.clickdevice.MyService
import com.example.clickdevice.PowerKeyObserver
import com.example.clickdevice.R
import com.example.clickdevice.ScriptExecutor
import com.example.clickdevice.Util
import com.example.clickdevice.helper.setOnTouchClick
import com.example.clickdevice.bean.ScriptCmdBean
import com.example.clickdevice.ui.theme.ClickDeviceTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScriptActivityCompose : ComponentActivity(), ScriptExecutor.ScriptInterFace {

    private var isRun = false
    private var isShow = false
    private var stopTime = 0L
    private val singleThreadExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var scriptExecutor: ScriptExecutor? = null
    private var myService: MyService? = null
    private var delayCoefficient = 1.0
    private var powerKeyObserver: PowerKeyObserver? = null
    private var observer: Observer<String>? = null
    private var thisPkgName = ""
    private var pkgNameNow = ""

    private var btnWindowView: LinearLayout? = null
    private var tvBtnWv: TextView? = null
    private var wm: WindowManager? = null
    private var btnLayoutParams: WindowManager.LayoutParams? = null



    var isRunning by mutableStateOf(false)
        private set

    var scriptConfig by mutableStateOf(ScriptRunConfig())
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scriptExecutor = ScriptExecutor(this)
        initSmallViewLayout()
        initBtnWindow()
        initObserver()

        setContent {
            ClickDeviceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScriptScreen(
                        scriptConfig = scriptConfig,

                        onBack = { finish() },
                        onOpenAccessibility = { openAccessibility() },
                        onStartScriptWindow = { startScriptWindow() },
                        onOpenScriptList = { openScriptList() },
                        onRunScript = { runScript() },
                        onStopScript = { stopScript() },
                        onConfigChange = { scriptConfig = it },
                        isRunning = isRunning
                    )
                }
            }
        }

        powerKeyObserver = PowerKeyObserver(this).apply {
            startListen()
            setHomeKeyListener {
                stopScript()
            }
        }
    }

    private fun initObserver() {
        observer = Observer<String> { s ->
            pkgNameNow = s ?: ""
            if (scriptConfig.checkAppChange) {
                if (pkgNameNow != thisPkgName) {
                    if (isRun) {
                        stopScript()
                    }
                }
            }
        }
        if (MyService.isStart()) {
            MyService.myService?.pkgNameMutableLiveData?.observeForever(observer!!)
        }
    }

    private fun openAccessibility() {
        try {
            startActivity(Intent("android.settings.ACCESSIBILITY_SETTINGS"))
        } catch (e: Exception) {
            startActivity(Intent("android.settings.SETTINGS"))
        }
    }

    private fun openScriptList() {
        if (isRun) {
            stopScript()
        }
        hideFloatWindows()
        startActivity(Intent(this, ScriptListActivityCompose::class.java))
    }

    private fun startScriptWindow() {
        if (!isShow) {
            showFloatWindows()
        } else {
            if (isRun) {
                stopScript()
            }
            hideFloatWindows()
        }
    }

    private fun runScript(data: List<ScriptCmdBean>? = null) {
        val config = scriptConfig
        val scriptData = data ?: MyLiveData.getInstance().with("json", String::class.java).value?.let { json ->
            try { Gson().fromJson<List<ScriptCmdBean>>(json, object : TypeToken<List<ScriptCmdBean>>() {}.type) } catch (e: Exception) { null }
        }
        if (!MyService.isStart()) {
            Toast.makeText(this, "请先开启辅助功能", Toast.LENGTH_LONG).show()
            return
        }
        if (scriptData.isNullOrEmpty()) {
            Toast.makeText(this, "请选择要执行的脚本", Toast.LENGTH_SHORT).show()
            return
        }
        val speed = config.speed
        delayCoefficient = if (speed < 0.25) 1.0 else if (speed > 5.0) 0.2 else 1.0 / speed
        thisPkgName = pkgNameNow
        isRun = true
        isRunning = true
        tvBtnWv?.text = "停止"
        singleThreadExecutor.execute {
            var j = 0
            while (isRun && (config.count <= 0 || j < config.count)) {
                scriptExecutor?.run(scriptData)
                var i2 = 0
                while (i2 < config.interval / 100 && isRun) {
                    try { Thread.sleep(100) } catch (e: InterruptedException) { break }
                    i2++
                }
                if (isRun) {
                    try { Thread.sleep((config.interval % 100).toLong()) } catch (e: InterruptedException) { break }
                }
                j++
            }
            isRun = false
            isRunning = false
            lifecycleScope.launch {
                tvBtnWv?.text = "开始"
            }
        }
    }

    private fun stopScript() {
        stopTime = System.currentTimeMillis()
        isRun = false
        isRunning = false
        tvBtnWv?.text = "开始"
    }

    // region 悬浮窗

    @SuppressLint("WrongConstant")
    private fun initSmallViewLayout() {
        btnWindowView = LayoutInflater.from(this).inflate(R.layout.window_b, null) as LinearLayout
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        btnLayoutParams = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, 8, PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnLayoutParams?.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        btnLayoutParams?.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
    }

    private fun initBtnWindow() {
        tvBtnWv = btnWindowView?.findViewById(R.id.tv_win_b)
        tvBtnWv?.text = "开始"
        tvBtnWv?.setOnTouchClick({
            if (stopTime + 2000 > System.currentTimeMillis()) {
                Toast.makeText(this, "点太快了,休息一下吧", Toast.LENGTH_SHORT).show()
                return@setOnTouchClick
            }
            runScript()
        }, {
            if (isRun) {
                stopScript()
                return@setOnTouchClick false
            }
            return@setOnTouchClick true
        })
    }

    @SuppressLint("WrongConstant")
    private fun alertWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
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
        if (wm != null && btnWindowView?.windowId == null) {
            wm?.addView(btnWindowView, btnLayoutParams)
        }
    }

    fun dismissWindow() {
        try {
            if (wm != null && btnWindowView?.windowId != null) {
                wm?.removeView(btnWindowView)
            }
        } catch (_: Exception) {
        }
    }

    private fun showFloatWindows() {
        alertWindow()
        isShow = true
    }

    private fun hideFloatWindows() {
        isShow = false
        dismissWindow()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Settings.canDrawOverlays(this)) {
                showWindow()
            } else {
                Toast.makeText(this, "悬浮窗权限被拒绝", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // endregion

    // region ScriptInterFace

    override fun delayedCmd(delayed: Int) {
        val time = (delayCoefficient * delayed).toInt()
        var i = 0
        while (i < time / 10 && isRun) {
            try { Thread.sleep(10) } catch (e: InterruptedException) { break }
            i++
        }
    }

    override fun clickCMD(x0: Int, y0: Int, duration: Int) {
        if (!isRun) return
        val service = myService ?: MyService.myService ?: return
        myService = service
        val xc = if (scriptConfig.xCoefficient in 0.25f..5.0f) scriptConfig.xCoefficient else 1.0f
        val yc = if (scriptConfig.yCoefficient in 0.25f..5.0f) scriptConfig.yCoefficient else 1.0f
        val sx = (x0 * xc).toInt()
        val sy = (y0 * yc).toInt()
        if (duration < 30) {
            service.dispatchGestureClick(sx.toFloat(), sy.toFloat())
            try { Thread.sleep(30) } catch (_: Exception) {}
        } else {
            val d = if (duration > 30000) 30000 else duration
            service.dispatchGestureClick(sx.toFloat(), sy.toFloat(), d)
            try { Thread.sleep(d.toLong()) } catch (_: Exception) {}
        }
    }

    override fun gestureCMD(x0: Int, y0: Int, x1: Int, y1: Int, duration: Int) {
        if (!isRun) return
        val service = myService ?: MyService.myService ?: return
        myService = service
        val xc = if (scriptConfig.xCoefficient in 0.25f..5.0f) scriptConfig.xCoefficient else 1.0f
        val yc = if (scriptConfig.yCoefficient in 0.25f..5.0f) scriptConfig.yCoefficient else 1.0f
        val sx0 = (x0 * xc).toInt()
        val sy0 = (y0 * yc).toInt()
        val sx1 = (x1 * xc).toInt()
        val sy1 = (y1 * yc).toInt()
        val d = when {
            duration > 30000 -> 30000
            duration < 100 -> 200
            else -> duration
        }
        service.dispatchGesture(sx0.toFloat(), sy0.toFloat(), sx1.toFloat(), sy1.toFloat(), d)
        try { Thread.sleep(d.toLong()) } catch (_: Exception) {}
    }

    // endregion

    override fun onDestroy() {
        super.onDestroy()
        isRun = false
        hideFloatWindows()
        powerKeyObserver?.stopListen()
        if (MyService.isStart()) {
            observer?.let { MyService.myService?.pkgNameMutableLiveData?.removeObserver(it) }
        }
        singleThreadExecutor.shutdownNow()
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQ_CODE = 2
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptScreen(
    scriptConfig: ScriptRunConfig,
    isFloatingWindowShow: Boolean=false,
    onBack: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onStartScriptWindow: () -> Unit,
    onOpenScriptList: () -> Unit,
    onRunScript: () -> Unit,
    onStopScript: () -> Unit,
    onConfigChange: (ScriptRunConfig) -> Unit,
    isRunning: Boolean
) {
    val context = LocalContext.current
    var scriptJson by remember { mutableStateOf("") }
    var scriptName by remember { mutableStateOf("") }
    var scriptData by remember { mutableStateOf<List<ScriptCmdBean>?>(null) }
    var isExecuting by remember { mutableStateOf(false) }
    var xCoeffText by remember { mutableStateOf("") }
    var yCoeffText by remember { mutableStateOf("") }
    var coeffSynced by remember { mutableStateOf(false) }
    var intervalText by remember { mutableStateOf(scriptConfig.interval.toString()) }
    var countText by remember { mutableStateOf(scriptConfig.count.toString()) }
    var speedText by remember { mutableStateOf(if (scriptConfig.speed == 1.0) "1" else scriptConfig.speed.toString()) }

    LaunchedEffect(scriptConfig.xCoefficient, scriptConfig.yCoefficient) {
        if (!coeffSynced && (scriptConfig.xCoefficient != 1.0f || scriptConfig.yCoefficient != 1.0f)) {
            xCoeffText = if (scriptConfig.xCoefficient == 1.0f) "" else scriptConfig.xCoefficient.toString()
            yCoeffText = if (scriptConfig.yCoefficient == 1.0f) "" else scriptConfig.yCoefficient.toString()
            coeffSynced = true
        }
    }

    LaunchedEffect(isRunning) {
        if (!isRunning) isExecuting = false
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        MyLiveData.getInstance().with("json", String::class.java)
            .observe(lifecycleOwner) { json ->
                scriptJson = json ?: ""
                scriptData = try {
                    Gson().fromJson(json, object : TypeToken<List<ScriptCmdBean>>() {}.type)
                } catch (e: Exception) { null }
            }
        MyLiveData.getInstance().with("scriptName", String::class.java)
            .observe(lifecycleOwner) { name ->
                scriptName = name ?: ""
            }
        MyLiveData.getInstance().with("xCoefficient", Float::class.java)
            .observe(lifecycleOwner) { coeff ->
                if (coeff != null && coeff > 0) {
                    onConfigChange(scriptConfig.copy(xCoefficient = coeff))
                }
            }
        MyLiveData.getInstance().with("yCoefficient", Float::class.java)
            .observe(lifecycleOwner) { coeff ->
                if (coeff != null && coeff > 0) {
                    onConfigChange(scriptConfig.copy(yCoefficient = coeff))
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("脚本执行") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                }
            )
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

            Text("脚本执行", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.matches(Regex("^\\d*$"))) {
                            intervalText = value
                            onConfigChange(scriptConfig.copy(interval = value.toIntOrNull() ?: 0))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("时间间隔(ms)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = countText,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.matches(Regex("^\\d*$"))) {
                            countText = value
                            onConfigChange(scriptConfig.copy(count = value.toIntOrNull() ?: 0))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("次数(0为无限)") },
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = speedText,
                onValueChange = { value ->
                    if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                        speedText = value
                        val parsed = value.toDoubleOrNull()
                        if (parsed != null) {
                            onConfigChange(scriptConfig.copy(speed = parsed))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("执行速度倍速(0.25~5)") },
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = scriptConfig.checkAppChange,
                    onCheckedChange = { onConfigChange(scriptConfig.copy(checkAppChange = it)) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("检测到应用切换时停止脚本", style = MaterialTheme.typography.bodyMedium)
            }

            Text("坐标系数", style = MaterialTheme.typography.titleMedium)
            Text(
                "实际坐标=原始坐标×系数。取值范围0.25~5，超出范围自动设为1",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("X:", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = xCoeffText,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                            xCoeffText = value
                            val parsed = value.toFloatOrNull()
                            if (parsed != null) {
                                onConfigChange(scriptConfig.copy(xCoefficient = parsed))
                            } else if (value.isEmpty()) {
                                onConfigChange(scriptConfig.copy(xCoefficient = 1.0f))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Text("Y:", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = yCoeffText,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                            yCoeffText = value
                            val parsed = value.toFloatOrNull()
                            if (parsed != null) {
                                onConfigChange(scriptConfig.copy(yCoefficient = parsed))
                            } else if (value.isEmpty()) {
                                onConfigChange(scriptConfig.copy(yCoefficient = 1.0f))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            Button(
                onClick = onStartScriptWindow,
                modifier = Modifier.fillMaxWidth(),
                colors = if (isFloatingWindowShow) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ) else ButtonDefaults.buttonColors()
            ) {
                Text(if (isFloatingWindowShow) "关闭悬浮窗" else "打开脚本悬浮窗")
            }

            Button(
                onClick = onOpenScriptList,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("选择脚本")
            }

            if (scriptName.isNotEmpty()) {
                Text("脚本名称: $scriptName", style = MaterialTheme.typography.bodyMedium)
            }

            if (scriptJson.isNotEmpty()) {
                Text(
                    text = scriptJson,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Util.copyText(scriptJson, context)
                            Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                        }
                )
            }




        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScriptScreenPreview() {
    ClickDeviceTheme {
        ScriptScreen(
            scriptConfig = ScriptRunConfig(),
            isFloatingWindowShow = false,
            onBack = {},
            onOpenAccessibility = {},
            onStartScriptWindow = {},
            onOpenScriptList = {},
            onRunScript = {},
            onStopScript = {},
            onConfigChange = {},
            isRunning = false
        )
    }
}

data class ScriptRunConfig(
    val interval: Int = 1000,
    val count: Int = 0,
    val speed: Double = 1.0,
    val checkAppChange: Boolean = false,
    val xCoefficient: Float = 1.0f,
    val yCoefficient: Float = 1.0f
)
