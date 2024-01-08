package com.example.foreground

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.blecentral.R

class ForegroundActivity : AppCompatActivity() {
    lateinit var foregroundUtil: ForegroundUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        foregroundUtil = ForegroundUtil()
        setContentView(R.layout.activity_foreground)

        findViewById<Button>(R.id.bt_start_foreground).setOnClickListener {
            foregroundUtil.startService(this)
        }
        findViewById<Button>(R.id.bt_send_data).setOnClickListener {
            foregroundUtil.sendMessage(this, "데이터 전송 ")
        }

        findViewById<Button>(R.id.bt_stop_foreground).setOnClickListener {
            foregroundUtil.stopService(this)
        }
    }

    override fun onDestroy() {
        foregroundUtil.stopService(this)
        super.onDestroy()
    }
}