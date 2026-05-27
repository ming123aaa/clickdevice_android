package com.example.clickdevice.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.Ohuang.ilivedata.MyLiveData
import com.example.clickdevice.MyService
import com.example.clickdevice.R
import com.example.clickdevice.RecordScriptExecutor
import com.example.clickdevice.bean.RecordScriptCmd
import com.example.clickdevice.databinding.WindowCanvesBinding
import com.example.clickdevice.db.AppDatabase
import com.example.clickdevice.db.RecordScriptBean
import com.example.clickdevice.helper.SmallWindowsHelper
import com.example.clickdevice.helper.setOnTouchClick
import com.example.clickdevice.ui.theme.ClickDeviceTheme
import com.example.clickdevice.view.RecordTouchView
import com.example.clickdevice.vm.RecordScriptViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecordScriptActivityCompose : ComponentActivity(), RecordScriptExecutor.RecordScriptInterface {

    private var viewModel: RecordScriptViewModel? = null
    private var smallWindowsHelper: SmallWindowsHelper? = null
    private var playSmallWindowsHelper: SmallWindowsHelper? = null
    private var smallWindowBinding: WindowCanvesBinding? = null
    private var windowBtnBinding: com.example.clickdevice.databinding.WindowBBinding? = null

    private var runnable1: Runnable? = null
    private var runnable2: Runnable? = null
    private var isRun = false
    private val mainHandler = Handler(Looper.getMainLooper())

    var commandCount by mutableStateOf(0)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[RecordScriptViewModel::class.java]
        viewModel?.recordScriptExecutor?.recordScriptInterface = this
        initSmallWindows()
        initPlaySmallWindows()

        val isEdit = intent.getBooleanExtra("isEdit", false)
        if (isEdit) {
            MyLiveData.getInstance().with("RecordScriptEdit", RecordScriptBean::class.java)
                .observe(this) { bean ->
                    bean?.let {
                        viewModel?.recordScriptBean = it
                        viewModel?.data = try {
                            Gson().fromJson(
                                it.scriptJson,
                                object : TypeToken<List<RecordScriptCmd>>() {}.type
                            ) ?: ArrayList()
                        } catch (_: Exception) { ArrayList() }
                        commandCount = viewModel?.data?.size ?: 0
                    }
                }
        }

        setContent {
            ClickDeviceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RecordScriptEditScreen(
                        isEdit = isEdit,
                        commandCount = commandCount,
                        onBack = { finish() },
                        onOpenRecordWindow = { openRecordWindow() },
                        onPlay = { togglePlayWindow() },
                        onSave = { name, xCoeff, yCoeff -> saveScript(name, xCoeff, yCoeff) }
                    )
                }
            }
        }
    }

    private fun initSmallWindows() {
        if (!MyService.isStart()) return
        smallWindowsHelper = SmallWindowsHelper(MyService.myService)
        val mLayoutParams = smallWindowsHelper?.mLayoutParams
        mLayoutParams?.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        smallWindowBinding = WindowCanvesBinding.inflate(layoutInflater)
        smallWindowBinding?.recordTouchView?.scriptListener =
            object : RecordTouchView.ScriptListener {
                override fun onActionDown() {
                    viewModel?.addDelayTime()
                }

                override fun onUpdate(recordScriptCmd: RecordScriptCmd, path: Path) {
                    notTouch()
                    if (MyService.isStart()) {
                        dispatchGesturePath(path, recordScriptCmd)
                        viewModel?.addRecordScriptCmd(recordScriptCmd)
                        commandCount = viewModel?.data?.size ?: 0
                    }
                }
            }

        smallWindowBinding?.tvStart?.setOnTouchClick({
            viewModel?.apply {
                if (!isRecord) {
                    smallWindowBinding?.tvStart?.text = "停止"
                    startRecord()
                }
            }
        }, {
            viewModel?.apply {
                if (isRecord) {
                    smallWindowBinding?.tvStart?.text = "开始"
                    stopRecord()
                    mainHandler.removeCallbacks(runnable1 ?: return@setOnTouchClick false)
                    mainHandler.removeCallbacks(runnable2 ?: return@setOnTouchClick false)
                    return@setOnTouchClick false
                }
            }
            return@setOnTouchClick true
        })

        smallWindowBinding?.tvClose?.setOnClickListener {
            closeSmallWindow()
        }

        smallWindowBinding?.tvHide?.setOnClickListener {
            smallWindowBinding?.layout1?.visibility = View.GONE
            Toast.makeText(this,"3秒后显示按钮", Toast.LENGTH_SHORT).show()
            mainHandler.postDelayed({
                smallWindowBinding?.layout1?.visibility = View.VISIBLE
            }, 3000)
        }
    }

    private fun initPlaySmallWindows() {
        playSmallWindowsHelper = SmallWindowsHelper(this)
        val mLayoutParams = playSmallWindowsHelper?.mLayoutParams
        mLayoutParams?.gravity = Gravity.TOP
        windowBtnBinding = com.example.clickdevice.databinding.WindowBBinding.inflate(layoutInflater)
        windowBtnBinding?.tvWinB?.text = "开始"
        windowBtnBinding?.tvWinB?.setOnTouchClick({
            isRun = true
            viewModel?.singleThreadExecutor?.execute(playRunnable)
            windowBtnBinding?.tvWinB?.text = "停止"
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
        viewModel?.playScript()
        isRun = false
        mainHandler.post {
            windowBtnBinding?.tvWinB?.text = "开始"
        }
    }

    private fun openRecordWindow() {
        if (!MyService.isStart()) {
            Toast.makeText(this, "请手动开启辅助功能，若已开启请重启应用再试一次。", Toast.LENGTH_LONG).show()
            return
        }
        showSmallWindows()
        playSmallWindowsHelper?.hide()
        isRun = false
    }

    private fun togglePlayWindow() {
        if (playSmallWindowsHelper?.isShow == true) {
            playSmallWindowsHelper?.hide()
        } else {
            showPlayWindow()
            closeSmallWindow()
        }
    }

    private fun showSmallWindows() {
        if (SmallWindowsHelper.requestPermission(this)) {
            if (smallWindowsHelper?.root == null) {
                smallWindowsHelper?.attach(smallWindowBinding?.root!!)
                val mLayoutParams = smallWindowsHelper?.mLayoutParams
                mLayoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
                mLayoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
                smallWindowsHelper?.mLayoutParams = mLayoutParams!!
            } else {
                smallWindowsHelper?.show()
            }
            canTouch()
        }
    }

    private fun showPlayWindow() {
        if (SmallWindowsHelper.requestPermission(this)) {
            if (playSmallWindowsHelper?.root == null) {
                playSmallWindowsHelper?.attach(windowBtnBinding?.root!!)
            } else {
                playSmallWindowsHelper?.show()
            }
        }
    }

    private fun closeSmallWindow() {
        mainHandler.removeCallbacks(runnable1 ?: Runnable {})
        mainHandler.removeCallbacks(runnable2 ?: Runnable {})
        smallWindowBinding?.tvStart?.text = "开始"
        viewModel?.stopRecord()
        smallWindowsHelper?.hide()
    }

    private fun dispatchGesturePath(path: Path, recordScriptCmd: RecordScriptCmd) {
        mainHandler.removeCallbacks(runnable1 ?: Runnable {})
        mainHandler.removeCallbacks(runnable2 ?: Runnable {})
        runnable2 = Runnable {
            canTouch()
            viewModel?.postLastTime()
        }
        runnable1 = Runnable {
            MyService.myService.dispatchGesture(path, recordScriptCmd.duration)
            mainHandler.postDelayed(
                runnable2!!,
                recordScriptCmd.duration.toLong()
            )
        }
        mainHandler.postDelayed(runnable1!!, 100)
    }

    private fun notTouch() {
        smallWindowBinding?.recordTouchView?.isEnabled = false
        val mLayoutParams = smallWindowsHelper?.mLayoutParams
        mLayoutParams?.flags =
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        smallWindowsHelper?.mLayoutParams = mLayoutParams!!
        smallWindowBinding?.recordTouchView?.setBackgroundColor(0x30805000)
    }

    private fun canTouch() {
        smallWindowBinding?.recordTouchView?.isEnabled = true
        val mLayoutParams = smallWindowsHelper?.mLayoutParams
        mLayoutParams?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        smallWindowsHelper?.mLayoutParams = mLayoutParams!!
        smallWindowBinding?.recordTouchView?.setBackgroundColor(0x30005080)
    }

    private fun playNotTouch() {
        val mLayoutParams = playSmallWindowsHelper?.mLayoutParams
        mLayoutParams?.flags =
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        playSmallWindowsHelper?.mLayoutParams = mLayoutParams!!
        windowBtnBinding?.tvWinB?.setTextColor(0xff000000.toInt())
    }

    private fun playCanTouch() {
        val mLayoutParams = playSmallWindowsHelper?.mLayoutParams
        mLayoutParams?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        playSmallWindowsHelper?.mLayoutParams = mLayoutParams!!
        windowBtnBinding?.tvWinB?.setTextColor(0xffff0000.toInt())
    }

    private fun saveScript(name: String, xCoefficient: Float = 1.0f, yCoefficient: Float = 1.0f) {
        if (name.isBlank()) {
            Toast.makeText(this, "请输入脚本名", Toast.LENGTH_LONG).show()
            return
        }
        val validXCoeff = if (xCoefficient in 0.25f..5.0f) xCoefficient else 1.0f
        val validYCoeff = if (yCoefficient in 0.25f..5.0f) yCoefficient else 1.0f
        if (validXCoeff != xCoefficient || validYCoeff != yCoefficient) {
            Toast.makeText(this, "坐标系数超出范围(0.25~5)，已自动设为1", Toast.LENGTH_SHORT).show()
        }
        viewModel?.let { vm ->
            vm.singleThreadExecutor.execute {
                vm.saveScriptBlocking(this, name, validXCoeff, validYCoeff)
                mainHandler.post {
                    Toast.makeText(this, "保存成功", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        SmallWindowsHelper.onActivityResult(this, requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRun = false
        smallWindowsHelper?.hide()
        playSmallWindowsHelper?.hide()
    }

    // region RecordScriptInterface

    override fun isRun(): Boolean = isRun

    override fun preDispatchGesture(x: Int, y: Int) {
        windowBtnBinding?.root?.post {
            windowBtnBinding?.tvWinB?.apply {
                val location = IntArray(2)
                getLocationOnScreen(location)
                if (x >= location[0] && x <= location[0] + width &&
                    y >= location[1] && y <= location[1] + height
                ) {
                    playNotTouch()
                }
            }
        }
    }

    override fun dispatchGesture(position: Int, path: Path, duration: Int) {
        if (MyService.isStart()) {
            MyService.myService.dispatchGesture(path, duration)
        }
    }

    override fun endDispatchGesture() {
        windowBtnBinding?.root?.post {
            playCanTouch()
        }
    }

    // endregion
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScriptEditScreen(
    isEdit: Boolean,
    commandCount: Int = 0,
    onBack: () -> Unit,
    onOpenRecordWindow: () -> Unit,
    onPlay: () -> Unit,
    onSave: (String, Float, Float) -> Unit
) {
    val context = LocalContext.current
    val viewModel = (context as? RecordScriptActivityCompose)?.let {
        ViewModelProvider(it)[RecordScriptViewModel::class.java]
    }
    var scriptName by remember { mutableStateOf("") }
    var xCoefficient by remember { mutableFloatStateOf(1.0f) }
    var yCoefficient by remember { mutableFloatStateOf(1.0f) }
    var xCoeffText by remember { mutableStateOf("") }
    var yCoeffText by remember { mutableStateOf("") }
    var commands by remember { mutableStateOf<List<RecordScriptCmd>>(emptyList()) }
    val owner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        MyLiveData.getInstance().with("RecordScriptEdit", RecordScriptBean::class.java)
            .observe(owner) { bean ->
                bean?.let {
                    scriptName = it.name ?: ""
                    xCoefficient = it.xCoefficient
                    yCoefficient = it.yCoefficient
                    xCoeffText = if (it.xCoefficient == 1.0f) "" else it.xCoefficient.toString()
                    yCoeffText = if (it.yCoefficient == 1.0f) "" else it.yCoefficient.toString()
                }
            }
    }

    LaunchedEffect(commandCount) {
        viewModel?.let { vm ->
            commands = ArrayList(vm.data)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "编辑录制脚本" else "新建录制脚本") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { onSave(scriptName, xCoefficient, yCoefficient) }) {
                        Text("保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = scriptName,
                onValueChange = { scriptName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("脚本名称") },
                singleLine = true
            )

            Text("坐标系数", style = MaterialTheme.typography.titleSmall)
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
                            if (parsed != null) xCoefficient = parsed
                            else if (value.isEmpty()) xCoefficient = 1.0f
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
                            if (parsed != null) yCoefficient = parsed
                            else if (value.isEmpty()) yCoefficient = 1.0f
                        }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenRecordWindow,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("打开录制窗口")
                }
                Button(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("播放")
                }
            }

            Divider()

            Text(
                "命令列表 (${commands.size}条)",
                style = MaterialTheme.typography.titleMedium
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(commands) { index, cmd ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = getCmdDescribe(index, cmd),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private fun getCmdDescribe(index: Int, cmd: RecordScriptCmd): String {
    val prefix = "$index.  "
    return when (cmd.type) {
        RecordScriptCmd.Type.Gesture -> prefix + "手势执行" + cmd.duration + "ms"
        RecordScriptCmd.Type.Delay -> prefix + "延时" + cmd.delayed + "ms"
        else -> prefix + "未知命令"
    }
}

@Preview(showBackground = true)
@Composable
fun RecordScriptEditScreenPreview() {
    ClickDeviceTheme {
        RecordScriptEditScreen(
            isEdit = false,
            onBack = {},
            onOpenRecordWindow = {},
            onPlay = {},
            onSave = { _, _, _ -> }
        )
    }
}
