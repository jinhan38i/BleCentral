package com.example.bleCentral

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.bleCentral.ble.BlePermission
import com.example.bleCentral.ble.BleUtil
import com.example.bleCentral.ble.BleUuid
import com.example.blecentral.R
import com.example.bleCentral.foreground.ForegroundUtil
import com.example.bleCentral.foreground.MyForegroundService

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ForegroundUtil.startService(this)
        BleUtil.getInstance(
            baseContext, BleUuid(
                serviceUuid = "fec26ec4-6d71-4442-9f81-55bc21d658d0",
                charUuid = "fec26ec4-6d71-4442-9f81-55bc21d658d1",
                descriptorUuid = "00002902-0000-1000-8000-00805f9b34fb",
            )
        )
        findViewById<Button>(R.id.bt_ble_enable).setOnClickListener {
            BlePermission.changeBluetoothEnable(this)
            findViewById<Button>(R.id.bt_ble_enable).text =
                "Enable = ${BlePermission.getBleEnable(this)}"
        }
        findViewById<Button>(R.id.bt_scan).setOnClickListener {
            if (!BlePermission.checkBlePermission(this)) return@setOnClickListener
            startActivity(Intent(this, CentralActivity::class.java))
        }
        findViewById<Button>(R.id.bt_peripheral).setOnClickListener {
            if (!BlePermission.checkBlePermission(this)) return@setOnClickListener
            startActivity(Intent(this, PeripheralActivity::class.java))
        }


        findViewById<Button>(R.id.bt_ble_enable).text =
            "Enable = ${BlePermission.getBleEnable(this)}"
        BlePermission.requestPermission(this)

        findViewById<Button>(R.id.bt_foreground).setOnClickListener {
            startActivity(Intent(this, ForegroundActivity::class.java))
        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}