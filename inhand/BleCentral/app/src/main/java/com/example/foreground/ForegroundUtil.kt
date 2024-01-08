package com.example.foreground

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class ForegroundUtil {

    /**
     * 서비스 초기화
     */
    fun initService(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent);
        }
    }

    /**
     * MyForegroundService 실행 및 notification 호출
     */
    fun startService(activity: Activity) {
        sendService(
            activity,
            MyForegroundService.Actions.START_SERVICE.toString(),
            mapOf(
                "channelId" to "inhand",
                "channelName" to "inhandPlus"
            )
        )
    }

    fun initBle() {

    }

    /**
     * 문자열 메세지 전송
     */
    fun sendMessage(activity: Activity, data: String) {
        sendService(
            activity,
            MyForegroundService.Actions.MESSAGE.toString(),
            mapOf("data" to data)
        )
    }

    /**
     * MyForegroundService 서비스 종료
     */
    fun stopService(activity: Activity) {
        sendService(
            activity,
            MyForegroundService.Actions.STOP_SERVICE.toString(),
            mapOf()
        )
    }

    private fun sendService(
        activity: Activity,
        action: String,
        data: Map<String, String> = mapOf(),
        needCheck: Boolean = true
    ) {
        if (needCheck) {
            if (!isMyServiceRunning(activity, MyForegroundService::class.java)) {
                Log.d(TAG, "sendService: 서비스 실행 안됨")
                return
            }
        }
        val intentStart = Intent(activity, MyForegroundService::class.java)
        intentStart.action = action
        if (data.isNotEmpty()) {
            data.entries.map {
                intentStart.putExtra(it.key, it.value)
            }
        }

        activity.startService(intentStart)
    }

    private fun isMyServiceRunning(activity: Activity, serviceClass: Class<*>): Boolean {
        try {
            val manager =
                activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
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

    companion object {
        private const val TAG = "ForegroundUtil"
    }
}
