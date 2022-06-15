package com.example.clickdevice

import android.graphics.Path
import com.example.clickdevice.db.RecordScriptBean

class RecordScriptExecutor {


    var recordScriptInterface:RecordScriptInterface?=null


    fun run(data:MutableList<RecordScriptBean>){
        repeat(data.size) {

        }
    }





    interface RecordScriptInterface{

        fun isRun():Boolean

        fun preDispatchGesture(x:Int,y:Int)

        fun dispatchGesture(path: Path,duration:Int)

        fun endDispatchGesture()

    }
}