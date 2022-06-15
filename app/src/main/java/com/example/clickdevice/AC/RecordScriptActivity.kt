package com.example.clickdevice.AC

import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.clickdevice.MyService
import com.example.clickdevice.bean.RecordScriptCmd
import com.example.clickdevice.databinding.ActivityRecordScriptBinding
import com.example.clickdevice.databinding.WindowCanvesBinding
import com.example.clickdevice.helper.SmallWindowsHelper
import com.example.clickdevice.view.RecordTouchView
import com.example.clickdevice.vm.RecordScriptViewModel

class RecordScriptActivity : AppCompatActivity() {


    private lateinit var smallWindowsHelper: SmallWindowsHelper
    private lateinit var playSmallWindowsHelper: SmallWindowsHelper
    private var smallWindowBinding: WindowCanvesBinding? = null
    private var binding: ActivityRecordScriptBinding? = null
    private var viewModel: RecordScriptViewModel? = null

    private var runnable1: Runnable? = null
    private var runnable2: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordScriptBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        viewModel = ViewModelProvider(this)[RecordScriptViewModel::class.java]
        binding?.btnOpen?.setOnClickListener {
            if (MyService.isStart()) {
                showSmallWindows()
            } else {
                Toast.makeText(this, "请手动开启辅助功能，若已开启请重启应用再试一次。", Toast.LENGTH_LONG)
                    .show()
            }
        }

        initSmallWindows()
        initPlaySmallWindows()
    }

    private fun initPlaySmallWindows() {
        playSmallWindowsHelper = SmallWindowsHelper(this)

    }

    private fun initSmallWindows() {
        smallWindowsHelper = SmallWindowsHelper(this)
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
                    smallWindowBinding?.tvStart?.text = "停止"
                    stopRecord()
                    binding?.root?.removeCallbacks(runnable1)
                    binding?.root?.removeCallbacks(runnable2)
                } else {
                    smallWindowBinding?.tvStart?.text = "开始"
                    startRecord()
                }

            }
        }

        smallWindowBinding?.tvClose?.setOnClickListener {
            viewModel?.stopRecord()
            hideSmallWindows()
        }
        smallWindowBinding?.tvHide?.setOnClickListener {
            smallWindowBinding?.layout1?.visibility = View.GONE
            binding?.root?.postDelayed({
                smallWindowBinding?.layout1?.visibility = View.VISIBLE
            }, 3000)
        }

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
        var mLayoutParams = smallWindowsHelper.mLayoutParams
        mLayoutParams?.flags =
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        smallWindowsHelper.mLayoutParams = mLayoutParams!!
        smallWindowBinding?.recordTouchView?.setBackgroundColor(0x30805000)
    }

    private fun canTouch() {
        smallWindowBinding?.recordTouchView?.isEnabled = true
        val mLayoutParams = smallWindowsHelper.mLayoutParams
        mLayoutParams?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        smallWindowsHelper.mLayoutParams = mLayoutParams!!
        smallWindowBinding?.recordTouchView?.setBackgroundColor(0x30005080)
    }


    private fun showSmallWindows() {
        if (SmallWindowsHelper.requestPermission(this)) {
            if (smallWindowsHelper.root == null) {
                smallWindowsHelper.attach(smallWindowBinding?.root!!)
                val mLayoutParams = smallWindowsHelper.mLayoutParams
                mLayoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
                mLayoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
                smallWindowsHelper.mLayoutParams = mLayoutParams
            } else {
                smallWindowsHelper.show()
            }
            canTouch()
        }

    }

    private fun hideSmallWindows() {
        smallWindowsHelper.hide()
        playSmallWindowsHelper.hide()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideSmallWindows()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        SmallWindowsHelper.onActivityResult(this, requestCode, resultCode, data)
    }
}