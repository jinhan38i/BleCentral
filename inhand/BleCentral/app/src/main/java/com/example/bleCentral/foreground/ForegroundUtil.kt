package com.example.bleCentral.foreground

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.bleCentral.MainActivity
import com.example.blecentral.R


object ForegroundUtil {

    private const val TAG = "ForegroundUtil"

    /**
     * MyForegroundService 실행 및 notification 호출
     */
    fun startService(context: Context) {
        if (isMyServiceRunning(context, MyForegroundService::class.java)) {
            return
        }
        sendService(
            context,
            MyForegroundService.Actions.START_SERVICE.toString(),
            mapOf(
                "channelId" to "inhandService",
                "channelName" to "inhandPlus"
            ),
            needCheck = false
        )
    }

    /**
     * 문자열 메세지 전송
     */
    fun sendMessageCentral(context: Context, data: String) {
        sendService(
            context,
            MyForegroundService.Actions.MESSAGE_CENTRAL.toString(),
            mapOf("data" to data)
        )
    }

    /**
     * MyForegroundService 서비스 종료
     */
    fun stopService(context: Context) {
        sendService(
            context,
            MyForegroundService.Actions.STOP_SERVICE.toString(),
            mapOf()
        )
    }

    fun connectInfo(context: Context) {
        sendService(
            context,
            MyForegroundService.Actions.CONNECT_INFO.toString(),
            mapOf("data" to "")
        )
    }

    fun startScan(context: Context) {
        sendService(
            context,
            MyForegroundService.Actions.START_SCAN.toString(),
            mapOf("data" to "")
        )
    }


    fun stopScan(context: Context) {
        sendService(
            context,
            MyForegroundService.Actions.STOP_SCAN.toString(),
            mapOf("data" to "")
        )
    }

    fun connectCentralByAddress(context: Context, deviceAddress: String, autoConnect: Boolean) {
        sendService(
            context,
            MyForegroundService.Actions.CONNECT.toString(),
            mapOf("data" to deviceAddress)
        )
    }


    fun disconnectCentral(context: Context) {
        sendService(
            context,
            MyForegroundService.Actions.DISCONNECT_CENTRAL.toString(),
            mapOf("data" to "")
        )
    }

    /**
     * MyForegroundService와 통신
     */
    private fun sendService(
        context: Context,
        action: String,
        data: Map<String, String> = mapOf(),
        needCheck: Boolean = true
    ) {
        if (needCheck) {
            if (!isMyServiceRunning(context, MyForegroundService::class.java)) {
                return
            }
        }
        val intentStart = Intent(context, MyForegroundService::class.java)
        intentStart.action = action
        if (data.isNotEmpty()) {
            data.entries.map {
                intentStart.putExtra(it.key, it.value)
            }
        }

        context.startService(intentStart)
    }

    /**
     * foreground service가 돌아가고 있는지 체크
     */
    private fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        try {
            val manager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    /**
     * 앱 실행 알림 호출
     */
    @SuppressLint("MissingPermission")
    fun appLaunchNotification(context: Context, channelId: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(
                NotificationManager::class.java
            )
            val channel = NotificationChannel(
                channelId,
                "channelName",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager?.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            100,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(context, channelId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentTitle("앱 실행")
                .setContentText("바디")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(100, notification)
    }

}
