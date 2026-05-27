package com.example.clickdevice.activity

import android.content.Intent
import android.graphics.drawable.shapes.RoundRectShape
import android.graphics.drawable.shapes.Shape
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.clickdevice.R
import com.example.clickdevice.ui.theme.ClickDeviceTheme
import kotlinx.coroutines.delay

class LauncherActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClickDeviceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LauncherScreen(onNavigateToMain = {
                        startActivity(Intent(this@LauncherActivityCompose, MainActivityCompose::class.java))
                        finish()
                    })
                }
            }
        }
    }
}

@Composable
fun LauncherScreen(onNavigateToMain: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.icon_app),
            contentDescription = "ClickDevice Logo",
            modifier = Modifier.clip(RoundedCornerShape(20))
        )
    }
    
    // 延迟跳转
    androidx.compose.runtime.LaunchedEffect(Unit) {
        delay(500)
        onNavigateToMain()
    }
}

@Preview(showBackground = true)
@Composable
fun LauncherScreenPreview() {
    ClickDeviceTheme {
        LauncherScreen(onNavigateToMain = {})
    }
}