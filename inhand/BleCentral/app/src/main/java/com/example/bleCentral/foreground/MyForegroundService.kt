package com.example.bleCentral.foreground

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.bleCentral.MainActivity
import com.example.bleCentral.ble.BleListener
import com.example.bleCentral.ble.BleUtil
import com.example.bleCentral.ble.BleUuid
import com.example.blecentral.R


@SuppressLint("MissingPermission")
class MyForegroundService : Service() {

    private var listenerCentral = object : BleListener {
        override fun scannedDevice(scanResult: ScanResult) {
            sendScannedResult(scanResult)
        }

        override fun bondedDevice(bondedDevice: BluetoothDevice) {
        }

        override fun stopScan() {
            sendBroadcast(Intent(ACTION_BLE_STOP_SCAN))
        }

        override fun connect(device: BluetoothDevice) {
            connectCentral()
        }

        override fun disConnect(device: BluetoothDevice) {
            disconnectCentral()
        }

        override fun writeMessage(message: String) {
            Log.d(TAG, "writeMessage() called with: message = $message")
            sendBroadcast(Intent(ACTION_BLE_WRITE_MESSAGE).apply {
                this.putExtra("message", message)
            })
        }

        override fun readMessage(byte: ByteArray, message: String, address: String) {
            Log.d(
                TAG,
                "readMessage() called with: byte = $byte, message = $message, address = $address"
            )
            sendBroadcast(Intent(ACTION_BLE_READ_MESSAGE).apply {
                this.putExtra("message", message)
            })
        }
    }

    companion object {
        private const val TAG = "MyForegroundService"
        const val ACTION_BLE_DEVICE_INFO = "bleDeviceInfo"
        const val ACTION_BLE_STOP_SCAN = "bleStopScan"
        const val ACTION_BLE_CONNECT_CENTRAL = "bleDeviceConnectCentral"
        const val ACTION_BLE_DISCONNECT = "bleDeviceDisconnect"
        const val ACTION_BLE_DEVICE_SCAN_RESULT = "bleDeviceScanResult"
        const val ACTION_BLE_WRITE_MESSAGE = "bleWriteMessage"
        const val ACTION_BLE_READ_MESSAGE = "bleReadMessage"
    }

    private var bleUtil: BleUtil? = null

    enum class Actions {
        START_SERVICE,
        STOP_SERVICE,
        MESSAGE_CENTRAL,
        CONNECT_INFO,
        ADVERTISING,
        START_SCAN,
        STOP_SCAN,
        CONNECT,
        DISCONNECT_CENTRAL,
        APP_LAUNCH
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.START_SERVICE.toString() -> {
                val channelId = intent.getStringExtra("channelId") ?: ""
                val channelName = intent.getStringExtra("channelName") ?: ""
                serviceStart(channelId, channelName)
                bleUtil = BleUtil.getInstance(
                    baseContext, BleUuid(
                        serviceUuid = "fec26ec4-6d71-4442-9f81-55bc21d658d0",
                        charUuid = "fec26ec4-6d71-4442-9f81-55bc21d658d1",
                        descriptorUuid = "00002902-0000-1000-8000-00805f9b34fb",
                    )
                )
                bleUtil!!.addCentralListener(listenerCentral)
            }

            Actions.STOP_SERVICE.toString() -> {
                bleUtil!!.removeCentralListener(listenerCentral)
                stopSelf()
            }

            Actions.MESSAGE_CENTRAL.toString() -> {
                bleUtil!!.writeCentral(intent.getStringExtra("data").toString())
            }

            Actions.CONNECT.toString() -> {
                bleUtil!!.connectCentralByAddress(
                    baseContext,
                    intent.getStringExtra("data").toString(),
                    false
                )
            }

            Actions.CONNECT_INFO.toString() -> getConnectInfo()
            Actions.START_SCAN.toString() -> bleUtil!!.startBleScan()
            Actions.STOP_SCAN.toString() -> bleUtil!!.stopBleScan()
            Actions.STOP_SCAN.toString() -> bleUtil!!.stopBleScan()
            Actions.DISCONNECT_CENTRAL.toString() -> bleUtil!!.disconnectCentral()

        }
        return START_NOT_STICKY
    }


    /**
     * 포그라운드 서비스 실행
     */
    private fun serviceStart(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager?.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            baseContext,
            100,
            Intent(baseContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification =
            NotificationCompat.Builder(this, channelId)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("서비스 시작")
                .setContentTitle("서비스를 시작했습니다.")
                .build()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(99, notification, FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(99, notification)
        }
    }

    private fun getConnectInfo() {
        val connectIntent = Intent(ACTION_BLE_DEVICE_INFO)
        val connectedGatt = bleUtil!!.getConnectedGatt()
        if (connectedGatt == null) {
            connectIntent.putExtra("deviceName", "")
        } else {
            connectIntent.putExtra("deviceName", connectedGatt.device.name)
        }
        sendBroadcast(connectIntent)
    }


    private fun connectCentral() {
        val connectIntent = Intent(ACTION_BLE_CONNECT_CENTRAL)
        val connectedGatt = bleUtil!!.getConnectedGatt()
        if (connectedGatt == null) {
            connectIntent.putExtra("deviceName", "")
            connectIntent.putExtra("deviceAddress", "")
        } else {
            connectIntent.putExtra("deviceName", connectedGatt.device.name)
            connectIntent.putExtra("deviceAddress", connectedGatt.device.address)
        }
        sendBroadcast(connectIntent)
    }

    private fun disconnectCentral() {
        sendBroadcast(Intent(ACTION_BLE_DISCONNECT))
    }

    private fun sendScannedResult(scanResult: ScanResult) {
        val connectIntent = Intent(ACTION_BLE_DEVICE_SCAN_RESULT)
        connectIntent.putExtra("deviceName", scanResult.device.name)
        connectIntent.putExtra("deviceAddress", scanResult.device.address)
        connectIntent.putExtra("deviceRssi", scanResult.rssi)
        sendBroadcast(connectIntent)
    }

}