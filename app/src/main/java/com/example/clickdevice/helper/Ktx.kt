package com.example.clickdevice.helper

import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.coroutines.CoroutineContext



fun Modifier.onClick(enabled: Boolean = true, time: Long = 500, onClick: () -> Unit) =
    composed {
        val lastTime = remember {
            mutableStateOf(0L)
        }
        return@composed clickable (enabled = enabled, onClick = {
            if (SystemClock.uptimeMillis() - lastTime.value > time) {
                onClick()
                lastTime.value = SystemClock.uptimeMillis()
            }
        })
    }

fun Long.toDate(): String {
    val date = Date(this)
    var simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    return simpleDateFormat.format(date)
}

fun View.setOnTouchClick(onClick: () -> Unit, onDown: () -> Boolean = { true }) {
    setOnTouchListener(TouchClickListener({
        onClick()
    }, onDown))
}

class TouchClickListener(
    private val listener: View.OnClickListener?,
    private val  onDown: () -> Boolean = { true }

) : OnTouchListener {

    var lastX: Float = 0f
    var lastY: Float = 0f
    var canClick: Boolean = true


    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        val rawX = event.getRawX()
        val rawY = event.getRawY()
        val overstep = (rawX - lastX) * (rawX - lastX) + (rawY - lastY) * (rawY - lastY) < 100
        when (event.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                lastX = rawX
                lastY = rawY
                return onDown()
            }

            MotionEvent.ACTION_MOVE -> if (canClick) {
                canClick = overstep
            }

            MotionEvent.ACTION_UP -> {
                if (canClick) {
                    canClick = overstep
                    if (canClick) {
                        listener?.onClick(v)
                    }
                }
                canClick = true
            }
        }

        return true
    }
}


fun CoroutineExceptionLog(
    tag: String,
    callBack: (Throwable) -> Unit = {}
): CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    Log.d("CoroutineExceptionLog", "$tag: $throwable")
    callBack.invoke(throwable)
}

fun IOCoroutineContext(
    tag: String = "IOCoroutineContext",
    exceptionCallBack: (Throwable) -> Unit = {}
): CoroutineContext =
    Dispatchers.IO + SupervisorJob() + CoroutineExceptionLog(tag, exceptionCallBack)


fun MainCoroutineContext(
    tag: String = "MainCoroutineContext",
    exceptionCallBack: (Throwable) -> Unit = {}
): CoroutineContext =
    Dispatchers.Main + SupervisorJob() + CoroutineExceptionLog(tag, exceptionCallBack)