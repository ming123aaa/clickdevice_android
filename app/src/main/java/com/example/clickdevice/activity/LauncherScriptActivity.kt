package com.example.clickdevice.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.Ohuang.ilivedata.MyLiveData
import com.example.clickdevice.MyApp
import com.example.clickdevice.MyService
import com.example.clickdevice.db.RecordScriptBean
import com.example.clickdevice.db.ScriptDataBean
import com.example.clickdevice.dialog.DialogHelper
import com.google.gson.Gson

class LauncherScriptActivity : Activity() {

    companion object {
        const val TYPE = "type"
        const val TYPE_SCRIPT = "type_script"
        const val TYPE_RECORD_SCRIPT = "type_record_script"

        const val ID = "id"

    }

    val TAG = "LauncherScriptActivity"


    var isLoad = false

    var scriptDataBean: ScriptDataBean? = null
    var recordScriptBean: RecordScriptBean? = null
    var type: String? = null
    var isLauncher=false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = intent.getStringExtra(TYPE)
        val id = intent.getIntExtra(ID, -1)
        Log.d(TAG, "onCreate: type=$type  id=$id")
        val appDatabase = (application as MyApp).appDatabase

        if (id > 0) {
            if (type == TYPE_RECORD_SCRIPT) {
                Thread {
                    recordScriptBean = appDatabase.recordScriptDao.findBeanById(id)
                    isLoad = true
                    runOnUiThread {
                        if (MyService.isStart()) {
                            startScript()
                        }
                    }
                }.start()

            } else if (type == TYPE_SCRIPT) {
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

        } else {
            finish()
            return
        }
        if (!MyService.isStart()) {
            DialogHelper.showMessagePositiveDialog(
                this, "辅助功能", "使用连点器需要开启(无障碍)辅助功能，是否现在去开启？", { dialog, index ->
                    try {
                        startActivity(Intent("android.settings.ACCESSIBILITY_SETTINGS"))
                    } catch (e: Exception) {
                        startActivity(Intent("android.settings.SETTINGS"))
                        e.printStackTrace()
                    }
                }, {
                    finish()
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (MyService.isStart() && isLoad) {
            startScript()
            finish()
        }
    }

    fun startScript() {
        if (isLauncher){
            return
        }
        isLauncher=true
        if (type == TYPE_RECORD_SCRIPT) {
            if (recordScriptBean == null) {
                Toast.makeText(this, "没有脚本数据", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            startRecordScriptPlayActivity()
        }
        if (type == TYPE_SCRIPT) {
            if (scriptDataBean == null) {
                Toast.makeText(this, "没有脚本数据", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            startScriptActivity()
        }
    }


    private fun startRecordScriptPlayActivity() {
        MyLiveData.getInstance().with("RecordScriptPlay", RecordScriptBean::class.java)
            .postValue(recordScriptBean)
        startActivity(Intent(this, RecordScriptPlayActivity::class.java))
    }

    private fun startScriptActivity() {
        MyLiveData.getInstance().with<String>("json", String::class.java)
            .setValue(scriptDataBean!!.getScriptJson())
        MyLiveData.getInstance().with<String>("scriptName", String::class.java)
            .setValue(scriptDataBean!!.getName())
        startActivity(Intent(this, ScriptActivity::class.java))
    }
}