package com.example.clickdevice.activity

import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.Ohuang.ilivedata.MyLiveData
import com.example.clickdevice.MyService
import com.example.clickdevice.RecordScriptExecutor
import com.example.clickdevice.adapter.RecordCMDAdapter
import com.example.clickdevice.bean.RecordScriptCmd
import com.example.clickdevice.databinding.ActivityRecordScriptBinding
import com.example.clickdevice.databinding.WindowBBinding
import com.example.clickdevice.databinding.WindowCanvesBinding
import com.example.clickdevice.db.RecordScriptBean
import com.example.clickdevice.helper.IOCoroutineContext
import com.example.clickdevice.helper.SmallWindowsHelper
import com.example.clickdevice.view.RecordTouchView
import com.example.clickdevice.vm.RecordScriptViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class RecordScriptActivity : AppCompatActivity(), RecordScriptExecutor.RecordScriptInterface {


    private  var smallWindowsHelper: SmallWindowsHelper?=null
    private  var playSmallWindowsHelper: SmallWindowsHelper?=null
    private var smallWindowBinding: WindowCanvesBinding? = null
    private var binding: ActivityRecordScriptBinding? = null
    private var viewModel: RecordScriptViewModel? = null
    private var windowBBinding: WindowBBinding? = null

    private var runnable1: Runnable? = null
    private var runnable2: Runnable? = null
    private var isRun = false
    private var recordCMDAdapter:RecordCMDAdapter?=null

    private var playRunnable = Runnable {

        viewModel?.playScript()
        isRun = false
        windowBBinding?.root?.post {
            windowBBinding?.tvWinB?.text = "开始"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordScriptBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        viewModel = ViewModelProvider(this)[RecordScriptViewModel::class.java]
        initEvent()
        initSmallWindows()
        initPlaySmallWindows()
        initRv()
        var booleanExtra = intent.getBooleanExtra("isEdit", false)
        if (booleanExtra) {
            MyLiveData.getInstance().with("RecordScriptEdit", RecordScriptBean::class.java)
                .observe(this) {
                    viewModel!!.recordScriptBean = it
                    val name = it.name
                    binding?.editName?.setText(name)
                    val gson = Gson()
                    viewModel!!.data = gson.fromJson(it.scriptJson,
                        object : TypeToken<List<RecordScriptCmd>>() {}.type)
                    recordCMDAdapter!!.setmData(viewModel!!.data)
                }
        }
    }

    private fun initRv() {
        binding?.rvScriptEdit?.layoutManager=LinearLayoutManager(this)
        recordCMDAdapter= RecordCMDAdapter(viewModel!!.data,this)
        viewModel?.recordCMDAdapter=recordCMDAdapter
        binding?.rvScriptEdit?.adapter=recordCMDAdapter
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun initEvent() {
        viewModel?.recordScriptExecutor?.recordScriptInterface = this
        binding?.btnOpen?.setOnClickListener {
            if (MyService.isStart()) {
                showSmallWindows()
                playSmallWindowsHelper?.hide()
                isRun = false
            } else {
                Toast.makeText(this, "请手动开启辅助功能，若已开启请重启应用再试一次。", Toast.LENGTH_LONG)
                    .show()
            }
        }
        binding?.btnPlay?.setOnClickListener {
            if (!playSmallWindowsHelper?.isShow!!) {
                showPlayWindow()
                closeSmallWindow()
            } else {
                playSmallWindowsHelper?.hide()
            }
        }
        binding?.btnBack?.setOnClickListener {
            finish()
        }

        binding?.btnComplete?.setOnClickListener {
            var editName = binding?.editName?.text
            editName?.run {
                GlobalScope.launch(IOCoroutineContext()) {
                    viewModel?.saveScript(this@RecordScriptActivity, editName.toString())
                    MainScope().launch {
                        finish()
                        Toast.makeText(this@RecordScriptActivity, "保存成功", Toast.LENGTH_LONG).show()
                    }
                }
            }
            editName ?: run {
                Toast.makeText(this@RecordScriptActivity, "请输入脚本名", Toast.LENGTH_LONG).show()
            }

        }
    }

    private fun initPlaySmallWindows() {
        playSmallWindowsHelper = SmallWindowsHelper(this)
        val mLayoutParams = playSmallWindowsHelper?.mLayoutParams



        mLayoutParams?.gravity = Gravity.TOP
        windowBBinding = WindowBBinding.inflate(layoutInflater)
        windowBBinding?.tvWinB?.setOnClickListener {
            if (!isRun) {

                isRun = true
                viewModel?.singleThreadExecutor?.execute(playRunnable)
                windowBBinding?.tvWinB?.text = "停止"
                Toast.makeText(this, "开始", Toast.LENGTH_LONG).show()

            } else {

                isRun = false
                Toast.makeText(this, "停止", Toast.LENGTH_LONG).show()

            }
        }
    }


    private fun initSmallWindows() {
        if (!MyService.isStart()){
            return
        }
        smallWindowsHelper = SmallWindowsHelper(MyService.myService)
        val mLayoutParams = smallWindowsHelper?.mLayoutParams
        var type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

        mLayoutParams?.type=type

        smallWindowBinding = WindowCanvesBinding.inflate(layoutInflater)
        smallWindowBinding?.recordTouchView?.scriptListener =
            object : RecordTouchView.ScriptListener {

                override fun onActionDown() {
                    viewModel?.addDelayTime()
                }


                override fun onUpdate(recordScriptCmd: RecordScriptCmd, path: Path) {
                    notTouch()
                    if (MyService.isStart()) {
                        dispatchGesturePath(path, recordScriptCmd)
                        viewModel?.addRecordScriptCmd(recordScriptCmd)
                    }
                }
            }

        smallWindowBinding?.tvStart?.setOnClickListener {
            viewModel?.apply {
                if (isRecord) {
                    smallWindowBinding?.tvStart?.text = "开始"
                    stopRecord()
                    binding?.root?.removeCallbacks(runnable1)
                    binding?.root?.removeCallbacks(runnable2)
                } else {
                    smallWindowBinding?.tvStart?.text = "停止"
                    startRecord()
                }

            }
        }

        smallWindowBinding?.tvClose?.setOnClickListener {
            closeSmallWindow()
        }
        smallWindowBinding?.tvHide?.setOnClickListener {
            smallWindowBinding?.layout1?.visibility = View.GONE
            binding?.root?.postDelayed({
                smallWindowBinding?.layout1?.visibility = View.VISIBLE
            }, 3000)
        }

    }

    private fun closeSmallWindow() {
        binding?.root?.removeCallbacks(runnable1)
        binding?.root?.removeCallbacks(runnable2)
        smallWindowBinding?.tvStart?.text = "开始"
        viewModel?.stopRecord()
        smallWindowsHelper?.hide()
    }

    private fun dispatchGesturePath(
        path: Path,
        recordScriptCmd: RecordScriptCmd
    ) {
        binding?.root?.removeCallbacks(runnable1)
        binding?.root?.removeCallbacks(runnable2)
        runnable2 = Runnable {
            canTouch()
            viewModel?.postLastTime()
        }
        runnable1 = Runnable {
            MyService.myService.dispatchGesture(path, recordScriptCmd.duration)
            binding?.root?.postDelayed(
                runnable2,
                recordScriptCmd.duration.toLong()
            )
        }
        binding?.root?.postDelayed(runnable1, 100)
    }


    private fun notTouch() {
        smallWindowBinding?.recordTouchView?.isEnabled = false
        var mLayoutParams = smallWindowsHelper?.mLayoutParams
        mLayoutParams?.flags =
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        smallWindowsHelper?.mLayoutParams = mLayoutParams!!

        smallWindowBinding?.recordTouchView?.setBackgroundColor(0x30805000)
    }

    private fun canTouch() {
        smallWindowBinding?.recordTouchView?.isEnabled = true
        val mLayoutParams = smallWindowsHelper?.mLayoutParams
        mLayoutParams?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        smallWindowsHelper?.mLayoutParams = mLayoutParams!!

        smallWindowBinding?.recordTouchView?.setBackgroundColor(0x30005080)
    }

    private fun playNotTouch() {

        var mLayoutParams = playSmallWindowsHelper?.mLayoutParams
        mLayoutParams?.flags =
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        playSmallWindowsHelper?.mLayoutParams = mLayoutParams!!
        windowBBinding?.tvWinB?.setTextColor(0xff000000.toInt())
    }


    private fun playCanTouch() {
        val mLayoutParams = playSmallWindowsHelper?.mLayoutParams
        mLayoutParams?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        playSmallWindowsHelper?.mLayoutParams = mLayoutParams!!
        windowBBinding?.tvWinB?.setTextColor(0xffff0000.toInt())
    }

    private fun showSmallWindows() {
        if (SmallWindowsHelper.requestPermission(this)) {
            if (smallWindowsHelper?.root == null) {
                smallWindowsHelper?.attach(smallWindowBinding?.root!!)
                val mLayoutParams = smallWindowsHelper?.mLayoutParams
                mLayoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
                mLayoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
                smallWindowsHelper?.mLayoutParams = mLayoutParams
            } else {
                smallWindowsHelper?.show()
            }
            canTouch()
        }

    }


    private fun showPlayWindow() {

        if (SmallWindowsHelper.requestPermission(this)) {
            if (playSmallWindowsHelper?.root == null) {
                playSmallWindowsHelper?.attach(windowBBinding?.root!!)
            } else {
                playSmallWindowsHelper?.show()
            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        smallWindowsHelper?.hide()
        playSmallWindowsHelper?.hide()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        SmallWindowsHelper.onActivityResult(this, requestCode, resultCode, data)
    }

    override fun isRun(): Boolean {
        return isRun
    }

    override fun preDispatchGesture(x: Int, y: Int) {
        windowBBinding?.root?.post {
            windowBBinding?.tvWinB?.apply {
                if (calcPointRange(this, x, y)) {
                    playNotTouch()
                }
            }
        }


    }

    override fun dispatchGesture(position: Int, path: Path, duration: Int) {
        if (MyService.isStart()) {
            MyService.myService.dispatchGesture(path, duration)
        }
    }

    override fun endDispatchGesture() {
        windowBBinding?.root?.post {
            playCanTouch()
        }
    }

    private fun calcPointRange(view: View, x: Int, y: Int): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return x >= location[0] && x <= location[0] + view.width && y >= location[1] && y <= location[1] + view.height
    }
}