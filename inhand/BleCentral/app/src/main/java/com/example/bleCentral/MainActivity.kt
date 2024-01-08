package com.example.bleCentral

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.bleCentral.ble.BlePermission
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
        Log.d(
            TAG,
            "onRequestPermissionsResult() called with: requestCode = $requestCode, permissions = $permissions, grantResults = $grantResults"
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(
            TAG,
            "onActivityResult() called with: requestCode = $requestCode, resultCode = $resultCode, data = $data"
        )
    }
}