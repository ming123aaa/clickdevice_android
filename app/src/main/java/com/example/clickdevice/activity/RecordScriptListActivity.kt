package com.example.clickdevice.activity

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Ohuang.ilivedata.MyLiveData
import com.example.clickdevice.R
import com.example.clickdevice.adapter.RecordScriptAdapter
import com.example.clickdevice.db.AppDatabase
import com.example.clickdevice.db.RecordScriptBean
import com.example.clickdevice.dialog.DialogHelper
import com.example.clickdevice.helper.DesktopIconHelper
import com.example.clickdevice.helper.IOCoroutineContext
import com.example.clickdevice.helper.MainCoroutineContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class RecordScriptListActivity : AppCompatActivity() {
    private var recyclerView: RecyclerView? = null
    private var recordScriptAdapter: RecordScriptAdapter? = null
    private var mData = listOf<RecordScriptBean>()
    private var dataLiveData: LiveData<List<RecordScriptBean>>? = null
    private var appDatabase: AppDatabase? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_script_list)
        recyclerView = findViewById(R.id.rv_script_list)
        findViewById<View>(R.id.btn_start).setOnClickListener {
            startActivity(Intent(this@RecordScriptListActivity, RecordScriptActivity::class.java))
        }

        val linearLayoutManager = LinearLayoutManager(this)

        recordScriptAdapter = RecordScriptAdapter(mData, this)
        recyclerView?.layoutManager = linearLayoutManager
        recyclerView?.adapter = recordScriptAdapter

        recordScriptAdapter?.clickListener = object : RecordScriptAdapter.ClickListener {
            override fun delete(recordScriptBean: RecordScriptBean?) {

             DialogHelper.DeleteDialogShow(
                    this@RecordScriptListActivity, "删除脚本", "你确定要删除" + recordScriptBean?.name + "?"
                ) {
                    GlobalScope.launch(IOCoroutineContext()) {
                        appDatabase?.recordScriptDao?.deleteRecordScriptBean(recordScriptBean)
                    }

                }

            }

            override fun edit(recordScriptBean: RecordScriptBean?) {
                MyLiveData.getInstance().with("RecordScriptEdit", RecordScriptBean::class.java)
                    .postValue(recordScriptBean)
                var intent = Intent(this@RecordScriptListActivity, RecordScriptActivity::class.java)
                intent.putExtra("isEdit",true)
                startActivity(intent)
            }

            override fun select(recordScriptBean: RecordScriptBean?) {
                MyLiveData.getInstance().with("RecordScriptPlay", RecordScriptBean::class.java)
                    .postValue(recordScriptBean)
                startActivity(Intent(this@RecordScriptListActivity,RecordScriptPlayActivity::class.java))
            }

            override fun createDesktop(recordScriptBean: RecordScriptBean?) {
                DesktopIconHelper.addShortcut(this@RecordScriptListActivity,recordScriptBean!!)
            }

        }
        GlobalScope.launch(MainCoroutineContext()) {
            appDatabase = AppDatabase.getInstance(this@RecordScriptListActivity)
            dataLiveData = appDatabase?.recordScriptDao?.loadLiveDataOfAllRecordScriptBean()
            MainScope().launch {
                dataLiveData?.value?.let {
                    mData = it
                    recordScriptAdapter?.setmData(mData)
                }
            }
            dataLiveData?.observe(this@RecordScriptListActivity) {
                mData = it
                recordScriptAdapter?.setmData(mData)
            }
        }
    }


}