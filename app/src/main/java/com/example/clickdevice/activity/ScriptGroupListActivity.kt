package com.example.clickdevice.activity

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Ohuang.ilivedata.MyLiveData
import com.example.clickdevice.R
import com.example.clickdevice.adapter.RecordScriptAdapter
import com.example.clickdevice.adapter.ScriptGroupAdapter
import com.example.clickdevice.bean.ScriptGroup
import com.example.clickdevice.bean.toScriptGroup
import com.example.clickdevice.db.AppDatabase
import com.example.clickdevice.db.RecordScriptBean
import com.example.clickdevice.db.ScriptGroupBean
import com.example.clickdevice.dialog.DialogHelper
import com.example.clickdevice.helper.DesktopIconHelper
import com.example.clickdevice.helper.IOCoroutineContext
import com.example.clickdevice.helper.MainCoroutineContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ScriptGroupListActivity : AppCompatActivity() {
    private var recyclerView: RecyclerView? = null
    private var mAdapter: ScriptGroupAdapter? = null
    private var mData = listOf<ScriptGroupBean>()
    private var dataLiveData: LiveData<List<ScriptGroupBean>>? = null
    private var appDatabase: AppDatabase? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_script_group_list)
        recyclerView = findViewById(R.id.rv_script_list)
        findViewById<View>(R.id.btn_start).setOnClickListener {
            ScriptGroupEditActivity.startActivity(this@ScriptGroupListActivity)
        }

        val linearLayoutManager = LinearLayoutManager(this)

        mAdapter = ScriptGroupAdapter(mData, this)
        recyclerView?.layoutManager = linearLayoutManager
        recyclerView?.adapter = mAdapter

        mAdapter?.clickListener = object : ScriptGroupAdapter.ClickListener {
            override fun delete(recordScriptBean: ScriptGroupBean?) {

                DialogHelper.DeleteDialogShow(
                    this@ScriptGroupListActivity,
                    "删除脚本",
                    "你确定要删除" + recordScriptBean?.name + "?"
                ) {
                    GlobalScope.launch(IOCoroutineContext()) {
                        appDatabase?.scriptGroupDao?.deleteScriptGroupBean(recordScriptBean)
                    }
                }
            }

            override fun edit(recordScriptBean: ScriptGroupBean?) {
                ScriptGroupEditActivity.startActivity(
                    this@ScriptGroupListActivity,
                    recordScriptBean!!.id
                )
            }

            override fun select(recordScriptBean: ScriptGroupBean?) {
                var toScriptGroup = recordScriptBean?.toScriptGroup()
                toScriptGroup.apply {
                    MyLiveData.getInstance().with("ScriptGroup", ScriptGroup::class.java)
                        .postValue(this)
                    startActivity(
                        Intent(
                            this@ScriptGroupListActivity,
                            ScriptGroupPlayActivity::class.java
                        )
                    )
                }

            }

            override fun createDesktop(recordScriptBean: ScriptGroupBean?) {
                Toast.makeText(this@ScriptGroupListActivity, "暂不支持", Toast.LENGTH_SHORT).show()
            }


        }
        GlobalScope.launch(MainCoroutineContext()) {
            appDatabase = AppDatabase.getInstance(this@ScriptGroupListActivity)
            dataLiveData = appDatabase?.scriptGroupDao?.loadLiveDataOfAllScriptGroupBean()
            MainScope().launch {
                dataLiveData?.value?.let {
                    mData = it
                    mAdapter?.setmData(mData)
                }
            }
            dataLiveData?.observe(this@ScriptGroupListActivity) {
                mData = it
                mAdapter?.setmData(mData)
            }
        }
    }


}