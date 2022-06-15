package com.example.clickdevice.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.clickdevice.bean.Bean
import com.example.clickdevice.bean.RecordScriptCmd

class RecordTouchView : View {

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs, 0)


    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var paint = Paint()

    private fun initPaint() {
        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.setColor(Color.GREEN)
        paint.style = Paint.Style.STROKE
        paint.setStrokeWidth(5f)
    }

    private var path: Path? = null
    private var scriptPath: Path? = null
    private var data: MutableList<Bean>? = null
    private var enable = true


    override fun onDraw(canvas: Canvas?) {
        initPaint()
        path?.apply {
            canvas?.drawPath(this, paint)
        }
    }

    var scriptListener: ScriptListener? = null
    private var downTime = 0L
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!enable) {
             return false
        }
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                downTime = SystemClock.uptimeMillis()
                scriptListener?.onActionDown()
                path = Path()
                scriptPath = Path()
                data = ArrayList()
                path!!.moveTo(event.x, event.y)
                scriptPath!!.moveTo(event.rawX, event.rawY)
                data?.add(Bean(event.rawX.toInt(), event.rawY.toInt()))
            }
            MotionEvent.ACTION_MOVE -> {
                path?.lineTo(event.x, event.y)
                scriptPath?.lineTo(event.rawX, event.rawY)
                data?.add(Bean(event.rawX.toInt(), event.rawY.toInt()))
            }
            MotionEvent.ACTION_UP -> {
                path?.lineTo(event.x, event.y)
                scriptPath?.lineTo(event.rawX, event.rawY)
                data?.add(Bean(event.rawX.toInt(), event.rawY.toInt()))
                scriptListener?.apply {
                    val createGestureCMD = RecordScriptCmd.createGestureCMD(
                        data,
                        (SystemClock.uptimeMillis() - downTime).toInt()
                    )
                    onUpdate(createGestureCMD, scriptPath!!)
                }
            }
        }
        invalidate()
        return true
    }

    interface ScriptListener {
        fun onActionDown() {

        }

        fun onUpdate(recordScriptCmd: RecordScriptCmd, path: Path)
    }

}