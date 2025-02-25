package com.georgel.logrecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.georgel.logrecord.ui.theme.LogRecordTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LogRecordTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }

        LogRecord.get().init(this).setFolder("dmr")
            .setInformat("yyyy-MM-dd")
            .setNotifyDesc("日志记录服务")
            .setNotifyId(12)
            .setNotifyIcon(R.mipmap.ic_launcher)
            .setOffDesc("关闭日志")
            .setOnDesc("开始日志")
            .setControlBroadcastAction("dmr_log_start","dmr_log_stop")
            .setFolder("ALOG")
            .setPrefix("DMR")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LogRecordTheme {
        Greeting("Android")
    }
}