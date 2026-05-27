package com.example.clickdevice.bean

import com.example.clickdevice.db.ScriptGroupBean
import com.example.clickdevice.helper.toDate


data class ScriptGroup(
    val name: String,
    val actionMap: Map<String, ScriptCmdBean>,
    val actionScript: List<ActionScript>,
    val xCoefficient: Float = 1.0f,
    val yCoefficient: Float = 1.0f
) {

    fun getScriptCmdBean(actionName: String): ScriptCmdBean? {
        return actionMap[actionName]
    }

    fun getListScriptCmdBean(actionScript:ActionScript):List<ScriptCmdBean>{
        return actionScript.script.map { getScriptCmdBean(it)?:ScriptCmdBean.BuildNoneCMD() }
    }
}

data class SimpleScriptGroup( val name: String,
                              val actionMap: Map<String, ScriptCmdBean>){

}


fun ScriptGroup.toSimpleScriptGroup():SimpleScriptGroup{
    actionMap.onEach {
        it.value.content=it.value.actionTypeName
    }
    return  SimpleScriptGroup(name,actionMap)
}

data class ActionScript(
    var name: String,
    var script: MutableList<String>
)


fun ScriptGroup.toScriptGroupBean():ScriptGroupBean{
    val scriptGroupBean = ScriptGroupBean()
    scriptGroupBean.createTime=System.currentTimeMillis().toDate()
    scriptGroupBean.updateTime=System.currentTimeMillis().toDate()
    scriptGroupBean.name=name
    scriptGroupBean.xCoefficient=xCoefficient
    scriptGroupBean.yCoefficient=yCoefficient
    scriptGroupBean.scriptJson=GsonUtil.gson.toJson(this@toScriptGroupBean)
    return scriptGroupBean
}

fun ScriptGroup.toScriptGroupBean(old:ScriptGroupBean):ScriptGroupBean{
    val scriptGroupBean = ScriptGroupBean()
    scriptGroupBean.id=old.id
    scriptGroupBean.createTime=old.createTime
    scriptGroupBean.updateTime=System.currentTimeMillis().toDate()
    scriptGroupBean.name=name
    scriptGroupBean.xCoefficient=xCoefficient
    scriptGroupBean.yCoefficient=yCoefficient
    scriptGroupBean.scriptJson=GsonUtil.gson.toJson(this@toScriptGroupBean)
    return scriptGroupBean
}
fun ScriptGroupBean.toScriptGroup():ScriptGroup{
    return GsonUtil.gson.fromJson(scriptJson,ScriptGroup::class.java)
}

