package com.example.foreground

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat


class MyForegroundService : Service() {

    companion object {
        private const val TAG = "MyForegroundService"
    }

    enum class Actions {
        START_SERVICE,
        STOP_SERVICE,
        MESSAGE
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
            }

            Actions.STOP_SERVICE.toString() -> {
                stopSelf()
            }

            Actions.MESSAGE.toString() -> {
                Log.d(TAG, "onStartCommand: message : ${intent.getStringExtra("data")}")
            }
        }
        return super.onStartCommand(intent, flags, startId)
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
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager?.createNotificationChannel(channel)
        }
        val notification =
            NotificationCompat.Builder(this, channelId)
                .setAutoCancel(true)
                .build()
//            .setSmallIcon(R.drawable.ic_notification)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(99, notification, FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(99, notification)
        }
    }

}