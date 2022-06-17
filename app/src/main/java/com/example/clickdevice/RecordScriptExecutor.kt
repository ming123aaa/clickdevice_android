package com.example.clickdevice

import android.gesture.Gesture
import android.graphics.Path
import com.example.clickdevice.bean.Bean
import com.example.clickdevice.bean.RecordScriptCmd

class RecordScriptExecutor {


     var recordScriptInterface: RecordScriptInterface? = null


    fun run(data: MutableList<RecordScriptCmd>) {

        repeat(data.size) {
            if (recordScriptInterface == null || !recordScriptInterface!!.isRun()) {
                return@repeat
            }
            when (data[it].type) {
                RecordScriptCmd.Type.Delay -> {
                    delay(data[it])
                }
                RecordScriptCmd.Type.Gesture -> {
                    gesture(it,data[it])
                }
                else -> {
                }
            }
        }
    }

    private fun delay(recordScriptCmd: RecordScriptCmd) {
        delay(recordScriptCmd.delayed.toLong())
    }

    private fun delay(time: Long): Boolean {
        if (time <= 0) {
            return false
        }
        val count = time / 10
        val t = time % 10
        Thread.sleep(t)
        for (i in 0 until count) {
            if (recordScriptInterface == null || !recordScriptInterface!!.isRun()) {
                return true
            }
            Thread.sleep(10)
        }
        return false
    }

    private fun gesture(position: Int, recordScriptCmd: RecordScriptCmd) {
        if (delay(recordScriptCmd.delayed.toLong())) {
            return
        }
        recordScriptInterface?.apply {
            if (recordScriptCmd.path == null || recordScriptCmd.path.size == 0) {
                return@apply
            }

            val bean = recordScriptCmd.path[0]
            preDispatchGesture(bean.x, bean.y)
            delay(100)
            val createPath = createPath(recordScriptCmd.path)
            dispatchGesture(position, createPath, recordScriptCmd.duration)
            delay(recordScriptCmd.duration.toLong())
            delay(100)
            endDispatchGesture()
        }

    }

    private fun createPath(data: MutableList<Bean>): Path {
        val path = Path()
        val bean = data[0]
        path.moveTo(bean.x.toFloat(), bean.y.toFloat())
        for (i in 1 until data.size) {
            val bean2 = data[i]
            path.lineTo(bean2.x.toFloat(), bean2.y.toFloat())
        }
        return path
    }


    interface RecordScriptInterface {

        fun isRun(): Boolean

        fun preDispatchGesture(x: Int, y: Int)

        fun dispatchGesture(position: Int, path: Path, duration: Int)

        fun endDispatchGesture()

    }
}