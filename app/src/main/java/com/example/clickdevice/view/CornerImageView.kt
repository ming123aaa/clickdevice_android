package com.example.clickdevice.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.os.Build
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView


/**
 * @author sean
 */
class CornerImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatImageView(context, attrs, defStyleAttr) {
    private var mWidth = 0f
    private var mHeight = 0f
    private var mMode = 0
    private var mRadius = 0f
    private val mPath by lazy { Path() }

    fun setRoundCorner(radius: Int) {
        mMode = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val viewStyleSetter = ViewStyleSetter(this)
            viewStyleSetter.setRoundRect(radius.toFloat())
        }
    }

    fun setRoundCornerBottom(radius: Int) {
        mMode = 1
        mRadius = radius.toFloat()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (mMode == 1) {
            mWidth = width.toFloat()
            mHeight = height.toFloat()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        if (mMode == 1) {
            if (mWidth > mRadius && mHeight > mRadius) {
                mPath.run {
                    moveTo(0f, 0f)
                    lineTo(mWidth, 0f)
                    lineTo(mWidth, mHeight - mRadius)
                    quadTo(mWidth, mHeight, mWidth - mRadius, mHeight)
                    lineTo(mRadius, mHeight)
                    quadTo(0f, mHeight, 0f, mHeight - mRadius)
                    lineTo(0f, 0f)
                }
                canvas!!.clipPath(mPath)
            }
        }
        super.onDraw(canvas)
    }
}
