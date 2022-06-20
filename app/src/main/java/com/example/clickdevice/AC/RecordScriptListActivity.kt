package com.example.clickdevice.AC

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clickdevice.R
import com.example.clickdevice.adapter.RecordScriptAdapter
import com.example.clickdevice.db.AppDatabase
import com.example.clickdevice.db.RecordScriptBean
import com.example.clickdevice.dialog.DialogHelper
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
                var dialog: Dialog? = null
                dialog = DialogHelper.DeleteDialogShow(
                    this@RecordScriptListActivity, "删除脚本", "你确定要删除" + recordScriptBean?.name + "?"
                ) {
                    GlobalScope.launch(IOCoroutineContext()) {
                        appDatabase?.recordScriptDao?.deleteRecordScriptBean(recordScriptBean)
                    }
                    dialog?.dismiss()
                }

            }

            override fun edit(recordScriptBean: RecordScriptBean?) {

            }

            override fun select(recordScriptBean: RecordScriptBean?) {

            }

        }
        GlobalScope.launch(MainCoroutineContext()) {
            appDatabase = AppDatabase.getInstance(this@RecordScriptListActivity)
            dataLiveData = appDatabase?.recordScriptDao?.loadLiveDataOfAllRecordScriptBean()
            MainScope().launch {
                dataLiveData?.value?.let {
                    mData=it
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