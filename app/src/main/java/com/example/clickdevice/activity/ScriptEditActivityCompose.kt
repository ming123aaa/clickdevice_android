package com.example.clickdevice.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager

import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.Ohuang.ilivedata.MyLiveData
import com.example.clickdevice.R
import com.example.clickdevice.SmallWindowView
import com.example.clickdevice.bean.ScriptCmdBean
import com.example.clickdevice.db.AppDatabase
import com.example.clickdevice.db.ScriptDataBean
import com.example.clickdevice.helper.smallWindowManager
import com.example.clickdevice.ui.theme.ClickDeviceTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class ScriptEditActivityCompose : ComponentActivity() {

    private var windowView: SmallWindowView? = null
    private var btnWindowView: SmallWindowView? = null
    private var tvBtnWv: TextView? = null
    private var wm: WindowManager? = null
    private var mLayoutParams: WindowManager.LayoutParams? = null
    private var btnLayoutParams: WindowManager.LayoutParams? = null

    // 悬浮窗回调：将坐标写回 EditText
    var onCoordinatePicked: ((x: Int, y: Int) -> Unit)? = null

    private val isNew: Boolean by lazy {
        intent.getBooleanExtra("isNew", true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSmallViewLayout()
        initBtnWindow()
        setContent {
            ClickDeviceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScriptEditScreen(
                        isNew = isNew,
                        onBack = { finish() },
                        activity = this@ScriptEditActivityCompose
                    )
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    private fun initSmallViewLayout() {
        wm = smallWindowManager()

        windowView = LayoutInflater.from(this).inflate(R.layout.window_a, null) as SmallWindowView
        mLayoutParams = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, 8, PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mLayoutParams?.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        mLayoutParams?.gravity = android.view.Gravity.NO_GRAVITY
        windowView?.setWm(wm)
        windowView?.setWmParams(mLayoutParams)
    }

    private fun initBtnWindow() {
        btnWindowView =
            LayoutInflater.from(this).inflate(R.layout.window_b, null) as SmallWindowView
        btnLayoutParams = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, 8, PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnLayoutParams?.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        btnLayoutParams?.gravity =
            android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
        btnWindowView?.enableMove = false
        btnWindowView?.setWm(wm)
        btnWindowView?.setWmParams(btnLayoutParams)

        tvBtnWv = btnWindowView?.findViewById(R.id.tv_win_b)
        tvBtnWv?.text = "完成"
        tvBtnWv?.setOnClickListener {
            if (windowView != null) {
                val x = windowView!!.actionUpX
                val y = windowView!!.actionUpY
                onCoordinatePicked?.invoke(x, y)
            }
            dismissWindow()
        }
    }

    @SuppressLint("WrongConstant")
    fun alertWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "can not DrawOverlays", Toast.LENGTH_SHORT).show()
                startActivityForResult(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    ),
                    2
                )
                return
            }
        }
        showWindow()
    }

    private fun showWindow() {
        if (wm != null && windowView?.windowId == null) {
            wm?.addView(windowView, mLayoutParams)
        }
        if (wm != null && btnWindowView?.windowId == null) {
            wm?.addView(btnWindowView, btnLayoutParams)
        }
    }

    fun dismissWindow() {
        try {
            if (wm != null && windowView?.windowId != null) {
                wm?.removeView(windowView)
            }
            if (wm != null && btnWindowView?.windowId != null) {
                wm?.removeView(btnWindowView)
            }
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        dismissWindow()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditScreen(
    isNew: Boolean,
    onBack: () -> Unit,
    activity: ScriptEditActivityCompose
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var scriptName by remember { mutableStateOf("") }
    var xCoefficient by remember { mutableFloatStateOf(1.0f) }
    var yCoefficient by remember { mutableFloatStateOf(1.0f) }
    var xCoeffText by remember { mutableStateOf("") }
    var yCoeffText by remember { mutableStateOf("") }
    var scriptDataBean by remember { mutableStateOf<ScriptDataBean?>(null) }
    var cmdList by remember { mutableStateOf(mutableListOf<ScriptCmdBean>()) }

    // 对话框状态
    var showDelayDialog by remember { mutableStateOf(false) }
    var showForDialog by remember { mutableStateOf(false) }
    var showClickDialog by remember { mutableStateOf(false) }
    var showGestureDialog by remember { mutableStateOf(false) }
    var showRandomClickDialog by remember { mutableStateOf(false) }
    var showJsonDialog by remember { mutableStateOf(false) }
    var showCmdTypeMenu by remember { mutableStateOf(false) }

    // 当前操作的命令索引（-1 表示追加，>=0 表示在该位置插入或编辑）
    var currentInsertIndex by remember { mutableIntStateOf(-1) }
    // 当前正在编辑的命令（null 表示新建）
    var editingCmd by remember { mutableStateOf<ScriptCmdBean?>(null) }

    // 观察 LiveData 中的 ScriptDataBean
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        if (isNew) return@LaunchedEffect
        MyLiveData.getInstance().with("ScriptDataBean", ScriptDataBean::class.java)
            .observe(lifecycleOwner) { bean ->
                scriptDataBean = bean
                scriptName = bean?.name ?: ""
                xCoefficient = bean?.xCoefficient ?: 1.0f
                yCoefficient = bean?.yCoefficient ?: 1.0f
                xCoeffText = if ((bean?.xCoefficient ?: 1.0f) == 1.0f) "" else (bean?.xCoefficient
                    ?: 1.0f).toString()
                yCoeffText = if ((bean?.yCoefficient ?: 1.0f) == 1.0f) "" else (bean?.yCoefficient
                    ?: 1.0f).toString()
                val list: List<ScriptCmdBean>? = bean?.scriptJson?.let {
                    Gson().fromJson(it, object : TypeToken<List<ScriptCmdBean>>() {}.type)
                }
                if (list != null) {
                    cmdList = list.toMutableList()
                }
            }
    }

    // 菜单命令类型
    val cmdTypes = listOf(
        "点击命令",
        "延时命令",
        "滑屏命令",
        "循环开始",
        "循环结束",
        "随机点击",
        "json数据导入"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "新建脚本" else "编辑脚本", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            if (scriptName.isBlank()) {
                                Toast.makeText(context, "请输入名称", Toast.LENGTH_SHORT).show()
                                return@FilledTonalButton
                            }
                            val validXCoeff = if (xCoefficient in 0.25f..5.0f) xCoefficient else 1.0f
                            val validYCoeff = if (yCoefficient in 0.25f..5.0f) yCoefficient else 1.0f
                            if (validXCoeff != xCoefficient || validYCoeff != yCoefficient) {
                                Toast.makeText(
                                    context,
                                    "坐标系数超出范围(0.25~5)，已自动设为1",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    val format = "yyyy-MM-dd HH:mm:ss"
                                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                                    val time = sdf.format(Date(System.currentTimeMillis()))
                                    val gson = Gson()
                                    val scriptJson = gson.toJson(cmdList)
                                    val appDatabase = AppDatabase.getInstance(context)
                                    if (isNew) {
                                        val bean = ScriptDataBean(scriptName, time, time, scriptJson)
                                        bean.xCoefficient = validXCoeff
                                        bean.yCoefficient = validYCoeff
                                        appDatabase.getScriptDao().insertScriptDataBean(bean)
                                    } else {
                                        val bean = scriptDataBean ?: ScriptDataBean()
                                        bean.updateTime = time
                                        bean.name = scriptName
                                        bean.scriptJson = scriptJson
                                        bean.xCoefficient = validXCoeff
                                        bean.yCoefficient = validYCoeff
                                        appDatabase.getScriptDao().insertScriptDataBean(bean)
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    activity.finish()
                                }
                            }
                        },
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = scriptName,
                onValueChange = { scriptName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("脚本名称") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("坐标系数", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "实际坐标=原始坐标x系数，取值范围 0.25~5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("X", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Text("Y", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = {
                    currentInsertIndex = -1
                    editingCmd = null
                    showCmdTypeMenu = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("插入命令")
            }

            // 命令列表
            val listState = rememberLazyListState()
            var draggedItemIndex by remember { mutableIntStateOf(-1) }
            var dragAccumY by remember { mutableFloatStateOf(0f) }
            val itemHeightPx = with(LocalDensity.current) { 120.dp.toPx() }
            val spacingPx = with(LocalDensity.current) { 4.dp.toPx() }
            val totalItemHeightPx = itemHeightPx + spacingPx

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(cmdList) { index, cmd ->
                    val isDragged = draggedItemIndex == index
                    val offsetY = if (isDragged) dragAccumY else 0f

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, offsetY.toInt()) }
                            .zIndex(if (isDragged) 1f else 0f)
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedItemIndex = index
                                        dragAccumY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragAccumY += dragAmount.y
                                        val currentIdx = draggedItemIndex
                                        if (currentIdx < 0) return@detectDragGesturesAfterLongPress
                                        val targetIndex =
                                            (currentIdx + (dragAccumY / totalItemHeightPx).toInt())
                                                .coerceIn(0, cmdList.size - 1)
                                        if (targetIndex != currentIdx) {
                                            val newList = cmdList.toMutableList()
                                            val item = newList.removeAt(currentIdx)
                                            newList.add(targetIndex, item)
                                            cmdList = newList
                                            dragAccumY -= (targetIndex - currentIdx) * totalItemHeightPx
                                            draggedItemIndex = targetIndex
                                        }
                                    },
                                    onDragEnd = {
                                        draggedItemIndex = -1
                                        dragAccumY = 0f
                                    },
                                    onDragCancel = {
                                        draggedItemIndex = -1
                                        dragAccumY = 0f
                                    }
                                )
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDragged)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDragged) 8.dp else 1.dp
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "#${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                val iconRes = when (cmd.action) {
                                    ScriptCmdBean.ACTION_CLICK -> R.drawable.icon_click
                                    ScriptCmdBean.ACTION_DELAYED -> R.drawable.icon_delay
                                    ScriptCmdBean.ACTION_GESTURE -> R.drawable.icon_gesture
                                    ScriptCmdBean.ACTION_FOR -> R.drawable.icon_for
                                    ScriptCmdBean.ACTION_FOR_END -> R.drawable.icon_for
                                    ScriptCmdBean.ACTION_RANDOM_CLICK -> R.drawable.icon_random
                                    else -> R.drawable.ic_launcher_foreground
                                }
                                Image(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = cmd.info(),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 18.sp
                                )
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = "拖拽排序",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        currentInsertIndex = index
                                        editingCmd = null
                                        showCmdTypeMenu = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,

                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("插入 ↑", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        editingCmd = cmd
                                        currentInsertIndex = index
                                        when (cmd.action) {
                                            ScriptCmdBean.ACTION_CLICK -> showClickDialog = true
                                            ScriptCmdBean.ACTION_DELAYED -> showDelayDialog = true
                                            ScriptCmdBean.ACTION_GESTURE -> showGestureDialog = true
                                            ScriptCmdBean.ACTION_FOR -> showForDialog = true
                                            ScriptCmdBean.ACTION_RANDOM_CLICK -> showRandomClickDialog = true
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,

                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("编辑", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val newList = cmdList.toMutableList()
                                            Collections.swap(newList, index, index - 1)
                                            cmdList = newList
                                        }
                                    },

                                ) {
                                    Icon(Icons.Default.Upload, contentDescription = "上移")
                                }

                                IconButton(
                                    onClick = {
                                        if (index < cmdList.size - 1) {
                                            val newList = cmdList.toMutableList()
                                            Collections.swap(newList, index, index + 1)
                                            cmdList = newList
                                        }
                                    },

                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "下移")
                                }

                                IconButton(
                                    onClick = {
                                        val newList = cmdList.toMutableList()
                                        newList.removeAt(index)
                                        cmdList = newList
                                    },

                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",

                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 命令类型选择菜单
    if (showCmdTypeMenu) {
        AlertDialog(
            onDismissRequest = { showCmdTypeMenu = false },
            title = { Text("选择命令类型", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    cmdTypes.forEachIndexed { index, name ->
                        Surface(
                            onClick = {
                                showCmdTypeMenu = false
                                when (index) {
                                    0 -> showClickDialog = true
                                    1 -> showDelayDialog = true
                                    2 -> showGestureDialog = true
                                    3 -> showForDialog = true
                                    4 -> {
                                        val cmd = ScriptCmdBean.BuildForEndCMD()
                                        val newList = cmdList.toMutableList()
                                        val insertAt =
                                            if (currentInsertIndex >= 0) currentInsertIndex else newList.size
                                        newList.add(insertAt, cmd)
                                        cmdList = newList
                                    }
                                    5 -> showRandomClickDialog = true
                                    6 -> showJsonDialog = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val iconRes = when (index) {
                                    0 -> R.drawable.icon_click
                                    1 -> R.drawable.icon_delay
                                    2 -> R.drawable.icon_gesture
                                    3 -> R.drawable.icon_for
                                    4 -> R.drawable.icon_for
                                    5 -> R.drawable.icon_random
                                    else -> R.drawable.ic_launcher_foreground
                                }
                                Image(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 延时对话框
    if (showDelayDialog) {
        DelayDialog(
            initialDelay = editingCmd?.delayed ?: 0,
            title = if (editingCmd != null) "编辑延时命令" else "延时命令",
            onConfirm = { delay ->
                val cmd = ScriptCmdBean.BuildDelayedCMD(delay)
                val newList = cmdList.toMutableList()
                if (editingCmd != null && currentInsertIndex >= 0) {
                    newList[currentInsertIndex] = cmd
                } else {
                    val insertAt = if (currentInsertIndex >= 0) currentInsertIndex else newList.size
                    newList.add(insertAt, cmd)
                }
                cmdList = newList
                showDelayDialog = false
                editingCmd = null
            },
            onDismiss = { showDelayDialog = false; editingCmd = null }
        )
    }

    // 循环开始对话框
    if (showForDialog) {
        DelayDialog(
            initialDelay = editingCmd?.frequency ?: 1,
            title = if (editingCmd != null) "编辑循环命令" else "循环命令",
            hint = "输入循环次数",
            onConfirm = { frequency ->
                val cmd = ScriptCmdBean.BuildForCMD(frequency)
                val newList = cmdList.toMutableList()
                if (editingCmd != null && currentInsertIndex >= 0) {
                    newList[currentInsertIndex] = cmd
                } else {
                    val insertAt = if (currentInsertIndex >= 0) currentInsertIndex else newList.size
                    newList.add(insertAt, cmd)
                }
                cmdList = newList
                showForDialog = false
                editingCmd = null
            },
            onDismiss = { showForDialog = false; editingCmd = null }
        )
    }

    // 点击命令对话框
    if (showClickDialog) {
        ClickDialog(
            cmd = editingCmd,
            activity = activity,
            onConfirm = { x, y, duration, delay ->
                val cmd = ScriptCmdBean.BuildClickCMD(x, y, duration, delay)
                val newList = cmdList.toMutableList()
                if (editingCmd != null && currentInsertIndex >= 0) {
                    newList[currentInsertIndex] = cmd
                } else {
                    val insertAt = if (currentInsertIndex >= 0) currentInsertIndex else newList.size
                    newList.add(insertAt, cmd)
                }
                cmdList = newList
                activity.dismissWindow()
                showClickDialog = false
                editingCmd = null
            },
            onDismiss = {
                activity.dismissWindow()
                showClickDialog = false
                editingCmd = null
            }
        )
    }

    // 滑屏命令对话框
    if (showGestureDialog) {
        GestureDialog(
            cmd = editingCmd,
            activity = activity,
            onConfirm = { x0, y0, x1, y1, duration, delay ->
                val cmd = ScriptCmdBean.BuildGestureCMD(x0, y0, x1, y1, duration, delay)
                val newList = cmdList.toMutableList()
                if (editingCmd != null && currentInsertIndex >= 0) {
                    newList[currentInsertIndex] = cmd
                } else {
                    val insertAt = if (currentInsertIndex >= 0) currentInsertIndex else newList.size
                    newList.add(insertAt, cmd)
                }
                cmdList = newList
                activity.dismissWindow()
                showGestureDialog = false
                editingCmd = null
            },
            onDismiss = {
                activity.dismissWindow()
                showGestureDialog = false
                editingCmd = null
            }
        )
    }

    // 随机点击对话框
    if (showRandomClickDialog) {
        RandomClickDialog(
            cmd = editingCmd,
            activity = activity,
            onConfirm = { x0, y0, x1, y1, duration, delay ->
                val cmd = ScriptCmdBean.BuildRandomClickCMD(x0, y0, x1, y1, duration, delay)
                val newList = cmdList.toMutableList()
                if (editingCmd != null && currentInsertIndex >= 0) {
                    newList[currentInsertIndex] = cmd
                } else {
                    val insertAt = if (currentInsertIndex >= 0) currentInsertIndex else newList.size
                    newList.add(insertAt, cmd)
                }
                cmdList = newList
                activity.dismissWindow()
                showRandomClickDialog = false
                editingCmd = null
            },
            onDismiss = {
                activity.dismissWindow()
                showRandomClickDialog = false
                editingCmd = null
            }
        )
    }

    // JSON 导入对话框
    if (showJsonDialog) {
        JsonImportDialog(
            onConfirm = { json ->
                try {
                    val list: List<ScriptCmdBean> =
                        Gson().fromJson(json, object : TypeToken<List<ScriptCmdBean>>() {}.type)
                    if (list.isNullOrEmpty()) {
                        Toast.makeText(context, "脚本为空或json格式有问题", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        val newList = cmdList.toMutableList()
                        val insertAt =
                            if (currentInsertIndex >= 0) currentInsertIndex else newList.size
                        newList.addAll(insertAt, list)
                        cmdList = newList
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "脚本为空或json格式有问题", Toast.LENGTH_LONG).show()
                }
                showJsonDialog = false
            },
            onDismiss = { showJsonDialog = false }
        )
    }
}

// ==================== 命令列表项 ====================


// ==================== 延时 / 循环 对话框 ====================

@Composable
fun DelayDialog(
    initialDelay: Int,
    title: String,
    hint: String = "延时时长(单位ms)",
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initialDelay.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(hint)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter { c -> c.isDigit() } },
                    label = { Text(hint) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val d = if (TextUtils.isEmpty(value)) 0 else value.toIntOrNull() ?: 0
                onConfirm(d)
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ==================== 点击命令对话框 ====================

@Composable
fun ClickDialog(
    cmd: ScriptCmdBean?,
    activity: ScriptEditActivityCompose,
    onConfirm: (x: Int, y: Int, duration: Int, delay: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var x by remember { mutableStateOf((cmd?.x0 ?: 0).toString()) }
    var y by remember { mutableStateOf((cmd?.y0 ?: 0).toString()) }
    var duration by remember { mutableStateOf((cmd?.duration ?: 0).toString()) }
    var delay by remember { mutableStateOf((cmd?.delayed ?: 0).toString()) }

    // 坐标拾取回调
    LaunchedEffect(Unit) {
        activity.onCoordinatePicked = { pickedX, pickedY ->
            x = pickedX.toString()
            y = pickedY.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("点击命令") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 坐标获取按钮
                Button(onClick = { activity.alertWindow() }) {
                    Text("坐标获取")
                }
                OutlinedTextField(
                    value = x, onValueChange = { x = it.filter { c -> c.isDigit() || c == '-' } },
                    label = { Text("X") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = y, onValueChange = { y = it.filter { c -> c.isDigit() || c == '-' } },
                    label = { Text("Y") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter { c -> c.isDigit() } },
                    label = { Text("执行时长(ms)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = delay,
                    onValueChange = { delay = it.filter { c -> c.isDigit() } },
                    label = { Text("延时时长(ms)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val xVal = if (TextUtils.isEmpty(x)) 0 else x.toIntOrNull() ?: 0
                val yVal = if (TextUtils.isEmpty(y)) 0 else y.toIntOrNull() ?: 0
                val dVal = if (TextUtils.isEmpty(duration)) 0 else duration.toIntOrNull() ?: 0
                val delayVal = if (TextUtils.isEmpty(delay)) 0 else delay.toIntOrNull() ?: 0
                onConfirm(xVal, yVal, dVal, delayVal)
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ==================== 滑屏命令对话框 ====================

@Composable
fun GestureDialog(
    cmd: ScriptCmdBean?,
    activity: ScriptEditActivityCompose,
    onConfirm: (x0: Int, y0: Int, x1: Int, y1: Int, duration: Int, delay: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var x0 by remember { mutableStateOf((cmd?.x0 ?: 0).toString()) }
    var y0 by remember { mutableStateOf((cmd?.y0 ?: 0).toString()) }
    var x1 by remember { mutableStateOf((cmd?.x1 ?: 0).toString()) }
    var y1 by remember { mutableStateOf((cmd?.y1 ?: 0).toString()) }
    var duration by remember { mutableStateOf((cmd?.duration ?: 1000).toString()) }
    var delay by remember { mutableStateOf((cmd?.delayed ?: 0).toString()) }

    // 当前正在获取哪个坐标: 1=起点, 2=终点, 0=无
    var pickTarget by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        activity.onCoordinatePicked = { pickedX, pickedY ->
            when (pickTarget) {
                1 -> {
                    x0 = pickedX.toString(); y0 = pickedY.toString()
                }

                2 -> {
                    x1 = pickedX.toString(); y1 = pickedY.toString()
                }
            }
            pickTarget = 0
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("滑屏命令") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("起点坐标", fontWeight = FontWeight.Bold)
                Button(onClick = { pickTarget = 1; activity.alertWindow() }) {
                    Text("坐标获取")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = x0,
                        onValueChange = { x0 = it.filter { c -> c.isDigit() || c == '-' } },
                        label = { Text("X") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = y0,
                        onValueChange = { y0 = it.filter { c -> c.isDigit() || c == '-' } },
                        label = { Text("Y") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("终点坐标", fontWeight = FontWeight.Bold)
                Button(onClick = { pickTarget = 2; activity.alertWindow() }) {
                    Text("坐标获取")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = x1,
                        onValueChange = { x1 = it.filter { c -> c.isDigit() || c == '-' } },
                        label = { Text("X") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = y1,
                        onValueChange = { y1 = it.filter { c -> c.isDigit() || c == '-' } },
                        label = { Text("Y") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter { c -> c.isDigit() } },
                    label = { Text("执行时长(ms)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = delay,
                    onValueChange = { delay = it.filter { c -> c.isDigit() } },
                    label = { Text("延时时长(ms)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v0 = { s: String -> if (TextUtils.isEmpty(s)) 0 else s.toIntOrNull() ?: 0 }
                onConfirm(v0(x0), v0(y0), v0(x1), v0(y1), v0(duration), v0(delay))
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ==================== 随机点击对话框 ====================

@Composable
fun RandomClickDialog(
    cmd: ScriptCmdBean?,
    activity: ScriptEditActivityCompose,
    onConfirm: (x0: Int, y0: Int, x1: Int, y1: Int, duration: Int, delay: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var x0 by remember { mutableStateOf((cmd?.x0 ?: 0).toString()) }
    var y0 by remember { mutableStateOf((cmd?.y0 ?: 0).toString()) }
    var x1 by remember { mutableStateOf((cmd?.x1 ?: 0).toString()) }
    var y1 by remember { mutableStateOf((cmd?.y1 ?: 0).toString()) }
    var duration by remember { mutableStateOf((cmd?.duration ?: 0).toString()) }
    var delay by remember { mutableStateOf((cmd?.delayed ?: 0).toString()) }

    var pickTarget by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        activity.onCoordinatePicked = { pickedX, pickedY ->
            when (pickTarget) {
                1 -> {
                    x0 = pickedX.toString(); y0 = pickedY.toString()
                }

                2 -> {
                    x1 = pickedX.toString(); y1 = pickedY.toString()
                }
            }
            pickTarget = 0
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("随机点击") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("坐标一", fontWeight = FontWeight.Bold)
                Button(onClick = { pickTarget = 1; activity.alertWindow() }) {
                    Text("坐标获取")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = x0,
                        onValueChange = { x0 = it.filter { c -> c.isDigit() || c == '-' } },
                        label = { Text("X") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = y0,
                        onValueChange = { y0 = it.filter { c -> c.isDigit() || c == '-' } },
                        label = { Text("Y") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("坐标二", fontWeight = FontWeight.Bold)
                Button(onClick = { pickTarget = 2; activity.alertWindow() }) {
                    Text("坐标获取")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = x1,
                        onValueChange = { x1 = it.filter { c -> c.isDigit() || c == '-' } },
                        label = { Text("X") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = y1,
                        onValueChange = { y1 = it.filter { c -> c.isDigit() || c == '-' } },
                        label = { Text("Y") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter { c -> c.isDigit() } },
                    label = { Text("执行时长(ms)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = delay,
                    onValueChange = { delay = it.filter { c -> c.isDigit() } },
                    label = { Text("延时时长(ms)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v0 = { s: String -> if (TextUtils.isEmpty(s)) 0 else s.toIntOrNull() ?: 0 }
                onConfirm(v0(x0), v0(y0), v0(x1), v0(y1), v0(duration), v0(delay))
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ==================== JSON 导入对话框 ====================

@Composable
fun JsonImportDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var jsonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入脚本") },
        text = {
            OutlinedTextField(
                value = jsonText,
                onValueChange = { jsonText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                label = { Text("请输入 JSON 内容") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(jsonText) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
