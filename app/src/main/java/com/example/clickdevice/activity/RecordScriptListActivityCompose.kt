package com.example.clickdevice.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScriptListScreen(
    onBack: () -> Unit,
    onStartRecording: () -> Unit
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
                title = { Text("录制脚本列表") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onStartRecording) {
                Icon(Icons.Default.Add, contentDescription = "开始录制")
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
                Text("暂无录制脚本，点击右下角开始录制")
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
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "id: ${script.id}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "name: ${script.name ?: "未命名录制脚本"}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "创建时间: ${script.createTime ?: ""}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "更新时间: ${script.updateTime ?: ""}",
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onSelect) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "播放")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
                TextButton(onClick = onCreateDesktop) {
                    Text("创建桌面")
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