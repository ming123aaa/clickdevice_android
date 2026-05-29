package com.example.clickdevice.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Ohuang.ilivedata.MyLiveData
import com.example.clickdevice.MyApp
import com.example.clickdevice.MyService
import com.example.clickdevice.R
import com.example.clickdevice.bean.ScriptGroup
import com.example.clickdevice.bean.toScriptGroup
import com.example.clickdevice.db.RecordScriptBean
import com.example.clickdevice.db.ScriptDataBean
import com.example.clickdevice.db.ScriptGroupBean
import com.example.clickdevice.ui.theme.ClickDeviceTheme

class LauncherScriptActivity : ComponentActivity() {

    companion object {
        const val TYPE = "type"
        const val TYPE_SCRIPT = "type_script"  //普通脚本
        const val TYPE_RECORD_SCRIPT = "type_record_script" //录制脚本
        const val TYPE_SCRIPT_GROUP = "type_script_group" //脚本组
        const val TYPE_KEY_BINDING = "type_key_binding" //按键设置
        const val ID = "id"
        private const val TAG = "LauncherScriptActivityCompose"
    }

    private var isLoad = false
    private var scriptDataBean: ScriptDataBean? = null
    private var recordScriptBean: RecordScriptBean? = null
    private var scriptGroupBean: ScriptGroupBean? = null
    private var type: String? = null
    private var id = -1
    private var isLauncher = false
    private var showAccessibilityDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = intent.getStringExtra(TYPE)
        id = intent.getIntExtra(ID, -1)
        Log.d(TAG, "onCreate: type=$type  id=$id")

        setContent {
            ClickDeviceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LauncherScriptScreen(
                        showAccessibilityDialog = showAccessibilityDialog,
                        onDismissDialog = {
                            showAccessibilityDialog = false
                            finish()
                        },
                        onOpenAccessibility = {
                            showAccessibilityDialog = false
                            try {
                                startActivity(Intent("android.settings.ACCESSIBILITY_SETTINGS"))
                            } catch (e: Exception) {
                                startActivity(Intent("android.settings.SETTINGS"))
                                e.printStackTrace()
                            }
                        }
                    )
                }
            }
        }

        window.decorView.postDelayed({
            initConfig()
        }, 500)
    }

    private fun initConfig() {
        val appDatabase = (application as MyApp).appDatabase

        if (type == TYPE_KEY_BINDING) {
            startActivity(Intent(this, KeyBindingListActivity::class.java))
            finish()
            return
        }

        if (id >= 0) {
            when (type) {
                TYPE_RECORD_SCRIPT -> {
                    Thread {
                        recordScriptBean = appDatabase.recordScriptDao.findBeanById(id)
                        isLoad = true
                        runOnUiThread {
                            if (MyService.isStart()) {
                                startScript()
                            }
                        }
                    }.start()
                }
                TYPE_SCRIPT -> {
                    Thread {
                        scriptDataBean = appDatabase.scriptDao.findBeanById(id)
                        isLoad = true
                        runOnUiThread {
                            if (MyService.isStart()) {
                                startScript()
                            }
                        }
                    }.start()
                }
                TYPE_SCRIPT_GROUP -> {
                    Thread {
                        scriptGroupBean = appDatabase.scriptGroupDao.findBeanById(id)
                        isLoad = true
                        runOnUiThread {
                            if (MyService.isStart()) {
                                startScript()
                            }
                        }
                    }.start()
                }
            }
        } else {
            finish()
            return
        }

        if (!MyService.isStart()) {
            showAccessibilityDialog = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (MyService.isStart() && isLoad) {
            startScript()
            finish()
        }
    }

    private fun startScript() {
        if (isLauncher) return
        isLauncher = true

        when (type) {
            TYPE_RECORD_SCRIPT -> {
                if (recordScriptBean == null) {
                    Toast.makeText(this, "没有脚本数据", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
                startRecordScriptPlayActivity()
            }
            TYPE_SCRIPT -> {
                if (scriptDataBean == null) {
                    Toast.makeText(this, "没有脚本数据", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
                startScriptActivity()
            }
            TYPE_SCRIPT_GROUP -> {
                if (scriptGroupBean == null) {
                    Toast.makeText(this, "没有脚本组数据", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
                startScriptGroupPlayActivity()
            }
        }
    }

    private fun startRecordScriptPlayActivity() {
        MyLiveData.getInstance().with("RecordScriptPlay", RecordScriptBean::class.java)
            .postValue(recordScriptBean)
        startActivity(Intent(this, RecordScriptPlayActivityCompose::class.java))
    }

    private fun startScriptActivity() {
        MyLiveData.getInstance().with<String>("json", String::class.java)
            .setValue(scriptDataBean!!.getScriptJson())
        MyLiveData.getInstance().with<String>("scriptName", String::class.java)
            .setValue(scriptDataBean!!.getName())
        startActivity(Intent(this, ScriptActivityCompose::class.java))
    }

    private fun startScriptGroupPlayActivity() {
        val scriptGroup = scriptGroupBean!!.toScriptGroup()
        MyLiveData.getInstance().with("ScriptGroup", ScriptGroup::class.java)
            .postValue(scriptGroup)
        startActivity(Intent(this, ScriptGroupPlayActivityCompose::class.java))
    }
}

@Composable
fun LauncherScriptScreen(
    showAccessibilityDialog: Boolean = false,
    onDismissDialog: () -> Unit = {},
    onOpenAccessibility: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.icon_app),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }

    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text("辅助功能") },
            text = { Text("使用连点器需要开启(无障碍)辅助功能，是否现在去开启？") },
            confirmButton = {
                TextButton(onClick = onOpenAccessibility) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDialog) {
                    Text("取消")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LauncherScriptScreenPreview() {
    ClickDeviceTheme {
        LauncherScriptScreen()
    }
}
