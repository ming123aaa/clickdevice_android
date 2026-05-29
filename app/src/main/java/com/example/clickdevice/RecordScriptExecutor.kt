package com.example.clickdevice

import android.graphics.Path
import com.example.clickdevice.bean.Bean
import com.example.clickdevice.bean.RecordScriptCmd

class RecordScriptExecutor {

    var delayCoefficient = 1.0
    var recordScriptInterface: RecordScriptInterface? = null


    fun run(data: List<RecordScriptCmd>) {
        try {
            repeat(data.size) {
                if (recordScriptInterface == null || !recordScriptInterface!!.isRun()) {
                    return@repeat
                }
                when (data[it].type) {
                    RecordScriptCmd.Type.Delay -> {
                        delay(data[it])
                    }

                    RecordScriptCmd.Type.Gesture -> {
                        gesture(it, data[it])
                    }

                    else -> {
                    }
                }
            }
        } catch (e: Throwable) {
        }

    }

    private fun delay(recordScriptCmd: RecordScriptCmd) {
        delay(recordScriptCmd.delayed.toLong())
    }

    fun sleep(time: Long): Boolean {
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

    fun delay(time: Long): Boolean {
        if (time <= 0) {
            return false
        }
        val adjustedTime = (time * delayCoefficient).toLong()
        return sleep(adjustedTime)
    }

    private fun gesture(position: Int, recordScriptCmd: RecordScriptCmd) {
        if (delay(recordScriptCmd.delayed.toLong())) {
            return
        }

        recordScriptInterface?.apply {
            if (!isRun()||recordScriptCmd.path == null || recordScriptCmd.path.isEmpty()) {
                return@apply
            }

            val bean = recordScriptCmd.path[0]
            preDispatchGesture(bean.x, bean.y)
            sleep(100)
            val createPath = createPath(recordScriptCmd.path)
            var duration = (recordScriptCmd.duration * delayCoefficient).toInt()
            if (duration < 10) {
                duration = 10
            }
            try {
                dispatchGesture(position, createPath, duration)
                sleep(duration.toLong())
            }catch (e: Throwable){}
            sleep(100)
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