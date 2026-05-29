package com.example.clickdevice.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clickdevice.db.AppDatabase
import com.example.clickdevice.db.KeyBindingBean
import com.example.clickdevice.helper.DesktopIconHelper
import com.example.clickdevice.helper.KeyFloatWindowManager
import com.example.clickdevice.ui.theme.ClickDeviceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.clickdevice.PowerKeyObserver
import com.example.clickdevice.findActivity
import com.example.clickdevice.helper.onClick
import com.example.clickdevice.ui.theme.md_theme_dark_inverseSurface


@OptIn(ExperimentalMaterial3Api::class)
class KeyBindingListActivity : AppCompatActivity() {
    val keyFloatWindowManager: KeyFloatWindowManager by lazy {
        KeyFloatWindowManager(this)
    }

    private var mPowerKeyObserver: PowerKeyObserver? = null

    private var isJumpEdit = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPowerKeyObserver = PowerKeyObserver(this).apply {
            startListen()
            setHomeKeyListener {
                keyFloatWindowManager.stopRunningScript()
            }
        }

        keyFloatWindowManager.restoreWindows()

        setContent {
            ClickDeviceTheme {

                KeyBindingListScreen(
                    keyFloatWindowManager = keyFloatWindowManager,
                    onAddKey = {
                        val intent = Intent(this, KeyBindingEditActivity::class.java)
                        intent.putExtra("isNew", true)
                        keyFloatWindowManager.hideAllWindows()
                        startActivity(intent)
                        isJumpEdit = true
                    },
                    onEditKey = { binding ->
                        val intent = Intent(this, KeyBindingEditActivity::class.java)
                        keyFloatWindowManager.hideAllWindows()
                        intent.putExtra("isNew", false)
                        intent.putExtra("id", binding.id)
                        startActivity(intent)
                        isJumpEdit = true
                    },
                    onDeleteKey = { binding ->
                        deleteKeyBinding(binding)
                    },
                    onToggleEnable = { binding ->
                        toggleKeyBindingEnable(binding)
                    }
                )
            }
        }
    }

    private fun deleteKeyBinding(binding: KeyBindingBean) {
        val context = this
        android.app.AlertDialog.Builder(context)
            .setTitle("删除按键")
            .setMessage("确定删除按键 \"${binding.keyName}\" 吗？")
            .setPositiveButton("确定") { _, _ ->
                GlobalScope.launch(Dispatchers.IO) {
                    val db = AppDatabase.getInstance(context)
                    db.getKeyBindingDao().deleteKeyBindingBean(binding)
                    keyFloatWindowManager.hideWindow(binding.id)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun toggleKeyBindingEnable(binding: KeyBindingBean) {


        if (!keyFloatWindowManager.isShow(binding.id)) {
            keyFloatWindowManager.showWindow(binding)
        } else {
            keyFloatWindowManager.hideWindow(binding.id)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isJumpEdit) {
            keyFloatWindowManager.restoreWindows()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mPowerKeyObserver?.stopListen()
        keyFloatWindowManager.destroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyBindingListScreen(
    keyFloatWindowManager: KeyFloatWindowManager = KeyFloatWindowManager(LocalContext.current),
    onAddKey: () -> Unit,
    onEditKey: (KeyBindingBean) -> Unit,
    onDeleteKey: (KeyBindingBean) -> Unit,
    onToggleEnable: (KeyBindingBean) -> Unit,
) {
    val context = LocalContext.current
    var keyBindings by remember { mutableStateOf<List<KeyBindingBean>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedBinding by remember { mutableStateOf<KeyBindingBean?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val db = AppDatabase.getInstance(context)
        db.getKeyBindingDao().loadLiveDataOfAllKeyBindingBean().observe(lifecycleOwner) { list ->
            keyBindings = list
        }
    }

    fun handleDelete() {
        selectedBinding?.let {
            onDeleteKey(it)
        }
        showDeleteDialog = false
        selectedBinding = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("按键设置", fontWeight = FontWeight.Bold) },
                actions = {
                    Text(
                        "创建桌面", color = MaterialTheme.colorScheme.primary,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clickable {
                                val activity = context.findActivity()
                                if (activity != null) {
                                    DesktopIconHelper.addKeyListShortcut(activity)
                                }
                            })

                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddKey,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("添加按键") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        if (keyBindings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "暂无按键",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "点击右下角按钮添加",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                items(keyBindings) { binding ->
                    KeyBindingItem(
                        keyFloatWindowManager = keyFloatWindowManager,
                        binding = binding,
                        onEdit = { onEditKey(binding) },
                        onDelete = {
                            selectedBinding = binding
                            showDeleteDialog = true
                        },
                        onToggleEnable = { onToggleEnable(binding) }
                    )
                }
            }
        }
    }

    if (showDeleteDialog && selectedBinding != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除按键") },
            text = { Text("确定删除按键 \"${selectedBinding!!.keyName}\" 吗？") },
            confirmButton = {
                TextButton(onClick = ::handleDelete) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    selectedBinding = null
                }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun KeyBindingItem(
    keyFloatWindowManager: KeyFloatWindowManager,
    binding: KeyBindingBean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnable: () -> Unit
) {
    val context = LocalContext.current
    val isShowing = keyFloatWindowManager.isShow(binding.id)
    var isLocked by remember { mutableStateOf(binding.windowLocked) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isShowing) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = binding.keyName.take(1),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isShowing) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = binding.keyName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (binding.keyDescription.isNotEmpty()) {
                        Text(
                            text = binding.keyDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(
                        checked = isShowing,
                        onCheckedChange = { onToggleEnable() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    Text(
                        text = if (isShowing) "显示中" else "已隐藏",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isShowing) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = getKeyScriptTypeName(binding.scriptType),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = binding.scriptName ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "ID: ${binding.scriptId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (isShowing) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLocked) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isLocked) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = if (isLocked) "悬浮窗已锁定" else "悬浮窗可拖动",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLocked) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isLocked,
                            onCheckedChange = { locked ->
                                isLocked = locked
                                val xy = keyFloatWindowManager.getWindowXY(binding.id)
                                binding.windowX = xy[0]
                                binding.windowY = xy[1]
                                binding.windowLocked = locked
                                keyFloatWindowManager.setWindowMoveEnable(binding.id, !locked)

                                GlobalScope.launch(Dispatchers.IO) {
                                    val db = AppDatabase.getInstance(context)
                                    db.getKeyBindingDao().insertKeyBindingBean(binding)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("编辑")
                }
                TextButton(
                    onClick = onDelete,
                    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

fun getKeyScriptTypeName(type: String?): String {
    return when (type) {
        LauncherScriptActivity.TYPE_SCRIPT -> "普通脚本"
        LauncherScriptActivity.TYPE_RECORD_SCRIPT -> "录制脚本"
        LauncherScriptActivity.TYPE_SCRIPT_GROUP -> "自定义脚本"
        else -> "未知"
    }
}

@Preview(showBackground = true)
@Composable
fun KeyBindingListPreview() {
    ClickDeviceTheme {
        KeyBindingListScreen(
            onAddKey = {},
            onEditKey = {},
            onDeleteKey = {},
            onToggleEnable = {}
        )
    }
}