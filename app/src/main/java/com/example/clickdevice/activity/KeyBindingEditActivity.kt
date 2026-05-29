package com.example.clickdevice.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clickdevice.PowerKeyObserver
import com.example.clickdevice.bean.ScriptGroup
import com.example.clickdevice.bean.ScriptRunParams
import com.example.clickdevice.bean.toScriptGroup
import com.example.clickdevice.db.AppDatabase
import com.example.clickdevice.db.KeyBindingBean
import com.example.clickdevice.ui.theme.ClickDeviceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
class KeyBindingEditActivity : AppCompatActivity() {
    private  var binding: KeyBindingBean by mutableStateOf(KeyBindingBean())
    private var isNew = true
    private var selectedScriptType by mutableStateOf( "")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        isNew = intent.getBooleanExtra("isNew", true)
        
        if (!isNew) {
            val id = intent.getIntExtra("id", -1)
            if (id > 0) {
                GlobalScope.launch(Dispatchers.IO) {
                    val db = AppDatabase.getInstance(this@KeyBindingEditActivity)
                    val bean = db.getKeyBindingDao().findBeanById(id)
                    if (bean!=null){
                        binding = bean
                        selectedScriptType=bean.scriptType
                    }

                    withContext(Dispatchers.Main) {
                        initUI()
                    }
                }
            } else {
                binding = KeyBindingBean()
                initUI()
            }
        } else {
            binding = KeyBindingBean().apply {
                textColor = -0x1000000
                textSize = 16

            }
            initUI()
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    private fun initUI() {
        setContent {
            ClickDeviceTheme {
                KeyBindingEditScreen(
                    binding = binding,
                    isNew = isNew,
                    selectedScriptType=selectedScriptType,
                    onSave = { saveBinding(it) },
                    onBack = { finish() },
                    onSelectScript = { type ->
                        selectScript(type)
                    }
                )
            }
        }
    }

    private fun selectScript(type: String) {
        val intent = when (type) {
            LauncherScriptActivity.TYPE_SCRIPT -> Intent(this, ScriptListActivityCompose::class.java)
            LauncherScriptActivity.TYPE_RECORD_SCRIPT -> Intent(this, RecordScriptListActivityCompose::class.java)
            LauncherScriptActivity.TYPE_SCRIPT_GROUP -> Intent(this, ScriptGroupListActivityCompose::class.java)
            else -> return
        }
        intent.putExtra("selectMode", true)
        startActivityForResult(intent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            binding.scriptType = data.getStringExtra("type") ?: ""
            binding.scriptId = data.getIntExtra("id", 0)
            binding.scriptName = data.getStringExtra("name") ?: ""
            selectedScriptType=binding.scriptType
            initUI()
        }
    }

    private fun saveBinding(binding: KeyBindingBean) {
        if (binding.keyName.isBlank()) {
            Toast.makeText(this, "请输入按键名称", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (binding.scriptType.isBlank() || binding.scriptId <= 0) {
            Toast.makeText(this, "请选择脚本", Toast.LENGTH_SHORT).show()
            return
        }

        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        if (isNew) {
            binding.createTime = now
            binding.updateTime = now
        } else {
            binding.updateTime = now
        }

        GlobalScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@KeyBindingEditActivity)
            db.getKeyBindingDao().insertKeyBindingBean(binding)
            
            withContext(Dispatchers.Main) {
                Toast.makeText(this@KeyBindingEditActivity, "保存成功", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}

@ExperimentalLayoutApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyBindingEditScreen(
    binding: KeyBindingBean,
    selectedScriptType: String="",
    isNew: Boolean,
    onSave: (KeyBindingBean) -> Unit,
    onBack: () -> Unit,
    onSelectScript: (String) -> Unit
) {
    val context = LocalContext.current
    var keyName by remember { mutableStateOf(binding.keyName ?: "") }
    var keyDescription by remember { mutableStateOf(binding.keyDescription ?: "") }
    var textSize by remember { mutableStateOf(binding.textSize.toString()) }
    var selectedColor by remember { mutableStateOf(binding.textColor) }

    
    var intervalTime by remember { mutableStateOf("1000") }
    var clickCount by remember { mutableStateOf("0") }
    var speed by remember { mutableStateOf("1") }
    var checkAppChange by remember { mutableStateOf(false) }
    var xCoefficient by remember { mutableStateOf("") }
    var yCoefficient by remember { mutableStateOf("") }
    
    var actions by remember { mutableStateOf<List<com.example.clickdevice.bean.ActionScript>>(emptyList()) }
    var selectedActionName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        binding.scriptParams?.let { params ->
            try {
                val scriptParams = ScriptRunParams.fromJson(params)
                intervalTime = scriptParams.intervalTime.toString()
                clickCount = scriptParams.clickCount.toString()
                speed = scriptParams.speed.toString()
                checkAppChange = scriptParams.checkAppChange
                xCoefficient = scriptParams.xCoefficient.toString()
                yCoefficient = scriptParams.yCoefficient.toString()
                selectedActionName = scriptParams.actionName
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(binding.scriptType, binding.scriptId) {
        if (binding.scriptType == LauncherScriptActivity.TYPE_SCRIPT_GROUP && binding.scriptId > 0) {
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val db = com.example.clickdevice.db.AppDatabase.getInstance(context)
                val bean = db.getScriptGroupDao().findBeanById(binding.scriptId)
                bean?.let { scriptGroupBean ->
                    try {
                        val scriptGroup = scriptGroupBean.toScriptGroup()
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            actions = scriptGroup.actionScript
                            if (selectedActionName.isEmpty() && actions.isNotEmpty()) {
                                selectedActionName = actions[0].name
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val scriptTypes = listOf(
        LauncherScriptActivity.TYPE_SCRIPT to "普通脚本",
        LauncherScriptActivity.TYPE_RECORD_SCRIPT to "录制脚本",
        LauncherScriptActivity.TYPE_SCRIPT_GROUP to "自定义脚本"
    )

    val colorOptions = listOf(
        -0x1000000 to "黑色",       // #000000
        -0x1 to "白色",            // #FFFFFF
        -0xff0001 to "红色",       // #FF0000
        -0xff0100 to "绿色",       // #00FF00
        -0x100 to "蓝色",          // #0000FF
        -0xffff01 to "黄色",       // #FFFF00
        -0xff00ff to "紫色",       // #FF00FF
        -0xff8000 to "橙色",       // #FF8000

        -0x445566 to "深空灰",     // #445566
        -0xeeddcc to "米白",       // #EEDDCC
        -0xdd8899 to "脏粉色",     // #DD8899
        -0xaaccbb to "鼠尾草绿",   // #AACCBB
        -0x88aacc to "静谧蓝",     // #88AACC
        -0xffcc99 to "奶油杏",     // #FFCC99
        -0xccaa88 to "卡其色",     // #CCAA88
        -0x99aacc to "灰蓝色" ,     // #99AACC

        -0xcc7788 to "樱花粉",     // #CC7788
        -0xaa4499 to "紫罗兰",     // #AA4499
        -0x6699cc to "天蓝色",     // #6699CC
        -0x88cc88 to "薄荷绿",     // #88CC88
        -0xffaa66 to "杏色",       // #FFAA66
        -0xdd6688 to "珊瑚红",     // #DD6688
        -0x669999 to "深海绿",     // #669999
        -0xcc9966 to "奶茶色",     // #CC9966
        -0x9966cc to "淡紫色",     // #9966CC
        -0xff8866 to "暖橘色",     // #FF8866
        -0x88aadd to "雾霾蓝",     // #88AADD
        -0xddaa88 to "裸色"        // #DDAA88
    )

    fun updateBinding() {
        binding.keyName = keyName
        binding.keyDescription = keyDescription
        binding.textSize = textSize.toIntOrNull() ?: 16
        binding.textColor = selectedColor
        binding.scriptType = selectedScriptType
        
        val params = ScriptRunParams(
            intervalTime = intervalTime.toIntOrNull() ?: 1000,
            clickCount = clickCount.toIntOrNull() ?: 0,
            speed = speed.toDoubleOrNull() ?: 1.0,
            checkAppChange = checkAppChange,
            xCoefficient = xCoefficient.toFloatOrNull() ?: 1.0f,
            yCoefficient = yCoefficient.toFloatOrNull() ?: 1.0f,
            actionName = if (binding.scriptType == LauncherScriptActivity.TYPE_SCRIPT_GROUP) selectedActionName else ""
        )
        binding.scriptParams = params.toJson()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "添加按键" else "编辑按键", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            updateBinding()
                            onSave(binding)
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SectionCard(title = "按键信息", icon = Icons.Default.Title) {
                OutlinedTextField(
                    value = keyName,
                    onValueChange = { keyName = it },
                    label = { Text("按键名称") },
                    placeholder = { Text("输入按键显示名称") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyDescription,
                    onValueChange = { keyDescription = it },
                    label = { Text("按键说明") },
                    placeholder = { Text("输入按键描述") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            SectionCard(title = "字体颜色", icon = Icons.Default.Palette) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colorOptions.forEach { (color, _) ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .then(
                                    if (selectedColor == color) Modifier.border(
                                        width = 3.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    else Modifier
                                )
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (color == -0x1) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = textSize,
                    onValueChange = { textSize = it.filter { c -> c.isDigit() } },
                    label = { Text("字体大小") },
                    placeholder = { Text("16") },
                    suffix = { Text("sp") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            SectionCard(title = "脚本设置", icon = Icons.Default.PlayArrow) {
                Text(
                    "选择脚本类型",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    scriptTypes.forEach { (type, name) ->
                        FilterChip(
                            selected = selectedScriptType == type,
                            onClick = {

                                onSelectScript(type)
                            },
                            label = { Text(name, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (binding.scriptId > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "已选择脚本",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow("类型", getKeyScriptTypeName(binding.scriptType))
                            InfoRow("名称", binding.scriptName ?: "")
                            InfoRow("ID", binding.scriptId.toString())
                        }
                    }
                }

                if (binding.scriptType == LauncherScriptActivity.TYPE_SCRIPT_GROUP && actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "选择动作",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow (
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        actions.forEach { action ->
                            FilterChip(
                                selected = selectedActionName == action.name,
                                onClick = { selectedActionName = action.name },
                                label = { Text(action.name, modifier = Modifier.padding(vertical = 2.dp, horizontal = 5.dp)) },
                                modifier = Modifier.padding(vertical = 3.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }
            }

            if (binding.scriptId > 0) {
                SectionCard(title = "运行参数", icon = Icons.Default.PlayArrow) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = intervalTime,
                            onValueChange = { intervalTime = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.weight(1f),
                            label = { Text("间隔") },
                            placeholder = { Text("1000") },
                            suffix = { Text("ms") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = clickCount,
                            onValueChange = { clickCount = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.weight(1f),
                            label = { Text("次数") },
                            placeholder = { Text("0") },
                            suffix = { Text("次") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = speed,
                        onValueChange = { speed = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("执行速度倍速") },
                        placeholder = { Text("1") },
                        suffix = { Text("倍") },
                        supportingText = { Text("取值范围 0.25~5") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { checkAppChange = !checkAppChange }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checkAppChange,
                            onCheckedChange = { checkAppChange = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("检测到应用切换时停止脚本", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                SectionCard(title = "坐标系数", icon = Icons.Default.Title) {
                    Text(
                        "实际坐标 = 原始坐标 x 系数，取值范围 0.25~5",
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
                            value = xCoefficient,
                            onValueChange = { value ->
                                if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    xCoefficient = value
                                }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("1") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Text("Y", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = yCoefficient,
                            onValueChange = { value ->
                                if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    yCoefficient = value
                                }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("1") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true)
@Composable
fun KeyBindingEditPreview() {
    ClickDeviceTheme {
        KeyBindingEditScreen(
            binding = KeyBindingBean().apply {
                keyName = "测试按键"
                keyDescription = "测试描述"
                textSize = 16
                textColor = -0x1000000
            },
            isNew = true,
            onSave = {},
            onBack = {},
            onSelectScript = {}
        )
    }
}