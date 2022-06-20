package com.example.clickdevice.vm

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import com.example.clickdevice.RecordScriptExecutor
import com.example.clickdevice.bean.RecordScriptCmd
import com.example.clickdevice.bean.ScriptCmdBean
import com.example.clickdevice.db.AppDatabase
import com.example.clickdevice.db.RecordScriptBean
import com.example.clickdevice.helper.toDate
import com.google.gson.Gson
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RecordScriptViewModel : ViewModel() {


    var data = ArrayList<RecordScriptCmd>()

    val singleThreadExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    val recordScriptExecutor = RecordScriptExecutor()

    var recordScriptBean: RecordScriptBean? = null

    var isRecord = false

    var lastTime = 0L


    fun playScript() {
        recordScriptExecutor.run(data)
    }


    fun addRecordScriptCmd(recordScriptCmd: RecordScriptCmd) {
        if (isRecord) {
            data.add(recordScriptCmd)
        }
    }

    fun postLastTime() {
        lastTime = SystemClock.uptimeMillis()
    }

    fun addDelayTime() {
        if (isRecord) {
            if (lastTime > 0) {
                val time = SystemClock.uptimeMillis() - lastTime
                val createDelayCMD = RecordScriptCmd.createDelayCMD(time.toInt())
                addRecordScriptCmd(createDelayCMD)
            }

            lastTime = SystemClock.uptimeMillis()
        }
    }

    fun startRecord() {
        isRecord = true
        lastTime = SystemClock.uptimeMillis()
    }

    fun stopRecord() {
        isRecord = false
    }

    suspend fun saveScript(context: Context,name: String) {
        if (recordScriptBean == null) {
            recordScriptBean=createRecordScriptCmd(name)
        }else{
            var currentTimeMillis = System.currentTimeMillis()
            recordScriptBean!!.name=name
            recordScriptBean!!.updateTime=currentTimeMillis.toDate()
            var gson = Gson()
            var toJson = gson.toJson(data)
            recordScriptBean!!.scriptJson=toJson
        }
        if (recordScriptBean!!.id>0){
            update(context,recordScriptBean!!)
        }else{
            insert(context,recordScriptBean!!)
        }
    }

    private fun update(context: Context, recordScript:RecordScriptBean){
        AppDatabase.getInstance(context).recordScriptDao.updateRecordScriptBean(recordScript)
    }
    private fun insert(context: Context, recordScript:RecordScriptBean){
        AppDatabase.getInstance(context).recordScriptDao.insertRecordScriptBean(recordScript)
    }



    private fun  createRecordScriptCmd(name:String):RecordScriptBean{
        var recordScriptBean1 = RecordScriptBean()
        var currentTimeMillis = System.currentTimeMillis()
        recordScriptBean1.name=name
        recordScriptBean1.createTime=currentTimeMillis.toDate()
        recordScriptBean1.updateTime=currentTimeMillis.toDate()
        var gson = Gson()
        var toJson = gson.toJson(data)
        recordScriptBean1.scriptJson=toJson
        return recordScriptBean1
    }


}