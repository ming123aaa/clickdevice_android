package com.example.clickdevice

class ScriptInterfaceImpl(var isRunning:()-> Boolean) : ScriptExecutor.ScriptInterFace{
    var delayCoefficient: Float=1F
    var xCoefficient: Float=1F
    var yCoefficient: Float=1F


    override fun isRun(): Boolean {
        return isRunning()
    }

    // endregion

    // region ScriptInterFace

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
        val adjustedTime = (time * delayCoefficient).toLong()
        sleep(adjustedTime)
    }

    override fun clickCMD(x0: Int, y0: Int, mDuration: Int) {
        if (!isRun) return
        val service =  MyService.myService ?: return
        

        val xc = if (xCoefficient in 0.25f..5.0f) xCoefficient else 1.0f
        val yc = if (yCoefficient in 0.25f..5.0f) yCoefficient else 1.0f
        val duration = (mDuration*delayCoefficient).toInt()
        val sx = (x0 * xc).toInt()
        val sy = (y0 * yc).toInt()
        if (duration < 30) {

            try {
                service.dispatchGestureClick(sx.toFloat(), sy.toFloat())
                sleep(30)
            } catch (_: Exception) {}
        } else {
            val d = if (duration > 30000) 30000 else duration

            try {
                service.dispatchGestureClick(sx.toFloat(), sy.toFloat(), d)
                sleep(d.toLong())
            } catch (_: Exception) {}
        }
    }

    override fun gestureCMD(x0: Int, y0: Int, x1: Int, y1: Int, mDuration: Int) {
        if (!isRun) return
        val service =  MyService.myService ?: return

        val xc = if (xCoefficient in 0.25f..5.0f) xCoefficient else 1.0f
        val yc = if (yCoefficient in 0.25f..5.0f) yCoefficient else 1.0f
        val sx0 = (x0 * xc).toInt()
        val sy0 = (y0 * yc).toInt()
        val sx1 = (x1 * xc).toInt()
        val sy1 = (y1 * yc).toInt()
        val duration = (mDuration*delayCoefficient).toInt()
        val d = when {
            duration > 30000 -> 30000
            duration < 100 -> 100
            else -> duration
        }

        try {
            service.dispatchGesture(sx0.toFloat(), sy0.toFloat(), sx1.toFloat(), sy1.toFloat(), d)
            sleep(d.toLong()) } catch (_: Exception) {}
    }
}