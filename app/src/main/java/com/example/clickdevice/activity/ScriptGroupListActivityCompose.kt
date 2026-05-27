package com.example.clickdevice.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.Ohuang.ilivedata.MyLiveData
import com.example.clickdevice.bean.ScriptGroup
import com.example.clickdevice.bean.toScriptGroup
import com.example.clickdevice.db.AppDatabase
import com.example.clickdevice.db.ScriptGroupBean
import com.example.clickdevice.helper.DesktopIconHelper
import com.example.clickdevice.findActivity
import com.example.clickdevice.ui.theme.ClickDeviceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScriptGroupListActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClickDeviceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScriptGroupListScreen(
                        onBack = { finish() },
                        onCreateNew = {
                            ScriptGroupEditActivityCompose.startActivity(this)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptGroupListScreen(
    onBack: () -> Unit,
    onCreateNew: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var scripts by remember { mutableStateOf<List<ScriptGroupBean>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedScript by remember { mutableStateOf<ScriptGroupBean?>(null) }

    val appDatabase = remember { AppDatabase.getInstance(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        appDatabase.scriptGroupDao.loadLiveDataOfAllScriptGroupBean()
            .observe(lifecycleOwner) { list ->
                scripts = list ?: emptyList()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自定义脚本列表") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNew) {
                Icon(Icons.Default.Add, contentDescription = "创建新脚本组")
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
                Text("暂无脚本组，点击右下角创建")
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val toScriptGroup = script.toScriptGroup()
                                MyLiveData.getInstance()
                                    .with("ScriptGroup", ScriptGroup::class.java)
                                    .postValue(toScriptGroup)
                                context.startActivity(
                                    Intent(context, ScriptGroupPlayActivityCompose::class.java)
                                )
                            }
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
                                text = "name: ${script.name ?: "未命名脚本组"}",
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
                                IconButton(onClick = {
                                    ScriptGroupEditActivityCompose.startActivity(context, script.id)
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                                }
                                IconButton(onClick = {
                                    selectedScript = script
                                    showDeleteDialog = true
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除")
                                }
                                IconButton(onClick = {
                                    val activity = context.findActivity()
                                    if (activity != null) {
                                        DesktopIconHelper.addShortcut(activity, script)
                                    }
                                }) {
                                    Icon(Icons.Default.Launch, contentDescription = "桌面快捷方式")
                                }
                            }
                        }
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
                            appDatabase.scriptGroupDao.deleteScriptGroupBean(scriptToDelete)
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

@Preview(showBackground = true)
@Composable
fun ScriptGroupListScreenPreview() {
    ClickDeviceTheme {
        ScriptGroupListScreen(
            onBack = {},
            onCreateNew = {}
        )
    }
}