package com.example.clickdevice.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.AddToHomeScreen
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
                    ScriptGroupListScreen(
                        onBack = { finish() },
                        onCreateNew = {
                            ScriptGroupEditActivityCompose.startActivity(this)
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

    private fun selectScript(script: ScriptGroupBean) {
        val intent = Intent().apply {
            putExtra("type", LauncherScriptActivity.TYPE_SCRIPT_GROUP)
            putExtra("id", script.id)
            putExtra("name", script.name)
        }
        setResult(RESULT_OK, intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptGroupListScreen(
    onBack: () -> Unit,
    onCreateNew: () -> Unit,
    isSelectMode: Boolean = false,
    onSelectScript: (ScriptGroupBean) -> Unit = {}
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
                title = { Text(if (isSelectMode) "选择自定义脚本" else "自定义脚本列表") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSelectMode) {
                FloatingActionButton(onClick = onCreateNew) {
                    Icon(Icons.Default.Add, contentDescription = "创建新脚本组")
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
                Text(if (isSelectMode) "暂无脚本组" else "暂无脚本组，点击右下角创建")
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
                        ScriptGroupSelectItem(
                            script = script,
                            onSelect = { onSelectScript(script) }
                        )
                    } else {
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
                                                Icons.Default.Widgets,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(script.name ?: "未命名脚本组", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "更新于 ${script.updateTime ?: ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = {
                                        val activity = context.findActivity()
                                        if (activity != null) {
                                            DesktopIconHelper.addShortcut(activity, script)
                                        }
                                    }) {
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
                                        onClick = {
                                            val toScriptGroup = script.toScriptGroup()
                                            MyLiveData.getInstance()
                                                .with("ScriptGroup", ScriptGroup::class.java)
                                                .postValue(toScriptGroup)
                                            context.startActivity(
                                                Intent(context, ScriptGroupPlayActivityCompose::class.java)
                                            )
                                        },
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
                                        onClick = {
                                            ScriptGroupEditActivityCompose.startActivity(context, script.id)
                                        },
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
                                        onClick = {
                                            selectedScript = script
                                            showDeleteDialog = true
                                        },
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

@Composable
fun ScriptGroupSelectItem(
    script: ScriptGroupBean,
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
                        Icons.Default.Widgets,
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