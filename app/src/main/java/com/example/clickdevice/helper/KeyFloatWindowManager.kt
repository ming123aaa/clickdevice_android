package com.example.clickdevice.helper


import android.content.Context
import android.content.SharedPreferences
import android.graphics.Matrix
import android.graphics.Path
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.Observer
import com.example.clickdevice.MyService
import com.example.clickdevice.R
import com.example.clickdevice.RecordScriptExecutor
import com.example.clickdevice.ScriptExecutor
import com.example.clickdevice.SmallWindowView
import com.example.clickdevice.activity.LauncherScriptActivity
import com.example.clickdevice.bean.Bean
import com.example.clickdevice.bean.ScriptRunParams
import com.example.clickdevice.bean.getScript
import com.example.clickdevice.bean.getScriptCmd
import com.example.clickdevice.bean.getScriptForName
import com.example.clickdevice.bean.toScriptGroup
import com.example.clickdevice.db.AppDatabase
import com.example.clickdevice.db.KeyBindingBean
import com.example.clickdevice.helper.KeyFloatWindowManager.FloatWindowInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.apply
import androidx.core.content.edit


class KeyFloatWindowManager(var context: Context) {
    private val MAX_WINDOWS = 5
    private val PREFS_NAME = "key_float_window_prefs"
    private val KEY_SHOWN_IDS = "shown_window_ids"

    private val floatWindows = SnapshotStateMap<Int, FloatWindowInfo>()
    private var isRunning = false
    private var runningKeyId = -1

    private val singleThreadExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var pkgNameNow = ""
    var thisPkgName = ""

    var checkAppChange = false

    val observer: Observer<String> = Observer<String> { s ->
        pkgNameNow = s ?: ""
        if (checkAppChange) {
            if (pkgNameNow != thisPkgName) {
                if (isRunning) {
                    stopRunningScript()
                }
            }
        }
    }


    data class FloatWindowInfo(
        val binding: KeyBindingBean,
        val windowView: SmallWindowView,
        val textView: TextView,
        val tv_stop: TextView,
        val scriptRunParams: ScriptRunParams,
        val smallWindowsHelper: SmallWindowsHelper
    )


    init {
        MyService.myService?.pkgNameMutableLiveData?.observeForever(observer)
    }

    fun isShow(id: Int): Boolean {
        return floatWindows.containsKey(id)
    }

     fun saveShownIds() {
        val ids = floatWindows.keys.joinToString(",")
        prefs.edit(commit = true) { putString(KEY_SHOWN_IDS, ids) }
    }

    private fun getSavedShownIds(): Set<Int> {
        val idsStr = prefs.getString(KEY_SHOWN_IDS, "") ?: ""
        if (idsStr.isBlank()) return emptySet()
        return idsStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun restoreWindows() {
        val savedIds = getSavedShownIds()
        if (savedIds.isEmpty()) return
        GlobalScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            savedIds.forEach { id ->
                val bean = db.getKeyBindingDao().findBeanById(id)
                if (bean != null) {
                    withContext(Dispatchers.Main) {
                        if (!floatWindows.containsKey(id)) {
                            showWindow(bean)
                        }
                    }
                }
            }
        }
    }


    fun showWindow(binding: KeyBindingBean) {
        if (floatWindows.size >= MAX_WINDOWS) {
            Toast.makeText(context, "最多只能显示5个悬浮窗按钮", Toast.LENGTH_SHORT).show()
            return
        }

        if (floatWindows.containsKey(binding.id)) {
            return
        }

        val windowView =
            LayoutInflater.from(context).inflate(R.layout.window_key, null) as SmallWindowView
        val textView = windowView.findViewById<TextView>(R.id.tv_key)
        val tv_stop = windowView.findViewById<TextView>(R.id.tv_stop)
        val fl_key = windowView.findViewById<FrameLayout>(R.id.fl_key)
        textView.text = binding.keyName
        textView.setTextColor(binding.textColor)
        textView.textSize = binding.textSize.toFloat()
        tv_stop.textSize = binding.textSize.toFloat()

    

        fl_key.setOnTouchClick({
            handleKeyClick(binding.id)
        }) {
            if (isRunning && binding.id == runningKeyId) {
                handleKeyClick(binding.id)
                return@setOnTouchClick false
            }
            return@setOnTouchClick true
        }

        try {
            val smallWindowView = SmallWindowsHelper(context).apply {
                windowView.setWm(mWindowManager)
                windowView.enableMove = !binding.windowLocked
                windowView.wmParams = mLayoutParams
                attach(windowView)
                
                if (binding.windowX != 0 || binding.windowY != 0) {
                    updateLayoutParams {
                        x = binding.windowX
                        y = binding.windowY
                    }
                }
            }

            floatWindows[binding.id] = FloatWindowInfo(
                binding,
                windowView,
                textView,
                tv_stop,
                ScriptRunParams.fromJson(binding.scriptParams),
                smallWindowView
            )
            saveShownIds()
        } catch (e: Exception) {
            Toast.makeText(context, "无法显示悬浮窗", Toast.LENGTH_SHORT).show()
        }
    }

