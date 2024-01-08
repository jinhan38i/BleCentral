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
import com.example.bleCentral.ble.BleUuid
import com.example.blecentral.R
import java.util.Date

@SuppressLint("MissingPermission")
class CentralActivity : AppCompatActivity(), BleListener {
    companion object {
        private const val TAG = "ScanActivity"
    }

    var connectedDevice: BluetoothDevice? = null

    private lateinit var listViewDevice: ListView
    private lateinit var listViewChat: ListView
    private lateinit var scanButton: Button
    private lateinit var stopScanButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var btWrite: Button
    private val resultList = ArrayList<ScanResult>()
    private val messageList = ArrayList<String>()
    lateinit var bleUtil: BleUtil

    override fun onCreate(savedInstanceState: Bundle?) {

        bleUtil = BleUtil.getInstance(
            this, BleUuid(
                serviceUuid = "fec26ec4-6d71-4442-9f81-55bc21d658d0",
                charUuid = "fec26ec4-6d71-4442-9f81-55bc21d658d1",
                descriptorUuid = "00002902-0000-1000-8000-00805f9b34fb",
            )
        )
        BleUtil
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_central)
        bleUtil.addCentralListener(this)

        scanButton = findViewById(R.id.scan)
        stopScanButton = findViewById(R.id.bt_stop_scan)
        disconnectButton = findViewById(R.id.bt_disconnect)
        listViewDevice = findViewById(R.id.listView_device)
        listViewChat = findViewById(R.id.listView_chat)
        btWrite = findViewById(R.id.bt_write)

        scanButton.setOnClickListener {
            resultList.clear()
            bleUtil.startBleScan()
        }
        stopScanButton.setOnClickListener {
            bleUtil.stopBleScan()
        }
        disconnectButton.setOnClickListener {
            bleUtil.disconnectCentral()
        }
        btWrite.setOnClickListener {
            bleUtil.writeCentral("C = ${Date().time}")
        }
        if (connectedDevice == null) {
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
    }

    override fun onDestroy() {
        bleUtil.stopBleScan()
        bleUtil.removeCentralListener(this)
        super.onDestroy()
    }

    override fun scannedDevice(device: ScanResult) {
        Log.d(TAG, "scannedDevice() called with: device = $device")

        for (scanResult in resultList) {
            if (scanResult.device.address == device.device.address) {
                return
            }
        }
        resultList.add(device)

        val adapter: ArrayAdapter<ScanResult> = object : ArrayAdapter<ScanResult>(
            this, android.R.layout.simple_list_item_1, resultList
        ) {
            @SuppressLint("MissingPermission", "SetTextI18n")
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<View>(android.R.id.text1) as TextView
                textView.setTextColor(Color.BLACK)
                val d = resultList[position].device
                textView.text = d.name + " = " + d.address
                view.setOnClickListener {
                    bleUtil.connectCentral(this@CentralActivity, d, false)
                }
                return view
            }
        }

        listViewDevice.adapter = adapter
    }

    override fun bondedDevice(bondedDevice: BluetoothDevice) {
        Log.d(TAG, "bondedDevice() called with: bondedDevice = $bondedDevice")
    }

    override fun stopScan() {
        Log.d(TAG, "stopScan() called")
    }

    override fun connect(device: BluetoothDevice) {
        connectedDevice = device
        runOnUiThread {
            listViewDevice.visibility = GONE
            scanButton.visibility = GONE
            disconnectButton.visibility = VISIBLE
            listViewChat.visibility = VISIBLE
            btWrite.visibility = VISIBLE
            resultList.clear()
        }
        showToast("연결 완료")
    }

    override fun disConnect(device: BluetoothDevice) {
        Log.d(TAG, "disConnect() called with: device = $device")
        connectedDevice = null
        runOnUiThread {
            listViewDevice.visibility = VISIBLE
            scanButton.visibility = VISIBLE
            disconnectButton.visibility = GONE
            listViewChat.visibility = GONE
            btWrite.visibility = GONE
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
            listViewChat.adapter = adapter
        }
    }
}