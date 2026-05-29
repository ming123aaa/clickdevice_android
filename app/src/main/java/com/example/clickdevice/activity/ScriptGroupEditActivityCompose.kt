package com.example.clickdevice.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.clickdevice.R
import com.example.clickdevice.SmallWindowView
import com.example.clickdevice.Util
import com.example.clickdevice.bean.ActionScript
import com.example.clickdevice.bean.ScriptCmdBean
import com.example.clickdevice.bean.ScriptGroup
import com.example.clickdevice.bean.SimpleScriptGroup
import com.example.clickdevice.bean.toScriptGroup
import com.example.clickdevice.bean.toScriptGroupBean
import com.example.clickdevice.bean.toSimpleScriptGroup
import com.example.clickdevice.db.AppDatabase
import com.example.clickdevice.db.ScriptGroupBean
import com.example.clickdevice.helper.smallWindowManager
import com.example.clickdevice.ui.theme.ClickDeviceTheme
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList
import java.util.TreeMap

class ScriptGroupEditActivityCompose : ComponentActivity() {

    private val scriptGroupId by lazy { intent.getIntExtra("scriptGroupId", 0) }

    private var windowView: SmallWindowView? = null
    private var btnWindowView: SmallWindowView? = null
    private var tvBtnWv: TextView? = null
    private var wm: WindowManager? = null
    private var mLayoutParams: WindowManager.LayoutParams? = null
    private var btnLayoutParams: WindowManager.LayoutParams? = null

    var onCoordinatePicked: ((x: Int, y: Int) -> Unit)? = null

