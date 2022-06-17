package com.example.clickdevice.vm

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import com.example.clickdevice.RecordScriptExecutor
import com.example.clickdevice.bean.RecordScriptCmd
import com.example.clickdevice.bean.ScriptCmdBean
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RecordScriptViewModel : ViewModel() {


    var data = ArrayList<RecordScriptCmd>()

    val singleThreadExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    val recordScriptExecutor = RecordScriptExecutor()


    var isRecord = false

    var lastTime = 0L


    fun playScript(){
        recordScriptExecutor.run(data)
    }


    fun addRecordScriptCmd(recordScriptCmd: RecordScriptCmd) {
        if (isRecord) {
            data.add(recordScriptCmd)
        }
    }

    fun postLastTime(){
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

    fun startRecord(){
        isRecord=true
        lastTime=SystemClock.uptimeMillis()
    }

    fun stopRecord(){
        isRecord=false
    }


}