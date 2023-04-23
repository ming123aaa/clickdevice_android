package com.example.clickdevice.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.clickdevice.R
import com.example.clickdevice.helper.SizeUtils
import com.example.clickdevice.view.CornerImageView

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        findViewById<CornerImageView>(R.id.cornerImageView).setRoundCorner(SizeUtils.dp2px(20f))
        window.decorView.postDelayed({
            startActivity(Intent(this@LauncherActivity, MainActivity::class.java))
            finish()
        }, 1500)//延迟等待无障碍服务自动启动
    }
}