    companion object {
        fun startActivity(context: Context, scriptGroupId: Int = 0) {
            val intent = Intent(context, ScriptGroupEditActivityCompose::class.java)
            intent.putExtra("scriptGroupId", scriptGroupId)
            context.startActivity(intent)
        }
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
                    ScriptGroupEditNavScreen(
                        scriptGroupId = scriptGroupId,
                        activity = this,
                        onBack = { finish() },
                        onComplete = { finish() }
                    )
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    private fun initSmallViewLayout() {
        wm =  smallWindowManager()
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
    }

    private fun initBtnWindow() {
        btnWindowView = LayoutInflater.from(this).inflate(R.layout.window_b, null) as SmallWindowView
        btnLayoutParams = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, 8, PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnLayoutParams?.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        btnLayoutParams?.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        btnWindowView?.enableMove=false
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
                Toast.makeText(this, "can not DrawOverlays", Toast.LENGTH_LONG).show()
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
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

sealed class ScriptGroupPage {
    object Main : ScriptGroupPage()
    object ScriptList : ScriptGroupPage()
    data class ScriptEdit(val index: Int) : ScriptGroupPage()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptGroupEditNavScreen(
    scriptGroupId: Int,
    activity: ScriptGroupEditActivityCompose? = null,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var scriptGroup by remember {
        mutableStateOf(
            ScriptGroup(name = "", actionMap = TreeMap(), actionScript = emptyList())
        )
    }
    var title by remember { mutableStateOf("") }
    var currentPage by remember { mutableStateOf<ScriptGroupPage>(ScriptGroupPage.Main) }
    var editingScriptBean: ScriptGroupBean? by remember { mutableStateOf(null) }

    LaunchedEffect(scriptGroupId) {
        withContext(Dispatchers.IO) {
            if (scriptGroupId > 0) {
                val bean = AppDatabase.getInstance(context).scriptGroupDao.findBeanById(scriptGroupId)
                bean?.let {
                    editingScriptBean = it
                    scriptGroup = it.toScriptGroup()
                    title = it.name ?: ""
                }
            }
        }
    }

    when (val page = currentPage) {
        is ScriptGroupPage.Main -> {
            ScriptGroupEditMainPage(
                title = title,
                onTitleChange = { title = it },
                scriptGroup = scriptGroup,
                onScriptGroupChange = { scriptGroup = it },
                activity = activity,
                onBack = onBack,
                onComplete = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "请输入脚本组名称", Toast.LENGTH_SHORT).show()
                        return@ScriptGroupEditMainPage
                    }
                    val validXCoeff = if (scriptGroup.xCoefficient in 0.25f..5.0f) scriptGroup.xCoefficient else 1.0f
                    val validYCoeff = if (scriptGroup.yCoefficient in 0.25f..5.0f) scriptGroup.yCoefficient else 1.0f
                    if (validXCoeff != scriptGroup.xCoefficient || validYCoeff != scriptGroup.yCoefficient) {
                        Toast.makeText(context, "坐标系数超出范围(0.25~5)，已自动设为1", Toast.LENGTH_SHORT).show()
                    }
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            val data = scriptGroup.copy(name = title, xCoefficient = validXCoeff, yCoefficient = validYCoeff)
                            val appDatabase = AppDatabase.getInstance(context)
                            val newData = if (editingScriptBean != null) {
                                data.toScriptGroupBean(editingScriptBean!!)
                            } else {
                                data.toScriptGroupBean()
                            }
                            if (scriptGroupId == 0) {
                                appDatabase.scriptGroupDao.insertScriptGroupBean(newData)
                            } else {
                                appDatabase.scriptGroupDao.updateScriptGroupBean(newData)
                            }
                        }
                        onComplete()
                    }
                },
                onShowScriptList = { currentPage = ScriptGroupPage.ScriptList },
                onExportJson = {
                    val simpleGroup = scriptGroup.copy(name = title).toSimpleScriptGroup()
                    val json = Gson().toJson(simpleGroup)
                    try {
                        Util.copyText(json, context)
                        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_LONG).show()
                    } catch (_: Exception) {}
                },
                onImportJson = { json ->
                    try {
                        if (json.isEmpty()) {
                            Toast.makeText(context, "数据为空", Toast.LENGTH_LONG).show()
                            return@ScriptGroupEditMainPage
                        }
                        val fromJson = Gson().fromJson(json, SimpleScriptGroup::class.java)
                        if (fromJson != null) {
                            scriptGroup = scriptGroup.copy(
                                name = fromJson.name,
                                actionMap = fromJson.actionMap
                            )
                            title = fromJson.name
                        } else {
                            Toast.makeText(context, "数据格式不正确", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "导入数据失败", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        is ScriptGroupPage.ScriptList -> {
            ScriptGroupScriptListPage(
                scriptGroup = scriptGroup,
                onBack = { currentPage = ScriptGroupPage.Main },
                onAddScript = {
                    val newAction = ActionScript(name = "", script = ArrayList())
                    val newList = ArrayList(scriptGroup.actionScript)
                    newList.add(newAction)
                    scriptGroup = scriptGroup.copy(actionScript = newList)
                    currentPage = ScriptGroupPage.ScriptEdit(newList.size - 1)
                },
                onEditScript = { index ->
                    currentPage = ScriptGroupPage.ScriptEdit(index)
                },
                onDeleteScript = { index ->
                    val newList = ArrayList(scriptGroup.actionScript)
                    newList.removeAt(index)
                    scriptGroup = scriptGroup.copy(actionScript = newList)
                }
            )
        }

        is ScriptGroupPage.ScriptEdit -> {
            val actionIndex = page.index
            val actionScript = scriptGroup.actionScript.getOrNull(actionIndex)
            if (actionScript != null) {
                ScriptGroupScriptEditPage(
                    actionScript = actionScript,
                    scriptGroup = scriptGroup,
                    onBack = { currentPage = ScriptGroupPage.ScriptList },
                    onUpdate = { updatedScript ->
                        val newList = ArrayList(scriptGroup.actionScript)
                        newList[actionIndex] = updatedScript
                        scriptGroup = scriptGroup.copy(actionScript = newList)
                    },
                    onExportJson = {
                        val json = Gson().toJson(actionScript)
                        try {
                            Util.copyText(json, context)
                            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_LONG).show()
                        } catch (_: Exception) {}
                    },
                    onImportJson = { json ->
                        try {
                            if (json.isEmpty()) {
                                Toast.makeText(context, "数据为空", Toast.LENGTH_LONG).show()
                                return@ScriptGroupScriptEditPage
                            }
                            val fromJson = Gson().fromJson(json, ActionScript::class.java)
                            if (fromJson != null) {
                                val newList = ArrayList(scriptGroup.actionScript)
                                newList[actionIndex] = fromJson
                                scriptGroup = scriptGroup.copy(actionScript = newList)
                            } else {
                                Toast.makeText(context, "数据格式不正确", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "导入数据失败", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptGroupEditMainPage(
    title: String,
    onTitleChange: (String) -> Unit,
    scriptGroup: ScriptGroup,
    onScriptGroupChange: (ScriptGroup) -> Unit,
    activity: ScriptGroupEditActivityCompose? = null,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onShowScriptList: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: (String) -> Unit
) {
    val context = LocalContext.current
    var showInsertCmdDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showBackDialog by remember { mutableStateOf(false) }
    var editingCmdName by remember { mutableStateOf("") }
    var editingCmdBean by remember { mutableStateOf(ScriptCmdBean.BuildNoneCMD()) }
    var editingCmdIndex by remember { mutableIntStateOf(-1) }
    var showCmdTypeDialog by remember { mutableStateOf(false) }
    var xCoeffText by remember { mutableStateOf("") }
    var yCoeffText by remember { mutableStateOf("") }
    var coeffLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(scriptGroup.xCoefficient, scriptGroup.yCoefficient) {
        if (!coeffLoaded) {
            xCoeffText = if (scriptGroup.xCoefficient == 1.0f) "" else scriptGroup.xCoefficient.toString()
            yCoeffText = if (scriptGroup.yCoefficient == 1.0f) "" else scriptGroup.yCoefficient.toString()
            coeffLoaded = true
        }
    }

    BackHandler {
        showBackDialog = true
    }

    if (showBackDialog) {
        AlertDialog(
            onDismissRequest = { showBackDialog = false },
            title = { Text("提示") },
            text = { Text("是否保存当前脚本组？") },
            confirmButton = {
                TextButton(onClick = {
                    showBackDialog = false
                    onComplete()
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackDialog = false
                    onBack()
                }) {
                    Text("不保存")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑脚本组") },
                navigationIcon = {
                    IconButton(onClick = { showBackDialog = true }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onComplete) {
                        Text("完成")
                    }
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
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("脚本组名称") }
            )

            Text("坐标系数", style = MaterialTheme.typography.titleMedium)
            Text(
                "用于适配不同分辨率设备，实际坐标=原始坐标×系数。取值范围0.25~5，超出范围自动设为1",
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
                            if (parsed != null) {
                                onScriptGroupChange(scriptGroup.copy(xCoefficient = parsed))
                            } else if (value.isEmpty()) {
                                onScriptGroupChange(scriptGroup.copy(xCoefficient = 1.0f))
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
                                onScriptGroupChange(scriptGroup.copy(yCoefficient = parsed))
                            } else if (value.isEmpty()) {
                                onScriptGroupChange(scriptGroup.copy(yCoefficient = 1.0f))
                            }
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
                Button(onClick = onShowScriptList, modifier = Modifier.weight(1f)) {
                    Text("脚本列表")
                }
                Button(onClick = onExportJson, modifier = Modifier.weight(1f)) {
                    Text("导出JSON")
                }
                Button(onClick = { showImportDialog = true }, modifier = Modifier.weight(1f)) {
                    Text("导入JSON")
                }
            }

            Text("命令列表", style = MaterialTheme.typography.titleMedium)

            if (scriptGroup.actionMap.isEmpty()) {
                Text(
                    text = "暂无命令，请在下方添加",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            scriptGroup.actionMap.entries.forEach { (name, cmd) ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = cmd.content ?: cmd.actionTypeName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            editingCmdName = name
                            editingCmdBean = cmd
                            editingCmdIndex = -2
                            showInsertCmdDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                        IconButton(onClick = {
                            val treeMap = TreeMap(scriptGroup.actionMap)
                            treeMap.remove(name)
                            onScriptGroupChange(scriptGroup.copy(actionMap = treeMap))
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    editingCmdName = ""
                    editingCmdBean = ScriptCmdBean.BuildNoneCMD()
                    editingCmdIndex = -1
                    showInsertCmdDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("插入命令")
            }
        }
    }

    if (showInsertCmdDialog) {
        AlertDialog(
            onDismissRequest = { showInsertCmdDialog = false },
            title = { Text("添加/编辑命令") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editingCmdName,
                        onValueChange = { editingCmdName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("命令名称") }
                    )
                    Text(
                        text = "命令类型: ${editingCmdBean.actionTypeName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { showCmdTypeDialog = true }) {
                        Text("选择命令类型")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editingCmdName.isNotBlank()) {
                        val treeMap = TreeMap(scriptGroup.actionMap)
                        treeMap[editingCmdName] = editingCmdBean
                        onScriptGroupChange(scriptGroup.copy(actionMap = treeMap))
                        showInsertCmdDialog = false
                    } else {
                        Toast.makeText(context, "请输入命令名称", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInsertCmdDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showCmdTypeDialog) {
        CmdTypeSelectDialog(
            currentCmd = editingCmdBean,
            activity = activity,
            onSelect = { cmd ->
                editingCmdBean = cmd
                showCmdTypeDialog = false
            },
            onDismiss = { showCmdTypeDialog = false }
        )
    }

    if (showImportDialog) {
        var importText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入数据") },
            text = {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("粘贴JSON数据") },
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onImportJson(importText)
                    showImportDialog = false
                }) {
                    Text("导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptGroupScriptListPage(
    scriptGroup: ScriptGroup,
    onBack: () -> Unit,
    onAddScript: () -> Unit,
    onEditScript: (Int) -> Unit,
    onDeleteScript: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("脚本列表") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onAddScript) {
                        Icon(Icons.Default.Add, contentDescription = "添加脚本")
                    }
                }
            )
        }
    ) { padding ->
        if (scriptGroup.actionScript.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无脚本，点击右上角添加")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                itemsIndexed(scriptGroup.actionScript) { index, actionScript ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditScript(index) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = actionScript.name.ifEmpty { "未命名脚本 ${index + 1}" },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "命令数: ${actionScript.script.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onEditScript(index) }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                            }
                            IconButton(onClick = { onDeleteScript(index) }) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptGroupScriptEditPage(
    actionScript: ActionScript,
    scriptGroup: ScriptGroup,
    onBack: () -> Unit,
    onUpdate: (ActionScript) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: (String) -> Unit
) {
    var scriptName by remember { mutableStateOf(actionScript.name) }
    var scriptKeys by remember { mutableStateOf(ArrayList(actionScript.script)) }
    var showInsertKeyDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var editingKeyIndex by remember { mutableIntStateOf(-1) }
    var editingKeyValue by remember { mutableStateOf("") }
    var showCmdSelectDialog by remember { mutableStateOf(false) }

    fun updateAndSync() {
        val updated = ActionScript(name = scriptName, script = scriptKeys)
        onUpdate(updated)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑脚本") },
                navigationIcon = {
                    IconButton(onClick = {
                        updateAndSync()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onExportJson) {
                        Text("导出")
                    }
                    TextButton(onClick = { showImportDialog = true }) {
                        Text("导入")
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
                onValueChange = {
                    scriptName = it
                    updateAndSync()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("脚本名称") }
            )

            Text("命令列表", style = MaterialTheme.typography.titleMedium)

            if (scriptKeys.isEmpty()) {
                Text(
                    text = "暂无命令，请在下方添加",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(scriptKeys) { index, key ->
                    val cmdMsg = scriptGroup.getScriptCmdBean(key)?.actionTypeName ?: "没有匹配到命令"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingKeyIndex = index
                                editingKeyValue = key
                                showCmdSelectDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 1}. $key",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = cmdMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                editingKeyIndex = index
                                editingKeyValue = key
                                showCmdSelectDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                            }
                            IconButton(onClick = {
                                scriptKeys = ArrayList(scriptKeys).apply { removeAt(index) }
                                updateAndSync()
                            }) {
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

            Button(
                onClick = {
                    editingKeyIndex = -1
                    editingKeyValue = ""
                    showCmdSelectDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("插入命令")
            }
        }
    }

    if (showCmdSelectDialog) {
        CmdKeySelectDialog(
            scriptGroup = scriptGroup,
            currentKey = editingKeyValue,
            onSelect = { key ->
                if (editingKeyIndex >= 0) {
                    scriptKeys = ArrayList(scriptKeys).apply { set(editingKeyIndex, key) }
                } else {
                    scriptKeys = ArrayList(scriptKeys).apply { add(key) }
                }
                updateAndSync()
                showCmdSelectDialog = false
            },
            onDismiss = { showCmdSelectDialog = false }
        )
    }

    if (showImportDialog) {
        var importText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入数据") },
            text = {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("粘贴ActionScript JSON") },
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onImportJson(importText)
                    showImportDialog = false
                }) {
                    Text("导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun CmdTypeSelectDialog(
    currentCmd: ScriptCmdBean,
    activity: ScriptGroupEditActivityCompose?,
    onSelect: (ScriptCmdBean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableIntStateOf(currentCmd.action) }
    var delayValue by remember { mutableStateOf(currentCmd.delayed.toString()) }
    var clickX by remember { mutableStateOf(currentCmd.x0.toString()) }
    var clickY by remember { mutableStateOf(currentCmd.y0.toString()) }
    var clickDuration by remember { mutableStateOf(currentCmd.duration.toString()) }
    var gestureX1 by remember { mutableStateOf(currentCmd.x1.toString()) }
    var gestureY1 by remember { mutableStateOf(currentCmd.y1.toString()) }
    var forFrequency by remember { mutableStateOf(currentCmd.frequency.toString()) }
    var pickTarget by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        activity?.onCoordinatePicked = { pickedX, pickedY ->
            when (pickTarget) {
                1 -> { clickX = pickedX.toString(); clickY = pickedY.toString() }
                2 -> { gestureX1 = pickedX.toString(); gestureY1 = pickedY.toString() }
            }
            pickTarget = 0
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择命令类型") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf(
                    ScriptCmdBean.ACTION_NONE to "无命令",
                    ScriptCmdBean.ACTION_DELAYED to "延时",
                    ScriptCmdBean.ACTION_CLICK to "点击",
                    ScriptCmdBean.ACTION_GESTURE to "手势",
                    ScriptCmdBean.ACTION_FOR to "循环开始",
                    ScriptCmdBean.ACTION_FOR_END to "循环结束",
                    ScriptCmdBean.ACTION_RANDOM_CLICK to "随机位置点击"
                )
                types.forEach { (type, name) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Text(name, modifier = Modifier.clickable { selectedType = type })
                    }
                }

                Divider()

                when (selectedType) {
                    ScriptCmdBean.ACTION_DELAYED -> {
                        OutlinedTextField(
                            value = delayValue,
                            onValueChange = { delayValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("延时(ms)") }
                        )
                    }
                    ScriptCmdBean.ACTION_CLICK -> {
                        Button(onClick = { pickTarget = 1; activity?.alertWindow() }) {
                            Text("坐标获取")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = clickX,
                                onValueChange = { clickX = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("X") }
                            )
                            OutlinedTextField(
                                value = clickY,
                                onValueChange = { clickY = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Y") }
                            )
                        }
                        OutlinedTextField(
                            value = clickDuration,
                            onValueChange = { clickDuration = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("执行时长(ms)") }
                        )
                        OutlinedTextField(
                            value = delayValue,
                            onValueChange = { delayValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("延时(ms)") }
                        )
                    }
                    ScriptCmdBean.ACTION_GESTURE, ScriptCmdBean.ACTION_RANDOM_CLICK -> {
                        Text("起点坐标", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { pickTarget = 1; activity?.alertWindow() }) {
                            Text("坐标获取")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = clickX,
                                onValueChange = { clickX = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("X0") }
                            )
                            OutlinedTextField(
                                value = clickY,
                                onValueChange = { clickY = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Y0") }
                            )
                        }
                        Text("终点坐标", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { pickTarget = 2; activity?.alertWindow() }) {
                            Text("坐标获取")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = gestureX1,
                                onValueChange = { gestureX1 = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("X1") }
                            )
                            OutlinedTextField(
                                value = gestureY1,
                                onValueChange = { gestureY1 = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Y1") }
                            )
                        }
                        OutlinedTextField(
                            value = clickDuration,
                            onValueChange = { clickDuration = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("执行时长(ms)") }
                        )
                        OutlinedTextField(
                            value = delayValue,
                            onValueChange = { delayValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("延时(ms)") }
                        )
                    }
                    ScriptCmdBean.ACTION_FOR -> {
                        OutlinedTextField(
                            value = forFrequency,
                            onValueChange = { forFrequency = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("循环次数") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cmd = when (selectedType) {
                    ScriptCmdBean.ACTION_NONE -> ScriptCmdBean.BuildNoneCMD()
                    ScriptCmdBean.ACTION_DELAYED -> {
                        val delay = delayValue.toIntOrNull() ?: 0
                        ScriptCmdBean.BuildDelayedCMD(delay)
                    }
                    ScriptCmdBean.ACTION_CLICK -> {
                        val x = clickX.toIntOrNull() ?: 0
                        val y = clickY.toIntOrNull() ?: 0
                        val dur = clickDuration.toIntOrNull() ?: 0
                        val delay = delayValue.toIntOrNull() ?: 0
                        ScriptCmdBean.BuildClickCMD(x, y, dur, delay)
                    }
                    ScriptCmdBean.ACTION_GESTURE -> {
                        val x0 = clickX.toIntOrNull() ?: 0
                        val y0 = clickY.toIntOrNull() ?: 0
                        val x1 = gestureX1.toIntOrNull() ?: 0
                        val y1 = gestureY1.toIntOrNull() ?: 0
                        val dur = clickDuration.toIntOrNull() ?: 0
                        val delay = delayValue.toIntOrNull() ?: 0
                        ScriptCmdBean.BuildGestureCMD(x0, y0, x1, y1, dur, delay)
                    }
                    ScriptCmdBean.ACTION_FOR -> {
                        val freq = forFrequency.toIntOrNull() ?: 1
                        ScriptCmdBean.BuildForCMD(freq)
                    }
                    ScriptCmdBean.ACTION_FOR_END -> ScriptCmdBean.BuildForEndCMD()
                    ScriptCmdBean.ACTION_RANDOM_CLICK -> {
                        val x0 = clickX.toIntOrNull() ?: 0
                        val y0 = clickY.toIntOrNull() ?: 0
                        val x1 = gestureX1.toIntOrNull() ?: 0
                        val y1 = gestureY1.toIntOrNull() ?: 0
                        val dur = clickDuration.toIntOrNull() ?: 0
                        val delay = delayValue.toIntOrNull() ?: 0
                        ScriptCmdBean.BuildRandomClickCMD(x0, y0, x1, y1, dur, delay)
                    }
                    else -> ScriptCmdBean.BuildNoneCMD()
                }
                onSelect(cmd)
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun CmdKeySelectDialog(
    scriptGroup: ScriptGroup,
    currentKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择命令") },
        text = {
            if (scriptGroup.actionMap.isEmpty()) {
                Text("没有可用的命令，请先在主页添加命令")
            } else {
                LazyColumn {
                    itemsIndexed(scriptGroup.actionMap.entries.toList()) { _, (name, cmd) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelect(name) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = cmd.actionTypeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ScriptGroupEditNavScreenPreview() {
    ClickDeviceTheme {
        ScriptGroupEditNavScreen(
            scriptGroupId = 0,
            onBack = {},
            onComplete = {}
        )
    }
}
