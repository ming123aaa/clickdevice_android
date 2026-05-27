package com.example.clickdevice.activity

import android.graphics.Matrix
import android.graphics.Path
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import com.Ohuang.ilivedata.MyLiveData
import com.example.clickdevice.MyService
import com.example.clickdevice.PowerKeyObserver
import com.example.clickdevice.RecordScriptExecutor
import com.example.clickdevice.bean.RecordScriptCmd
import com.example.clickdevice.databinding.WindowBBinding
import com.example.clickdevice.db.RecordScriptBean
import com.example.clickdevice.helper.SmallWindowsHelper
import com.example.clickdevice.helper.setOnTouchClick
import com.example.clickdevice.ui.theme.ClickDeviceTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RecordScriptPlayActivityCompose : ComponentActivity(), RecordScriptExecutor.RecordScriptInterface {

    private var isRun = false
    private val singleThreadExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val recordScriptExecutor = RecordScriptExecutor()
    private var data = ArrayList<RecordScriptCmd>()
    private var powerKeyObserver: PowerKeyObserver? = null
    private lateinit var playSmallWindowsHelper: SmallWindowsHelper
    private var windowBBinding: WindowBBinding? = null
    private var scriptName by mutableStateOf("")
    private var scriptJson by mutableStateOf("")
    private var time by mutableStateOf(1000L)
    private var count by mutableStateOf(0)
    private var xCoefficient by mutableStateOf(1.0f)
    private var yCoefficient by mutableStateOf(1.0f)
    private var checkAppChange by mutableStateOf(false)
    private var thisPkgName = ""
    private var pkgNameNow = ""
    private val observer = Observer<String> { s ->
        pkgNameNow = s
        if (checkAppChange && isRun) {
            if (pkgNameNow != thisPkgName) {
                isRun = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordScriptExecutor.recordScriptInterface = this
        setContent {
            ClickDeviceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RecordScriptPlayScreen(
                        scriptName = scriptName,
                        scriptJson = scriptJson,
                        time = time,
                        count = count,
                        xCoefficient = xCoefficient,
                        yCoefficient = yCoefficient,
                        checkAppChange = checkAppChange,
                        onTimeChange = { time = it },
                        onCountChange = { count = it },
                        onXCoefficientChange = { xCoefficient = it },
                        onYCoefficientChange = { yCoefficient = it },
                        onCheckAppChangeChange = { checkAppChange = it },
                        onBack = { finish() },
                        onOpenFloatWindow = { togglePlayWindow() }
                    )
                }
            }
        }

        initPlaySmallWindows()

        powerKeyObserver = PowerKeyObserver(this).apply {
            startListen()
            setHomeKeyListener { stopPlay() }
        }

        if (MyService.isStart()) {
            MyService.myService.pkgNameMutableLiveData.observeForever(observer)
        }

        MyLiveData.getInstance().with("RecordScriptPlay", RecordScriptBean::class.java)
            .observe(this) { bean ->
                bean?.let {
                    scriptName = it.name ?: ""
                    scriptJson = it.scriptJson ?: ""
                    xCoefficient = it.xCoefficient
                    yCoefficient = it.yCoefficient
                    data = try {
                        Gson().fromJson(it.scriptJson, object : TypeToken<List<RecordScriptCmd>>() {}.type)
                    } catch (e: Exception) { ArrayList() }
                }
            }
    }

    private fun initPlaySmallWindows() {
        if (MyService.isStart()) {
            playSmallWindowsHelper = SmallWindowsHelper(MyService.myService)
            val mLayoutParams = playSmallWindowsHelper.mLayoutParams
            mLayoutParams?.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            playSmallWindowsHelper = SmallWindowsHelper(this)
        }
        val mLayoutParams = playSmallWindowsHelper.mLayoutParams
        mLayoutParams?.gravity = Gravity.TOP
        windowBBinding = WindowBBinding.inflate(layoutInflater)
        windowBBinding?.tvWinB?.setOnTouchClick({
            if (!MyService.isStart()) {
                Toast.makeText(this, "请手动开启辅助功能，若已开启请重启应用再试一次。", Toast.LENGTH_LONG).show()
                return@setOnTouchClick
            }
            if (data.isEmpty()) {
                Toast.makeText(this, "没有脚本数据", Toast.LENGTH_LONG).show()
                return@setOnTouchClick
            }
            isRun = true
            thisPkgName = pkgNameNow
            singleThreadExecutor.execute(playRunnable)
            windowBBinding?.tvWinB?.text = "停止"
            Toast.makeText(this, "开始", Toast.LENGTH_LONG).show()
        }, {
            if (isRun) {
                isRun = false
                Toast.makeText(this, "停止", Toast.LENGTH_LONG).show()
                return@setOnTouchClick false
            }
            return@setOnTouchClick true
        })
    }

    private val playRunnable = Runnable {
        var i = 0
        while (isRun && (count <= 0 || i < count)) {
            recordScriptExecutor.run(data)
            if (isRun && (count <= 0 || i < count - 1)) {
                recordScriptExecutor.delay(time)
            }
            i++
        }
        isRun = false
        windowBBinding?.root?.post {
            windowBBinding?.tvWinB?.text = "开始"
        }
    }

    private fun togglePlayWindow() {
        if (playSmallWindowsHelper.isShow) {
            playSmallWindowsHelper.hide()
        } else {
            showPlayWindow()
        }
    }

    private fun showPlayWindow() {
        if (SmallWindowsHelper.requestPermission(this)) {
            if (playSmallWindowsHelper.root == null) {
                playSmallWindowsHelper.attach(windowBBinding?.root!!)
            } else {
                playSmallWindowsHelper.show()
            }
        }
    }

    private fun stopPlay() {
        isRun = false
        windowBBinding?.root?.post {
            windowBBinding?.tvWinB?.text = "开始"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRun = false
        powerKeyObserver?.stopListen()
        playSmallWindowsHelper.hide()
        if (MyService.isStart()) {
            MyService.myService.pkgNameMutableLiveData.removeObserver(observer)
        }
        singleThreadExecutor.shutdownNow()
    }

    override fun isRun() = isRun

    override fun preDispatchGesture(x: Int, y: Int) {
        windowBBinding?.root?.post {
            windowBBinding?.tvWinB?.apply {
                if (calcPointRange(this, x, y)) {
                    playNotTouch()
                }
            }
        }
    }

    override fun dispatchGesture(position: Int, path: Path, duration: Int) {
        if (MyService.isStart()) {
            val xc = if (xCoefficient in 0.25f..5.0f) xCoefficient else 1.0f
            val yc = if (yCoefficient in 0.25f..5.0f) yCoefficient else 1.0f
            if (xc != 1.0f || yc != 1.0f) {
                val matrix = Matrix()
                matrix.setScale(xc, yc)
                val scaledPath = Path()
                path.transform(matrix, scaledPath)
                MyService.myService.dispatchGesture(scaledPath, duration)
            } else {
                MyService.myService.dispatchGesture(path, duration)
            }
        }
    }

    override fun endDispatchGesture() {
        windowBBinding?.root?.post {
            playCanTouch()
        }
    }

    private fun playNotTouch() {
        val mLayoutParams = playSmallWindowsHelper.mLayoutParams
        mLayoutParams?.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        playSmallWindowsHelper.mLayoutParams = mLayoutParams!!
        windowBBinding?.tvWinB?.setTextColor(0xff000000.toInt())
    }

    private fun playCanTouch() {
        val mLayoutParams = playSmallWindowsHelper.mLayoutParams
        mLayoutParams?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        playSmallWindowsHelper.mLayoutParams = mLayoutParams!!
        windowBBinding?.tvWinB?.setTextColor(0xffff0000.toInt())
    }

    private fun calcPointRange(view: View, x: Int, y: Int): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return x >= location[0] && x <= location[0] + view.width && y >= location[1] && y <= location[1] + view.height
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScriptPlayScreen(
    scriptName: String,
    scriptJson: String,
    time: Long,
    count: Int,
    xCoefficient: Float,
    yCoefficient: Float,
    checkAppChange: Boolean,
    onTimeChange: (Long) -> Unit,
    onCountChange: (Int) -> Unit,
    onXCoefficientChange: (Float) -> Unit,
    onYCoefficientChange: (Float) -> Unit,
    onCheckAppChangeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onOpenFloatWindow: () -> Unit
) {
    var xCoeffText by remember { mutableStateOf("") }
    var yCoeffText by remember { mutableStateOf("") }
    var coeffSynced by remember { mutableStateOf(false) }
    var timeText by remember { mutableStateOf(time.toString()) }
    var countText by remember { mutableStateOf(count.toString()) }

    LaunchedEffect(xCoefficient, yCoefficient) {
        if (!coeffSynced && (xCoefficient != 1.0f || yCoefficient != 1.0f)) {
            xCoeffText = if (xCoefficient == 1.0f) "" else xCoefficient.toString()
            yCoeffText = if (yCoefficient == 1.0f) "" else yCoefficient.toString()
            coeffSynced = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("录制脚本播放") },
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
            

            if (scriptName.isNotEmpty()) {
                Text("脚本名称: $scriptName", style = MaterialTheme.typography.bodyLarge)
            }

        

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("间隔(ms):", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.matches(Regex("^\\d*$"))) {
                            timeText = value
                            val newTime = value.toLongOrNull() ?: 1000L
                            if (newTime >= 100) onTimeChange(newTime)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Text("次数:", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = countText,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.matches(Regex("^\\d*$"))) {
                            countText = value
                            val newCount = value.toIntOrNull() ?: 1
                            if (newCount >= 1) onCountChange(newCount)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checkAppChange,
                    onCheckedChange = onCheckAppChangeChange
                )
                Text("应用切换停止运行")
            }

            Text("坐标系数", style = MaterialTheme.typography.titleMedium)
            Text(
                "实际坐标=原始坐标×系数。取值范围0.25~5，超出范围自动设为1",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("X系数:", style = MaterialTheme.typography.bodyMedium)
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
                Text("Y系数:", style = MaterialTheme.typography.bodyMedium)
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
                onClick = onOpenFloatWindow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("打开脚本悬浮窗")
            }
           
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecordScriptPlayScreenPreview() {
    ClickDeviceTheme {
        RecordScriptPlayScreen(
            scriptName = "测试脚本",
            scriptJson = "[]",
            time = 1000,
            count = 1,
            xCoefficient = 1.0f,
            yCoefficient = 1.0f,
            checkAppChange = false,
            onTimeChange = {},
            onCountChange = {},
            onXCoefficientChange = {},
            onYCoefficientChange = {},
            onCheckAppChangeChange = {},
            onBack = {},
            onOpenFloatWindow = {}
        )
    }
}