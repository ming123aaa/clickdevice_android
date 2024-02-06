package com.example.clickdevice.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.example.clickdevice.R
import com.example.clickdevice.bean.ActionScript
import com.example.clickdevice.bean.ScriptCmdBean
import com.example.clickdevice.bean.ScriptGroup
import com.example.clickdevice.bean.toScriptGroup
import com.example.clickdevice.bean.toScriptGroupBean
import com.example.clickdevice.db.AppDatabase
import com.example.clickdevice.db.ScriptGroupBean
import com.example.clickdevice.fg.ScriptGroupEditFragment
import com.example.clickdevice.fg.ScriptGroupEditListFragment
import com.example.clickdevice.fg.ScriptGroupEditScriptFragment
import com.example.clickdevice.helper.FragmentHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList
import java.util.TreeMap

class ScriptGroupEditActivity : AppCompatActivity() {

    sealed class Page {
        object ScriptGroupEdit : Page()
        object ScriptList : Page()
        object ScriptEdit : Page()
    }

    private val _mLiveData = MutableLiveData<ScriptGroup>()
    val mLiveData: MutableLiveData<ScriptGroup> = _mLiveData

    private val _page = MutableLiveData<Page>()

    private var scriptGroupBean: ScriptGroupBean?=null

    companion object {
        fun startActivity(context: Context, scriptGroupId: Int = 0) {
            val intent = Intent(context, ScriptGroupEditActivity::class.java)
            intent.putExtra("scriptGroupId", scriptGroupId)
            context.startActivity(intent)
        }
    }

    private val scriptGroupId by lazy {
        intent.getIntExtra("scriptGroupId", 0)
    }

    private val scriptGroupEditFragmentHelper: FragmentHelper<ScriptGroupEditFragment> by lazy {
        FragmentHelper(
            id = R.id.frameLayout, fragmentManager = supportFragmentManager,
            tag = "ScriptGroupEditFragment"
        ) {
            ScriptGroupEditFragment()
        }
    }
    private val scriptGroupEditListFragmentHelper: FragmentHelper<ScriptGroupEditListFragment> by lazy {
        FragmentHelper(
            id = R.id.frameLayout, fragmentManager = supportFragmentManager,
            tag = "ScriptGroupEditListFragment"
        ) {
            ScriptGroupEditListFragment()
        }
    }
    private val scriptGroupEditScriptFragmentFragmentHelper: FragmentHelper<ScriptGroupEditScriptFragment> by lazy {
        FragmentHelper(
            id = R.id.frameLayout, fragmentManager = supportFragmentManager,
            tag = "ScriptGroupEditScriptFragment"
        ) {
            ScriptGroupEditScriptFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_script_group_edit)
        initData()
        initFragment()
        _page.value = Page.ScriptGroupEdit
        _page.observe(this) {
            showPage(it)
        }
    }

    private fun initData() {
        if (scriptGroupId == 0) {
            _mLiveData.postValue(
                ScriptGroup(
                    name = "",
                    actionMap = TreeMap(),
                    actionScript = emptyList()
                )
            )
        } else {
            MainScope().launch(Dispatchers.IO) {
                val findBeanById =
                    AppDatabase.getInstance(applicationContext).scriptGroupDao.findBeanById(
                        scriptGroupId
                    )
                scriptGroupBean=findBeanById
                _mLiveData.postValue(findBeanById.toScriptGroup())
            }
        }
    }



    private fun initFragment(){
        scriptGroupEditFragmentHelper.showFragment(now = true)
        scriptGroupEditListFragmentHelper.showFragment(now = true)
        scriptGroupEditScriptFragmentFragmentHelper.showFragment(now = true)
    }

    private fun showPage(page: Page) {
        scriptGroupEditFragmentHelper.hideFragment(now = true)
        scriptGroupEditListFragmentHelper.hideFragment(now = true)
        scriptGroupEditScriptFragmentFragmentHelper.hideFragment(now = true)
        when (page) {
            is Page.ScriptGroupEdit ->{
                scriptGroupEditFragmentHelper.showFragment()
            }
            is Page.ScriptList ->{
                refreshData()
                scriptGroupEditListFragmentHelper.showFragment()
            }
            is Page.ScriptEdit ->{
                scriptGroupEditScriptFragmentFragmentHelper.showFragment()
            }
        }

    }

     fun refreshData() {
        _mLiveData.value?.apply {
            _mLiveData.postValue(copy())
        }
    }

    fun showScriptList(){
        _page.postValue(Page.ScriptList)
    }

     fun goBack(){
        when(_page.value){
            is Page.ScriptGroupEdit ->{
                finish()
            }
            is Page.ScriptList ->{
                _page.postValue(Page.ScriptGroupEdit)
            }
            is Page.ScriptEdit ->{
                _page.postValue(Page.ScriptList)
            }
        }

    }

    override fun onBackPressed() {
        goBack()
    }


    fun onComplete(title: String) {
        val value: ScriptGroup = _mLiveData.value ?: return
        val data=  value.copy(name = title)
        val newData=if (scriptGroupBean!=null) {
            data.toScriptGroupBean(scriptGroupBean!!)
        }else{
            data.toScriptGroupBean()
        }

        GlobalScope.launch (Dispatchers.IO){
            if (scriptGroupId==0){
                AppDatabase.getInstance(applicationContext).scriptGroupDao.insertScriptGroupBean(
                    newData
                )
            }else {
                AppDatabase.getInstance(applicationContext).scriptGroupDao.updateScriptGroupBean(
                    newData
                )
            }
            withContext(Dispatchers.Main){
                finish()
            }
        }
    }


     fun editScript(script: ActionScript) {
        _page.postValue(Page.ScriptEdit)
        scriptGroupEditScriptFragmentFragmentHelper.getFragment().setScript(script)
    }
    fun newScript() {
        val actionScript = ActionScript(name = "", script = ArrayList<String>())
        val value = _mLiveData.value!!
        val arrayList = ArrayList<ActionScript>(value.actionScript)
        arrayList.add(actionScript)
        _mLiveData.postValue(value.copy(actionScript=arrayList))
        editScript(actionScript)

    }

    fun putCmd(name: String, script: ScriptCmdBean) {
        val value: ScriptGroup = _mLiveData.value ?: return
        val treeMap = TreeMap<String, ScriptCmdBean>(value.actionMap)
        treeMap[name]=script
        val copy = value.copy(actionMap = treeMap)
        _mLiveData.postValue(copy)
    }

    fun removeCmd(name: String) {
        val value: ScriptGroup = _mLiveData.value ?: return
        val treeMap = TreeMap<String, ScriptCmdBean>(value.actionMap)
        treeMap.remove(name)
        val copy = value.copy(actionMap = treeMap)
        _mLiveData.postValue(copy)
    }

    fun getActionScriptMsg(key:String):String{
        val value: ScriptGroup = _mLiveData.value ?: return "没有匹配到命令"
        return value.getScriptCmdBean(key)?.actionTypeName?:"没有匹配到命令"
    }

    fun getCmds(): Map<String,ScriptCmdBean> {
        return _mLiveData.value ?.actionMap?: emptyMap()
    }

    fun deleteScript(data: ActionScript) {
        val value= _mLiveData.value ?:return
        val actionScript = ArrayList<ActionScript>(value.actionScript)
        actionScript.remove(data)
        _mLiveData.postValue(value.copy(actionScript = actionScript))
    }


}