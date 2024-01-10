package com.example.bleCentral

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.blecentral.R
import com.example.bleCentral.foreground.ForegroundUtil

class ForegroundActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_foreground)

        findViewById<Button>(R.id.bt_start_foreground).setOnClickListener {
            ForegroundUtil.startService(this)
        }
        findViewById<Button>(R.id.bt_send_data).setOnClickListener {
            ForegroundUtil.sendMessageCentral(this, "데이터 전송 ")
        }

        findViewById<Button>(R.id.bt_stop_foreground).setOnClickListener {
            ForegroundUtil.stopService(this)
        }
    }

}