package com.example.clickdevice.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.Ohuang.ilivedata.MyLiveData
import com.example.clickdevice.db.AppDatabase
import com.example.clickdevice.db.RecordScriptBean
import com.example.clickdevice.helper.DesktopIconHelper
import com.example.clickdevice.ui.theme.ClickDeviceTheme
import kotlinx.coroutines.Dispatchers
import com.example.clickdevice.findActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecordScriptListActivityCompose : ComponentActivity() {
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
                    RecordScriptListScreen(
                        onBack = { finish() },
                        onStartRecording = {
                            startActivity(Intent(this, RecordScriptActivityCompose::class.java))
                        },
                        isSelectMode = isSelectMode,
                        onSelectScript = { script ->
                            selectScript(script)
                        }
                    )
                }
            }
        }
    }

    private fun selectScript(script: RecordScriptBean) {
        val intent = Intent().apply {
            putExtra("type", LauncherScriptActivity.TYPE_RECORD_SCRIPT)
            putExtra("id", script.id)
            putExtra("name", script.name)
        }
        setResult(RESULT_OK, intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScriptListScreen(
    onBack: () -> Unit,
    onStartRecording: () -> Unit,
    isSelectMode: Boolean = false,
    onSelectScript: (RecordScriptBean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var scripts by remember { mutableStateOf<List<RecordScriptBean>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedScript by remember { mutableStateOf<RecordScriptBean?>(null) }

    val appDatabase = remember {
        AppDatabase.getInstance(context)
    }

    LaunchedEffect(Unit) {
        val dao = appDatabase.recordScriptDao
        dao.loadLiveDataOfAllRecordScriptBean().observe(lifecycleOwner) { list ->
            scripts = list ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSelectMode) "选择录制脚本" else "录制脚本列表") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSelectMode) {
                FloatingActionButton(onClick = onStartRecording) {
                    Icon(Icons.Default.Add, contentDescription = "开始录制")
                }
            }
        }
    ) { padding ->
        if (scripts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isSelectMode) "暂无录制脚本" else "暂无录制脚本，点击右下角开始录制")
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
                        RecordScriptSelectItem(
                            script = script,
                            onSelect = { onSelectScript(script) }
                        )
                    } else {
                        RecordScriptItem(
                            script = script,
                            onSelect = {
                                MyLiveData.getInstance().with("RecordScriptPlay", RecordScriptBean::class.java)
                                    .postValue(script)
                                context.startActivity(Intent(context, RecordScriptPlayActivityCompose::class.java))
                            },
                            onEdit = {
                                MyLiveData.getInstance().with("RecordScriptEdit", RecordScriptBean::class.java)
                                    .postValue(script)
                                val intent = Intent(context, RecordScriptActivityCompose::class.java)
                                intent.putExtra("isEdit", true)
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
                item{
                    Spacer(modifier = Modifier.height(100.dp))
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
                            appDatabase.recordScriptDao.deleteRecordScriptBean(scriptToDelete)
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
fun RecordScriptSelectItem(
    script: RecordScriptBean,
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
                        Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(script.name ?: "未命名", style = MaterialTheme.typography.titleMedium)
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
fun RecordScriptItem(
    script: RecordScriptBean,
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
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(script.name ?: "未命名录制脚本", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "更新于 ${script.updateTime ?: ""}",
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
                    Text("回放", style = MaterialTheme.typography.labelLarge)
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
fun RecordScriptListScreenPreview() {
    ClickDeviceTheme {
        RecordScriptListScreen(
            onBack = {},
            onStartRecording = {}
        )
    }
}