    fun hideWindow(keyId: Int) {
        val info = floatWindows.remove(keyId)
        if (keyId == runningKeyId) {
            stopRunningScript()
        }
        info?.apply {
            try {
                smallWindowsHelper.hide()
            } catch (e: Exception) {
            }
        }
        saveShownIds()
    }

    fun setWindowMoveEnable(keyId: Int, isMove: Boolean) {
        if (floatWindows.containsKey(keyId)) {
            floatWindows[keyId]!!.windowView.enableMove = isMove
        }
    }

    fun setWindowXY(keyId: Int, x0: Int, y0: Int) {
        if (floatWindows.containsKey(keyId)) {
            floatWindows[keyId]!!.smallWindowsHelper.updateLayoutParams{
                x = x0
                y = y0
            }
            
        }
    }

    fun getWindowXY(keyId: Int): Array<Int> {
        if (floatWindows.containsKey(keyId)) {
            val layoutParams = floatWindows[keyId]!!.smallWindowsHelper.mLayoutParams
            return arrayOf(layoutParams.x, layoutParams.y)
        }
        return arrayOf(0, 0)
    }


    fun hideAllWindows(isSave: Boolean = true) {
        stopRunningScript()
        if (isSave){
            saveShownIds()
        }
        floatWindows.values.forEach { info ->
            try {
                info.smallWindowsHelper.hide()
            } catch (e: Exception) {
            }
        }
        floatWindows.clear()
        if (!isSave) {
            saveShownIds()
        }

    }



    private fun handleKeyClick(keyId: Int) {
        if (!MyService.isStart()) {
            Toast.makeText(context, "请先开启辅助功能", Toast.LENGTH_SHORT).show()
            return
        }

        val info = floatWindows[keyId]
        info ?: return
        if (isRunning) {
            isRunning = false
        } else {
            if (runningKeyId == -1) {
                isRunning = true
                runningKeyId = info.binding.id
                singleThreadExecutor.execute {
                    if (isRunning) {
                        startScript(info, info.binding, info.textView)
                    }
                }
            }
        }


    }

    private fun createScriptInterface(info: FloatWindowInfo): KeyScriptInterface {
        return KeyScriptInterface(info.textView, info, {
            return@KeyScriptInterface isRunning && runningKeyId == info.binding.id
        })
    }

