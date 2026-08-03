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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AddToHomeScreen
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(script.name ?: "", style = MaterialTheme.typography.titleMedium)
                Text(
                    "ID: ${script.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(script.name ?: "未命名", style = MaterialTheme.typography.titleMedium)
                    Text(
                        script.updateTime ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onCreateDesktop) {
                    Icon(
                        Icons.Default.AddToHomeScreen,
                        contentDescription = "创建桌面图标",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSelect,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("运行", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("编辑", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
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
