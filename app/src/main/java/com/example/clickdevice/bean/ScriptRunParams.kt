package com.example.clickdevice.bean

import com.google.gson.Gson

class ScriptRunParams(
    var intervalTime: Int = 1000,
    var clickCount: Int = 0,
    var speed: Double = 1.0,
    var checkAppChange: Boolean = false,
    var xCoefficient: Float = 1.0f,
    var yCoefficient: Float = 1.0f,
    var actionName: String = ""
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): ScriptRunParams {
            return try {
                Gson().fromJson(json, ScriptRunParams::class.java)
            } catch (_: Exception) {
                ScriptRunParams()
            }
        }
    }
}

