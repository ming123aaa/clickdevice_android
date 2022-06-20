package com.example.clickdevice.helper

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext


fun Long.toDate(): String {
    val date = Date(this)
    var simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    return simpleDateFormat.format(date)


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