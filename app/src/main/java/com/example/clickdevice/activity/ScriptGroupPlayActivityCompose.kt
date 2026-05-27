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
import android.view.View
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import com.Ohuang.ilivedata.MyLiveData
import com.example.clickdevice.MyService
import com.example.clickdevice.PowerKeyObserver
import com.example.clickdevice.R
import com.example.clickdevice.ScriptExecutor
import com.example.clickdevice.Util
import com.example.clickdevice.bean.ScriptCmdBean
import com.example.clickdevice.bean.ScriptGroup
import com.example.clickdevice.helper.setOnTouchClick
import com.example.clickdevice.ui.theme.ClickDeviceTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScriptGroupPlayActivityCompose : ComponentActivity(), ScriptExecutor.ScriptInterFace {

    private var isRun = false
    private var stopTime = 0L
    private val singleThreadExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var scriptExecutor: ScriptExecutor? = null
    private var myService: MyService? = null
    private var delayCoefficient = 1.0
    private var powerKeyObserver: PowerKeyObserver? = null
    private var observer: Observer<String>? = null
    private var scriptGroup: ScriptGroup? = null
    private var checkAppChange = false
    private var thisPkgName = ""
    private var pkgNameNow = ""

    private var btnWindowView: LinearLayout? = null
    private var tvBtnWv: TextView? = null
    private var wm: WindowManager? = null
    private var btnLayoutParams: WindowManager.LayoutParams? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    var isRunning by mutableStateOf(false)
        private set
    var currentJson by mutableStateOf("")
        private set
    var scriptGroupName by mutableStateOf("")
        private set
    var xCoefficient by mutableStateOf(1.0f)
        private set
    var yCoefficient by mutableStateOf(1.0f)
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
                    ScriptGroupPlayScreen(
                        xCoefficient = xCoefficient,
                        yCoefficient = yCoefficient,
                        onXCoefficientChange = { xCoefficient = it },
                        onYCoefficientChange = { yCoefficient = it },
                        onBack = { finish() },
                        onOpenAccessibility = { openAccessibility() },
                        onStartScriptWindow = { startScriptWindow() },
                        onOpenScriptList = { openScriptList() },
                        onRunScript = { time, count, speed, actionIndex, checkChange ->
                            runScript(time, count, speed, actionIndex, checkChange)
                        },
                        onStopScript = { stopScript() },
                        isRunning = isRunning
                    )
                }
            }
        }

        powerKeyObserver = PowerKeyObserver(this).apply {
            startListen()
            setHomeKeyListener { stopScript() }
        }

        MyLiveData.getInstance().with("ScriptGroup", ScriptGroup::class.java)
            .observe(this) { group ->
                scriptGroup = group
                scriptGroupName = group?.name ?: ""
                group?.let {
                    xCoefficient = it.xCoefficient
                    yCoefficient = it.yCoefficient
                }
            }
    }

    private fun initObserver() {
        observer = Observer<String> { s ->
            pkgNameNow = s ?: ""
            if (checkAppChange) {
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
        if (isRun) stopScript()
        hideFloatWindows()
        startActivity(Intent(this, ScriptGroupListActivityCompose::class.java))
    }

    private var isShow = false

    private fun startScriptWindow() {
        if (!isShow) {
            showFloatWindows()
        } else {
            if (isRun) stopScript()
            hideFloatWindows()
        }
    }

    private fun runScript(time: Int, count: Int, speed: Double, actionIndex: Int, checkChange: Boolean) {
        if (!MyService.isStart()) {
            Toast.makeText(this, "请先开启辅助功能", Toast.LENGTH_LONG).show()
            return
        }
        val group = scriptGroup
        if (group == null || group.actionScript.isEmpty()) {
            Toast.makeText(this, "请选择脚本", Toast.LENGTH_SHORT).show()
            return
        }
        delayCoefficient = if (speed in 0.25..5.0) 1.0 / speed else 1.0
        checkAppChange = checkChange
        thisPkgName = pkgNameNow
        val action = group.actionScript.getOrElse(actionIndex) { return }
        val commands = group.getListScriptCmdBean(action)
        currentJson = action.script.toString()
        isRun = true
        isRunning = true
        tvBtnWv?.text = "停止"
        singleThreadExecutor.execute {
            var j = 0
            while (isRun && (count <= 0 || j < count)) {
                scriptExecutor?.run(commands)
                var i2 = 0
                while (i2 < time / 100 && isRun) {
                    try { Thread.sleep(100) } catch (e: InterruptedException) { break }
                    i2++
                }
                if (isRun) {
                    try { Thread.sleep((time % 100).toLong()) } catch (e: InterruptedException) { break }
                }
                j++
            }
            isRun = false
            isRunning = false
            mainHandler.post { tvBtnWv?.text = "开始" }
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
            val group = scriptGroup
            if (group == null || group.actionScript.isEmpty()) {
                Toast.makeText(this, "请选择脚本", Toast.LENGTH_SHORT).show()
                return@setOnTouchClick
            }
            runScript(1000, 1, 1.0, 0, false)
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
        } catch (_: Exception) {}
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
        val xc = if (xCoefficient in 0.25f..5.0f) xCoefficient else 1.0f
        val yc = if (yCoefficient in 0.25f..5.0f) yCoefficient else 1.0f
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
        val xc = if (xCoefficient in 0.25f..5.0f) xCoefficient else 1.0f
        val yc = if (yCoefficient in 0.25f..5.0f) yCoefficient else 1.0f
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
        private const val OVERLAY_PERMISSION_REQ_CODE = 3
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptGroupPlayScreen(
    xCoefficient: Float = 1.0f,
    yCoefficient: Float = 1.0f,
    onXCoefficientChange: (Float) -> Unit = {},
    onYCoefficientChange: (Float) -> Unit = {},
    isFloatingWindowShow: Boolean=false,
    onBack: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onStartScriptWindow: () -> Unit,
    onOpenScriptList: () -> Unit,
    onRunScript: (time: Int, count: Int, speed: Double, actionIndex: Int, checkAppChange: Boolean) -> Unit,
    onStopScript: () -> Unit,
    isRunning: Boolean
) {
    val context = LocalContext.current
    var intervalTime by remember { mutableStateOf("1000") }
    var clickCount by remember { mutableStateOf("0") }
    var speed by remember { mutableStateOf("1") }
    var checkAppChange by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var localGroup by remember { mutableStateOf<ScriptGroup?>(null) }
    var xCoeffText by remember { mutableStateOf("") }
    var yCoeffText by remember { mutableStateOf("") }
    var coeffSynced by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(xCoefficient, yCoefficient) {
        if (!coeffSynced && (xCoefficient != 1.0f || yCoefficient != 1.0f)) {
            xCoeffText = if (xCoefficient == 1.0f) "" else xCoefficient.toString()
            yCoeffText = if (yCoefficient == 1.0f) "" else yCoefficient.toString()
            coeffSynced = true
        }
    }

    LaunchedEffect(Unit) {
        MyLiveData.getInstance().with("ScriptGroup", ScriptGroup::class.java)
            .observe(lifecycleOwner) { group ->
                localGroup = group
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自定义脚本播放") },
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
                    value = intervalTime,
                    onValueChange = { intervalTime = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.weight(1f),
                    label = { Text("时间间隔(ms)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = clickCount,
                    onValueChange = { clickCount = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.weight(1f),
                    label = { Text("次数(0为无限)") },
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = speed,
                onValueChange = { speed = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("延迟系数(0.25~5)") },
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checkAppChange,
                    onCheckedChange = { checkAppChange = it }
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("X:", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = xCoeffText,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                            xCoeffText = value
                            val parsed = value.toFloatOrNull()
                            if (parsed != null) onXCoefficientChange(parsed)
                            else if (value.isEmpty()) onXCoefficientChange(1.0f)
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
                            if (parsed != null) onYCoefficientChange(parsed)
                            else if (value.isEmpty()) onYCoefficientChange(1.0f)
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

            Divider()

            localGroup?.let { group ->
                Text("脚本名称: ${group.name}", style = MaterialTheme.typography.titleMedium)

                if (group.actionScript.isNotEmpty()) {
                    Text("选择动作:", style = MaterialTheme.typography.bodyMedium)
                    group.actionScript.forEachIndexed { index, action ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedIndex == index,
                                onClick = { selectedIndex = index }
                            )
                            Text(
                                text = action.name.ifEmpty { "动作 ${index + 1}" },
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    val selectedAction = group.actionScript.getOrNull(selectedIndex)
                    selectedAction?.let { action ->
                        Text(
                            text = action.script.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Util.copyText(action.script.toString(), context)
                                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
            } ?: Text("未加载脚本组", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScriptGroupPlayScreenPreview() {
    ClickDeviceTheme {
        ScriptGroupPlayScreen(
            isFloatingWindowShow = false,
            onBack = {},
            onOpenAccessibility = {},
            onStartScriptWindow = {},
            onOpenScriptList = {},
            onRunScript = { _, _, _, _, _ -> },
            onStopScript = {},
            isRunning = false
        )
    }
}
