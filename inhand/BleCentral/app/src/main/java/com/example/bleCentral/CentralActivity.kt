package com.example.bleCentral

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bleCentral.ble.BleModel
import com.example.bleCentral.foreground.ForegroundUtil
import com.example.bleCentral.foreground.MyForegroundService
import com.example.blecentral.R
import java.util.Date


@SuppressLint("MissingPermission")
class CentralActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CentralActivity"
    }

    private lateinit var listViewDevice: ListView
    private lateinit var listViewChat: ListView
    private lateinit var scanButton: Button
    private lateinit var stopScanButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var btWrite: Button
    private lateinit var btConnectedDevice: Button
    private val resultList = ArrayList<BleModel>()
    private val messageList = ArrayList<String>()
    private var connect = false
    private var deviceName = ""
    private var deviceAddress = ""

    private val bleReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runOnUiThread {
                when (intent.action) {
                    MyForegroundService.ACTION_BLE_DEVICE_INFO -> {
                        val data = intent.getStringExtra("deviceName")
                        deviceName = data ?: ""
                        if (deviceName.isNotEmpty()) {
                            connectSuccess()
                        }
                    }

                    MyForegroundService.ACTION_BLE_CONNECT_CENTRAL -> {
                        deviceName = intent.getStringExtra("deviceName") ?: ""
                        deviceAddress = intent.getStringExtra("deviceAddress") ?: ""
                        connectSuccess()
                    }

                    MyForegroundService.ACTION_BLE_DISCONNECT -> disconnect()

                    MyForegroundService.ACTION_BLE_DEVICE_SCAN_RESULT -> {
                        val deviceName = intent.getStringExtra("deviceName") ?: ""
                        val deviceAddress = intent.getStringExtra("deviceAddress") ?: ""
                        val rssi = intent.getIntExtra("deviceRssi", 0)
                        Log.d(TAG, "onReceive: deviceName : $deviceName")
                        for (model in resultList) {
                            if (model.deviceAddress == deviceAddress) {
                                return@runOnUiThread
                            }
                        }
                        val bleModel = BleModel(deviceName, deviceAddress, rssi)
                        resultList.add(bleModel)
                        setScanList()
                    }

                    MyForegroundService.ACTION_BLE_WRITE_MESSAGE,
                    MyForegroundService.ACTION_BLE_READ_MESSAGE -> {
                        setMessageList(intent.getStringExtra("message") ?: "")
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun connectSuccess() {
        connect = true
        resultList.clear()
        listViewDevice.visibility = GONE
        scanButton.visibility = GONE
        disconnectButton.visibility = VISIBLE
        listViewChat.visibility = VISIBLE
        btWrite.visibility = VISIBLE
        btConnectedDevice.text = "연결 : $deviceName"
        showToast("연결 성공")
    }

    fun disconnect(){
        connect = false
        deviceName = ""
        deviceAddress = ""
        messageList.clear()
        listViewDevice.visibility = VISIBLE
        scanButton.visibility = VISIBLE
        disconnectButton.visibility = GONE
        listViewChat.visibility = GONE
        btWrite.visibility = GONE
        showToast("연결 해제")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_central)

        scanButton = findViewById(R.id.scan)
        stopScanButton = findViewById(R.id.bt_stop_scan)
        disconnectButton = findViewById(R.id.bt_disconnect)
        listViewDevice = findViewById(R.id.listView_device)
        listViewChat = findViewById(R.id.listView_chat)
        btWrite = findViewById(R.id.bt_write)
        btConnectedDevice = findViewById(R.id.bt_connected_device)

        scanButton.setOnClickListener {
            resultList.clear()
            ForegroundUtil.startScan(this)
        }
        stopScanButton.setOnClickListener {
            ForegroundUtil.stopScan(this)
        }
        disconnectButton.setOnClickListener {
            ForegroundUtil.disconnectCentral(this)
        }
        btWrite.setOnClickListener {
            ForegroundUtil.sendMessageCentral(this, "C = ${Date().time}")
        }
        if (!connect) {
            listViewDevice.visibility = VISIBLE
            scanButton.visibility = VISIBLE
            disconnectButton.visibility = GONE
            listViewChat.visibility = GONE
            btWrite.visibility = GONE
        } else {
            listViewDevice.visibility = GONE
            scanButton.visibility = GONE
            disconnectButton.visibility = VISIBLE
            listViewChat.visibility = VISIBLE
            btWrite.visibility = VISIBLE
        }
        Handler(Looper.getMainLooper()).post {
            ForegroundUtil.connectInfo(this)
        }
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction(MyForegroundService.ACTION_BLE_STOP_SCAN)
        intentFilter.addAction(MyForegroundService.ACTION_BLE_DEVICE_INFO)
        intentFilter.addAction(MyForegroundService.ACTION_BLE_CONNECT_CENTRAL)
        intentFilter.addAction(MyForegroundService.ACTION_BLE_DEVICE_SCAN_RESULT)
        intentFilter.addAction(MyForegroundService.ACTION_BLE_DISCONNECT)
        intentFilter.addAction(MyForegroundService.ACTION_BLE_WRITE_MESSAGE)
        intentFilter.addAction(MyForegroundService.ACTION_BLE_READ_MESSAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                bleReceiver,
                intentFilter,
                RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(
                bleReceiver,
                intentFilter,
            )
        }
    }

    override fun onPause() {
        ForegroundUtil.stopScan(this)
        unregisterReceiver(bleReceiver)
        super.onPause()
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setScanList() {
        val adapter: ArrayAdapter<BleModel> = object : ArrayAdapter<BleModel>(
            this@CentralActivity, android.R.layout.simple_list_item_1, resultList
        ) {
            @SuppressLint("MissingPermission", "SetTextI18n")
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getView(position, convertView, parent)
                val textView =
                    view.findViewById<View>(android.R.id.text1) as TextView
                textView.setTextColor(Color.BLACK)
                val d = resultList[position]
                textView.text = d.deviceName + " = " + d.deviceAddress
                view.setOnClickListener {
                    ForegroundUtil.connectCentralByAddress(
                        this@CentralActivity,
                        d.deviceAddress,
                        false
                    )
                }
                return view
            }
        }
        listViewDevice.adapter = adapter
    }

    private fun setMessageList(message: String) {
        messageList.add(message)
        val adapter: ArrayAdapter<String> = object : ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_1, messageList
        ) {
            @SuppressLint("MissingPermission", "SetTextI18n")
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<View>(android.R.id.text1) as TextView
                textView.setTextColor(Color.BLACK)
                textView.text = messageList[position]
                return view
            }
        }
        listViewChat.adapter = adapter
    }
}