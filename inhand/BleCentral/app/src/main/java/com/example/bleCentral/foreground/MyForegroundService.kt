package com.example.bleCentral.foreground

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.bleCentral.MainActivity
import com.example.bleCentral.ble.BleUtil
import com.example.bleCentral.ble.BleUuid
import com.example.blecentral.R


class MyForegroundService : Service() {

    companion object {
        private const val TAG = "MyForegroundService"
    }

    var bleUtil: BleUtil? = null

    enum class Actions {
        START_SERVICE,
        STOP_SERVICE,
        MESSAGE,
        APP_LAUNCH
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: data : ${intent?.extras}")
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
            }

            Actions.STOP_SERVICE.toString() -> {
                stopSelf()
            }

            Actions.MESSAGE.toString() -> {
                Log.d(TAG, "onStartCommand: message : ${intent.getStringExtra("data")}")
            }

            Actions.APP_LAUNCH.toString() -> {
                appLaunch("inhand")
            }
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
                .setOngoing(false)
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


    @SuppressLint("MissingPermission")
    fun appLaunch(channelId: String) {
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            100,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification =
            NotificationCompat.Builder(this, channelId)
                .setAutoCancel(true)
                .setContentTitle("앱 실행")
                .setContentText("클릭해서 앱을 실행시키세요")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .build()


        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(100, notification)
    }

}