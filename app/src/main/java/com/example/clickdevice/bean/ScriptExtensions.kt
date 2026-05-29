package com.example.clickdevice.bean

import com.example.clickdevice.db.RecordScriptBean
import com.example.clickdevice.db.ScriptDataBean
import com.google.gson.reflect.TypeToken

fun ScriptDataBean.getScriptCmd(): List<ScriptCmdBean> {
    return try {
        GsonUtil.gson.fromJson(
            scriptJson,
            object : TypeToken<List<ScriptCmdBean>>() {}.type
        )
    } catch (_: Exception) {
        emptyList()
    }
}

fun RecordScriptBean.getScript(): List<RecordScriptCmd> {
    return try {
        GsonUtil.gson.fromJson(
            scriptJson,
            object : TypeToken<List<RecordScriptCmd>>() {}.type
        )
    } catch (_: Exception) {
        emptyList()
    }
}
