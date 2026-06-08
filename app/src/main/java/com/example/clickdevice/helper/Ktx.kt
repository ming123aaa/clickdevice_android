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
        return@composed clickable(enabled = enabled, onClick = {
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

fun View.setOnTouchClick(
    onClick: () -> Unit,
    onDown: () -> Boolean = { true }, //触发ACTION_DOWN时回调  ture 点击/长按事件可用  false 点击/长按事件不可用
    onLongClick: () -> Boolean = { true },  //  ture  拦截点击事件  false 不拦截点击事件
    maxMovePx: Int = 20,
) {
    setOnTouchListener(
        TouchClickListener(
            maxMovePx = maxMovePx,
            listener = {
                onClick()
            },
            onDown = onDown,
            onLongClick = { onLongClick() }
        )
    )
}

class TouchClickListener(
    private val maxMovePx: Int = 20,//可移动的最大像素单位
    private val listener: View.OnClickListener?,
    private val onDown: () -> Boolean = { true },//触发ACTION_DOWN时回调 ture 点击/长按事件可用  false 点击/长按事件不可用
    private val onLongClick: View.OnLongClickListener?

) : OnTouchListener {

    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var canClick: Boolean = true

    private var isLongClick: Boolean = false
    private var lastDownTime: Long = Long.MAX_VALUE


    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val rawX = event.rawX
        val rawY = event.rawY

        val overstep = if (maxMovePx > 0) {
            (rawX - lastX) * (rawX - lastX) + (rawY - lastY) * (rawY - lastY) < maxMovePx * maxMovePx
        } else {
            event.x >= 0 && event.y >= 0 && event.x <= view.width && event.y <= view.height
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = rawX
                lastY = rawY
                lastDownTime = System.currentTimeMillis()
                return onDown()
            }

            MotionEvent.ACTION_MOVE -> if (canClick) {
                canClick = overstep
                if (System.currentTimeMillis() - lastDownTime > 2500 && !isLongClick) {
                    isLongClick = true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (canClick) {
                    canClick = overstep
                    if (System.currentTimeMillis() - lastDownTime > 2500 && !isLongClick) {
                        isLongClick = true
                    }
                    if (canClick) {
                        if (isLongClick) {

                            if (onLongClick?.onLongClick(view) == false) {
                                listener?.onClick(view)
                            }
                        } else {
                            listener?.onClick(view)
                        }
                    }
                }
                lastDownTime= Long.MAX_VALUE
                canClick = true
                isLongClick = false

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