    private fun runScript(info: FloatWindowInfo, call: () -> Unit) {
        var j = 0
        val count = info.scriptRunParams.clickCount
        val time = info.scriptRunParams.intervalTime

        while (isRunning && (count <= 0 || j < count)) {
            call()
            var i2 = 0
            while (i2 < time / 100 && isRunning) {
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    break
                }
                i2++
            }
            if (isRunning) {
                try {
                    Thread.sleep((time % 100).toLong())
                } catch (e: InterruptedException) {
                    break
                }
            }
            j++
        }
    }


    private fun startScript(info: FloatWindowInfo, binding: KeyBindingBean, textView: TextView) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                runningKeyId = binding.id
                withContext(Dispatchers.Main) {
                    floatWindows.values.forEach { info ->
                        info.textView.text = info.binding.keyName
                        info.textView.visibility = View.VISIBLE
                        info.tv_stop.visibility = View.INVISIBLE
                        playNotTouch(info)
                    }
                    textView.visibility = View.INVISIBLE
                    info.tv_stop.visibility = View.VISIBLE
                    playCanTouch(info)
                }
                if (info.scriptRunParams.checkAppChange) {
                    checkAppChange = true
                    pkgNameNow = thisPkgName
                } else {
                    checkAppChange = false
                }


                val scriptId = binding.scriptId
                when (binding.scriptType) {
                    LauncherScriptActivity.TYPE_SCRIPT -> {
                        startScriptType(scriptId, info)
                    }

                    LauncherScriptActivity.TYPE_RECORD_SCRIPT -> {
                        startRecordType(scriptId, info)

                    }

                    LauncherScriptActivity.TYPE_SCRIPT_GROUP -> {
                        startGroupType(scriptId, info)
                    }

                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "脚本未设置", Toast.LENGTH_SHORT).show()
                        }
                    }
                }


            } catch (e: Throwable) {

            } finally {
                withContext(Dispatchers.Main) {
                    stopRunningScript()
                }
            }


        }


    }

    private suspend fun startGroupType(
        scriptId: Int,
        info: FloatWindowInfo
    ) {
        val scriptGroup = runCatching {
            AppDatabase.getInstance(context).scriptGroupDao.findBeanById(scriptId)
        }
            .getOrNull()
        if (scriptGroup != null) {
            if (isRunning) {
                val scriptCmd =
                    scriptGroup.toScriptGroup().getScriptForName(info.scriptRunParams.actionName)
                ScriptExecutor(createScriptInterface(info)).apply {
                    runScript(info) {
                        run(scriptCmd)
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "运行失败,请检查脚本是否存在", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun startRecordType(
        scriptId: Int,
        info: FloatWindowInfo
    ) {
        val recordScript = runCatching {
            AppDatabase.getInstance(context).recordScriptDao.findBeanById(scriptId)
        }
            .getOrNull()
        if (recordScript != null) {
            if (isRunning) {
                val scriptCmd = recordScript.getScript()
                RecordScriptExecutor().apply {
                    recordScriptInterface = createScriptInterface(info)
                    delayCoefficient =
                        if (info.scriptRunParams.speed in 0.25..5.0) info.scriptRunParams.speed else 1.0


                    runScript(info) {
                        run(scriptCmd)
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "运行失败,请检查脚本是否存在", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun startScriptType(
        scriptId: Int,
        info: FloatWindowInfo
    ) {
        val scriptData = runCatching {
            AppDatabase.getInstance(context).scriptDao.findBeanById(scriptId)
        }.getOrNull()
        if (scriptData != null) {
            if (isRunning) {
                val scriptCmd = scriptData.getScriptCmd()
                ScriptExecutor(createScriptInterface(info)).apply {
                    runScript(info) {
                        run(scriptCmd)
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "运行失败,请检查脚本是否存在", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun stopRunningScript() {
        isRunning = false
        checkAppChange = false
        runningKeyId = -1

        GlobalScope.launch(Dispatchers.Main) {
            floatWindows.values.forEach { info ->
                info.textView.text = info.binding.keyName
                info.textView.visibility = View.VISIBLE
                info.tv_stop.visibility = View.INVISIBLE
                playCanTouch(info)
            }
        }

    }


    private fun playNotTouch(floatWindowInfo: FloatWindowInfo) {
        floatWindowInfo.smallWindowsHelper.setTouchEnable(false)
    }

    private fun playCanTouch(floatWindowInfo: FloatWindowInfo) {
        floatWindowInfo.smallWindowsHelper.setTouchEnable(true)
    }


    fun isScriptRunning(): Boolean {
        return isRunning
    }

    fun getRunningKeyId(): Int {
        return runningKeyId
    }


    fun destroy() {
        hideAllWindows()
        MyService.myService?.pkgNameMutableLiveData?.observeForever(observer)

    }
}

class KeyScriptInterface(
    val tvWinB: TextView,
    val floatWindowInfo: FloatWindowInfo,
    val isRunning: () -> Boolean,
) : RecordScriptExecutor.RecordScriptInterface, ScriptExecutor.ScriptInterFace {
    override fun isRun() = this.isRunning()

    val scriptRunParams: ScriptRunParams
        get() = floatWindowInfo.scriptRunParams


    override fun preDispatchGesture(x: Int, y: Int) {
        tvWinB.post {
            tvWinB.apply {
                val xc =
                    if (scriptRunParams.xCoefficient in 0.25f..5.0f) scriptRunParams.xCoefficient else 1.0f
                val yc =
                    if (scriptRunParams.yCoefficient in 0.25f..5.0f) scriptRunParams.yCoefficient else 1.0f
                val sx = (x * xc).toInt()
                val sy = (y * yc).toInt()

                if (calcPointRange(this, sx, sy)) {
                    playNotTouch()
                }
            }
        }
    }

    override fun dispatchGesture(position: Int, path: Path, duration: Int) {
         if (!isRun) {
                return 
            }
        if (MyService.isStart()) {
            val xc =
                if (scriptRunParams.xCoefficient in 0.25f..5.0f) scriptRunParams.xCoefficient else 1.0f
            val yc =
                if (scriptRunParams.yCoefficient in 0.25f..5.0f) scriptRunParams.yCoefficient else 1.0f
            if (xc != 1.0f || yc != 1.0f) {
                val matrix = Matrix()
                matrix.setScale(xc, yc)
                val scaledPath = Path()
                path.transform(matrix, scaledPath)
                MyService.myService.dispatchGesture(scaledPath, duration)
            } else {
                MyService.myService.dispatchGesture(path, duration)
            }
        }
    }

    override fun endDispatchGesture() {
        tvWinB.post {
            playCanTouch()
        }
    }

    private fun playNotTouch() {
        floatWindowInfo.smallWindowsHelper.setTouchEnable(false)
    }

    private fun playCanTouch() {
        floatWindowInfo.smallWindowsHelper.setTouchEnable(true)
    }

    private fun calcPointRange(view: View, x: Int, y: Int): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return x >= location[0] && x <= location[0] + view.width && y >= location[1] && y <= location[1] + view.height
    }

    fun sleep(time: Long): Boolean {
        if (time <= 0) {
            return false
        }
         if (!isRun) {
                return true
            }

        val count = time / 10
        val t = time % 10
        Thread.sleep(t)
        for (i in 0 until count) {
            if (!isRun) {
                return true
            }
            Thread.sleep(10)
        }
        return false
    }


    override fun delayedCmd(time: Int) {
        if (time <= 0) {
            return
        }
       if (!isRun) {
                return 
            }
        val adjustedTime = (time * scriptRunParams.speed).toLong()
        sleep(adjustedTime)
    }


    override fun clickCMD(x0: Int, y0: Int, duration: Int) {
          if (!isRun) {
                return 
            }
        preDispatchGesture(x0, y0)
        sleep(100)


        var newDuration = (duration * scriptRunParams.speed).toInt()
        newDuration = when {
            newDuration > 30000 -> 30000
            newDuration < 30 -> 30
            else -> newDuration
        }

        val xc =
            if (scriptRunParams.xCoefficient in 0.25f..5.0f) scriptRunParams.xCoefficient else 1.0f
        val yc =
            if (scriptRunParams.yCoefficient in 0.25f..5.0f) scriptRunParams.yCoefficient else 1.0f
        val sx = (x0 * xc).toInt()
        val sy = (y0 * yc).toInt()

        if (MyService.isStart()) {
            try {
                MyService.myService.dispatchGestureClick(sx.toFloat(), sy.toFloat(), newDuration)
            } catch (e: Throwable) {
            }
        }
        sleep(newDuration.toLong())
        sleep(100)
        endDispatchGesture()
    }


    override fun gestureCMD(
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
        duration: Int
    ) {
        preDispatchGesture(x0, y0)
        sleep(100)
        val createPath = createPath(listOf<Bean>(Bean(x0, y0), Bean(x1, y1)))
        var newDuration = (duration * scriptRunParams.speed).toInt()
        newDuration = when {
            newDuration > 30000 -> 30000
            newDuration < 30 -> 30
            else -> newDuration
        }
        try {
            dispatchGesture(0, createPath, newDuration)
            sleep(newDuration.toLong())
        } catch (e: Throwable) {

        }

        sleep(100)
        endDispatchGesture()
    }

    private fun createPath(data: List<Bean>): Path {
        val path = Path()
        val bean = data[0]
        path.moveTo(bean.x.toFloat(), bean.y.toFloat())
        for (i in 1 until data.size) {
            val bean2 = data[i]
            path.lineTo(bean2.x.toFloat(), bean2.y.toFloat())
        }
        return path
    }


}