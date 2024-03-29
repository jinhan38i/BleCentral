package com.example.bleCentral

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.graphics.Color
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
import com.example.bleCentral.ble.BleListener
import com.example.bleCentral.ble.BleUtil
import com.example.blecentral.R
import java.util.Date

class PeripheralActivity : AppCompatActivity(), BleListener {
    companion object {
        private const val TAG = "PeripheralActivity"
    }

    private lateinit var btStartAdvertising: Button
    private lateinit var btStopAdvertising: Button
    private lateinit var btDisconnect: Button
    private lateinit var btWrite: Button
    private lateinit var btLaunchApp: Button
    private lateinit var listViewPeripheralChat: ListView
    private val messageList = ArrayList<String>()
    lateinit var bleUtil: BleUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peripheral)

        bleUtil = BleUtil.getInstance()

        bleUtil.addPeripheralListener(this)

        btStartAdvertising = findViewById(R.id.bt_start_advertising)
        btStopAdvertising = findViewById(R.id.bt_stop_advertising)
        btDisconnect = findViewById(R.id.bt_peripheral_disconnect)
        btWrite = findViewById(R.id.bt_peripheral_write)
        listViewPeripheralChat = findViewById(R.id.listView_peripheral_chat)
        btLaunchApp = findViewById(R.id.bt_start_app)

        btStartAdvertising.setOnClickListener { bleUtil.startAdvertising(this, "watch2") }
        btStopAdvertising.setOnClickListener { bleUtil.stopAdvertising() }
        btDisconnect.setOnClickListener {
            bleUtil.disconnectPeripheral()
        }
        btWrite.setOnClickListener {
            bleUtil.writePeripheral("P = ${Date().time}")
        }

        btLaunchApp.setOnClickListener {
            bleUtil.appLaunchNotificationPeripheral()
        }

    }

    override fun onDestroy() {
        bleUtil.stopAdvertising()
        bleUtil.removePeripheralListener(this)
        super.onDestroy()
    }

    override fun scannedDevice(scanResult: ScanResult) {
    }

    override fun bondedDevice(bondedDevice: BluetoothDevice) {
    }

    override fun stopScan() {
    }

    override fun connect(device: BluetoothDevice) {
        showToast("연결 성공")
        runOnUiThread {
            btStartAdvertising.visibility = GONE
            btStopAdvertising.visibility = GONE
            btDisconnect.visibility = VISIBLE
            btWrite.visibility = VISIBLE
            listViewPeripheralChat.visibility = VISIBLE
            btLaunchApp.visibility = VISIBLE
        }
    }

    override fun disConnect(device: BluetoothDevice) {
        runOnUiThread {
            btStartAdvertising.visibility = VISIBLE
            btStopAdvertising.visibility = VISIBLE
            btDisconnect.visibility = GONE
            btWrite.visibility = GONE
            listViewPeripheralChat.visibility = GONE
            btLaunchApp.visibility = GONE
            messageList.clear()
        }
        showToast("연결 해제")
    }

    override fun readMessage(byte: ByteArray, message: String, address: String) {
        messageList.add(message)
        setMessageList()
    }

    override fun writeMessage(message: String) {
        messageList.add(message)
        setMessageList()
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setMessageList() {
        runOnUiThread {
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
            listViewPeripheralChat.adapter = adapter
        }
    }
}