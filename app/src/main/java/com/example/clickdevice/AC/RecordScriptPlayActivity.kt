package com.example.clickdevice.AC

import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.Ohuang.ilivedata.MyLiveData
import com.example.clickdevice.MyService
import com.example.clickdevice.PowerKeyObserver
import com.example.clickdevice.PowerKeyObserver.OnPowerKeyListener
import com.example.clickdevice.RecordScriptExecutor
import com.example.clickdevice.bean.RecordScriptCmd
import com.example.clickdevice.databinding.ActivityRecordScriptPlayBinding
import com.example.clickdevice.databinding.WindowBBinding
import com.example.clickdevice.db.RecordScriptBean
import com.example.clickdevice.helper.SmallWindowsHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RecordScriptPlayActivity : AppCompatActivity(), RecordScriptExecutor.RecordScriptInterface {
    private var binding: ActivityRecordScriptPlayBinding? = null
    private lateinit var playSmallWindowsHelper: SmallWindowsHelper
    private var windowBBinding: WindowBBinding? = null
    private var isRun = false
    private val singleThreadExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val recordScriptExecutor = RecordScriptExecutor()
    var data = ArrayList<RecordScriptCmd>()
    var time = 0L
    var count = 0

    private var playRunnable = Runnable {
        forCount(count) {
            if (!isRun) {
                return@forCount true
            }
            playScript()
            recordScriptExecutor.delay(time)
            return@forCount false
        }
        isRun = false
        windowBBinding?.root?.post {
            windowBBinding?.tvWinB?.text = "开始"
        }
    }

    private fun forCount(i: Int, action: (Int) -> Boolean) {
        for (j in 0 until i) {
            val action1 = action(j)
            if (action1) break
        }
    }

    private var powerKeyObserver:PowerKeyObserver?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordScriptPlayBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        initEvent()
        recordScriptExecutor.recordScriptInterface = this
        initPlaySmallWindows()
        powerKeyObserver = PowerKeyObserver(this)
        powerKeyObserver?.startListen() //h开始注册广播
        powerKeyObserver?.setHomeKeyListener(OnPowerKeyListener { isRun=false })
    }


    override fun onDestroy() {
        super.onDestroy()
        powerKeyObserver?.stopListen()
        playSmallWindowsHelper.hide()
    }


    private fun initEvent() {
        binding?.btnScriptOpenScript?.setOnClickListener {
            if (!playSmallWindowsHelper.isShow) {
                showPlayWindow()
            } else {
                playSmallWindowsHelper.hide()
            }
        }
        binding?.btnSelect?.setOnClickListener {
            startActivity(Intent(this, RecordScriptListActivity::class.java))
            playSmallWindowsHelper.hide()
        }
        MyLiveData.getInstance().with("RecordScriptPlay", RecordScriptBean::class.java)
            .observe(this) {
                binding?.tvName?.text = "脚本名称: " + it.name
                val gson = Gson()
                data = gson.fromJson(it.scriptJson,
                    object : TypeToken<List<RecordScriptCmd>>() {}.type)
            }
    }


    private fun showPlayWindow() {
        if (SmallWindowsHelper.requestPermission(this)) {
            if (playSmallWindowsHelper.root == null) {
                playSmallWindowsHelper.attach(windowBBinding?.root!!)
            } else {
                playSmallWindowsHelper.show()
            }
        }

    }

    private fun initPlaySmallWindows() {
        if (MyService.isStart()){
            playSmallWindowsHelper=SmallWindowsHelper(MyService.myService)
            val mLayoutParams = playSmallWindowsHelper.mLayoutParams
            mLayoutParams?.type=WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        }else{
            playSmallWindowsHelper = SmallWindowsHelper(this)
        }

        val mLayoutParams = playSmallWindowsHelper.mLayoutParams
        mLayoutParams?.gravity = Gravity.TOP
        windowBBinding = WindowBBinding.inflate(layoutInflater)
        windowBBinding?.tvWinB?.setOnClickListener {
            if (!isRun) {
                if (!MyService.isStart()){
                    Toast.makeText(
                        this@RecordScriptPlayActivity,
                        "请手动开启辅助功能，若已开启请重启应用再试一次。",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
                val text = binding?.editScriptTime?.text.toString()
                val toString = binding?.editScriptNumber?.text.toString()
                tryCatch{
                    time=text.toLong()
                }
                tryCatch { count=toString.toInt() }

                isRun = true
                singleThreadExecutor.execute(playRunnable)
                windowBBinding?.tvWinB?.text = "停止"
                Toast.makeText(this, "开始", Toast.LENGTH_LONG).show()

            } else {

                isRun = false
                Toast.makeText(this, "停止", Toast.LENGTH_LONG).show()

            }
        }
    }

    private fun playScript() {
        recordScriptExecutor.run(data)
    }

    override fun isRun() = isRun

    override fun preDispatchGesture(x: Int, y: Int) {
        windowBBinding?.root?.post {
            windowBBinding?.tvWinB?.apply {
                if (calcPointRange(this, x, y)) {
                    playNotTouch()
                }
            }
        }
    }

    private fun tryCatch(action:()->Unit){
        try {
            action.invoke()
        }catch (e:Exception){

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


    private fun playNotTouch() {

        var mLayoutParams = playSmallWindowsHelper.mLayoutParams
        mLayoutParams?.flags =
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        playSmallWindowsHelper.mLayoutParams = mLayoutParams!!
        windowBBinding?.tvWinB?.setTextColor(0xff000000.toInt())
    }


    private fun playCanTouch() {
        val mLayoutParams = playSmallWindowsHelper.mLayoutParams
        mLayoutParams?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        playSmallWindowsHelper.mLayoutParams = mLayoutParams!!
        windowBBinding?.tvWinB?.setTextColor(0xffff0000.toInt())
    }

    private fun calcPointRange(view: View, x: Int, y: Int): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return x >= location[0] && x <= location[0] + view.width && y >= location[1] && y <= location[1] + view.height
    }
}