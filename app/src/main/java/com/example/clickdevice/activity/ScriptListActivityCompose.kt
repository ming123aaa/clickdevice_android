package com.example.clickdevice.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Ohuang.ilivedata.MyLiveData
import com.example.clickdevice.MyApp
import com.example.clickdevice.findActivity
import com.example.clickdevice.db.AppDatabase
import com.example.clickdevice.db.ScriptDataBean
import com.example.clickdevice.helper.DesktopIconHelper
import com.example.clickdevice.ui.theme.ClickDeviceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScriptListActivityCompose : ComponentActivity() {
    private var isSelectMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isSelectMode = intent.getBooleanExtra("selectMode", false)
        
        setContent {
            ClickDeviceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScriptListScreen(
                        onBack = { finish() },
                        onCreateNew = { createNewScript() },
                        isSelectMode = isSelectMode,
                        onSelectScript = { script ->
                            selectScript(script)
                        }
                    )
                }
            }
        }
    }

    private fun createNewScript() {
        val intent = Intent(this, ScriptEditActivityCompose::class.java)
        intent.putExtra("isNew", true)
        startActivity(intent)
    }

    private fun selectScript(script: ScriptDataBean) {
        val intent = Intent().apply {
            putExtra("type", LauncherScriptActivity.TYPE_SCRIPT)
            putExtra("id", script.id)
            putExtra("name", script.name)
        }
        setResult(RESULT_OK, intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptListScreen(
    onBack: () -> Unit,
    onCreateNew: () -> Unit,
    isSelectMode: Boolean = false,
    onSelectScript: (ScriptDataBean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var scripts by remember { mutableStateOf<List<ScriptDataBean>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedScript by remember { mutableStateOf<ScriptDataBean?>(null) }

    val appDatabase = remember {
        (context.applicationContext as MyApp).appDatabase
    }

    LaunchedEffect(Unit) {
        val dao = appDatabase.getScriptDao()
        dao.loadLiveDataOfAllScriptDataBean().observe(lifecycleOwner) { list ->
            scripts = list ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSelectMode) "选择普通脚本" else "普通脚本列表") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!isSelectMode) {
                        IconButton(onClick = onCreateNew) {
                            Icon(Icons.Default.Add, contentDescription = "创建新脚本")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (scripts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isSelectMode) "暂无脚本" else "暂无脚本，点击右上角 + 创建")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                items(scripts) { script ->
                    if (isSelectMode) {
                        ScriptSelectItem(
                            script = script,
                            onSelect = { onSelectScript(script) }
                        )
                    } else {
                        ScriptItem(
                            script = script,
                            onSelect = {
                                MyLiveData.getInstance().with("json", String::class.java).setValue(script.scriptJson)
                                MyLiveData.getInstance().with("scriptName", String::class.java).setValue(script.name)
                                MyLiveData.getInstance().with("xCoefficient", Float::class.java).setValue(script.getXCoefficient())
                                MyLiveData.getInstance().with("yCoefficient", Float::class.java).setValue(script.getYCoefficient())
                                context.startActivity(Intent(context, ScriptActivityCompose::class.java))
                            },
                            onEdit = {
                                val intent = Intent(context, ScriptEditActivityCompose::class.java)
                                intent.putExtra("isNew", false)
                                MyLiveData.getInstance().with("ScriptDataBean", ScriptDataBean::class.java).setValue(script)
                                context.startActivity(intent)
                            },
                            onDelete = {
                                selectedScript = script
                                showDeleteDialog = true
                            },
                            onCreateDesktop = {
                                val activity = context.findActivity()
                                if (activity != null) {
                                    DesktopIconHelper.addShortcut(activity, script)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog && selectedScript != null) {
        val scriptToDelete = selectedScript!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除脚本") },
            text = { Text("你确定要删除${scriptToDelete.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            appDatabase.getScriptDao().deleteScriptDataBean(scriptToDelete)
                        }
                    }
                    showDeleteDialog = false
                }) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ScriptSelectItem(
    script: ScriptDataBean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(script.name ?: "", style = MaterialTheme.typography.titleMedium)
            Text("ID: ${script.id}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun ScriptItem(
    script: ScriptDataBean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCreateDesktop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp)
    ) {
        // 创建桌面图标（右上角）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "创建桌面图标",
                color = Color(0xFFAA0000),
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable { onCreateDesktop() }
                    .padding(4.dp)
            )
        }

        // 脚本信息
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text(
                text = "id:",
                modifier = Modifier.width(70.dp),
                fontSize = 13.sp
            )
            Text(
                text = script.stringId,
                fontSize = 13.sp
            )
        }

        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text(
                text = "name:",
                modifier = Modifier.width(70.dp),
                fontSize = 13.sp
            )
            Text(
                text = script.name ?: "",
                fontSize = 13.sp
            )
        }

        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text(
                text = "创建时间：",
                modifier = Modifier.width(70.dp),
                fontSize = 13.sp
            )
            Text(
                text = script.createTime ?: "",
                fontSize = 13.sp
            )
        }

        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text(
                text = "更新时间：",
                modifier = Modifier.width(70.dp),
                fontSize = 13.sp
            )
            Text(
                text = script.updateTime ?: "",
                fontSize = 13.sp
            )
        }

        // 操作按钮：删除、编辑、选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f).height(30.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAA0000)),
                border = BorderStroke(1.dp, Color.Gray),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("删除", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f).height(30.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00AA00)),
                border = BorderStroke(1.dp, Color.Gray),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("编辑", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onSelect,
                modifier = Modifier.weight(1f).height(30.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0044AA)),
                border = BorderStroke(1.dp, Color.Gray),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("选择", fontSize = 13.sp)
            }
        }

        // 分割线
       Divider(
            modifier = Modifier.padding(top = 4.dp),
            thickness = 1.dp,
            color = Color(0xFFCCCCCC)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScriptListScreenPreview() {
    ClickDeviceTheme {
        ScriptListScreen(
            onBack = {},
            onCreateNew = {}
        )
    }
}
