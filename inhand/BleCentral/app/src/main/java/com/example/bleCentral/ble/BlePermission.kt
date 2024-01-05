package com.example.bleCentral.ble

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat

@SuppressLint("MissingPermission")
object BlePermission {

    /**
     * 블루투스 활성화/비활성화 설정
     */
    val changeBluetoothEnable = { activity: Activity, adapter: BluetoothAdapter ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            when (adapter.isEnabled) {
                true -> adapter.disable()
                false -> adapter.enable()
            }
        } else {
            activity.startActivityForResult(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                BleUtil.bleEnable
            )
        }
    }

    /**
     * 권한 요청
     */
    val requestNotificationPermission = { activity: Activity ->
        if (!checkBlePermission(activity)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                val intent: Intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${activity.packageName}"))
                activity.startActivityForResult(intent, BleUtil.blePermission)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_SCAN,
                    ),
                    BleUtil.blePermission
                )
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    BleUtil.blePermission
                )
            }
        }
    }

    /**
     * 권한 체크
     */
    val checkBlePermission: (Activity) -> Boolean = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            it.checkCallingOrSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && it.checkCallingOrSelfPermission(
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED && it.checkCallingOrSelfPermission(
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED && it.checkCallingOrSelfPermission(
                Manifest.permission.BLUETOOTH_SCAN,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            it.checkCallingOrSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}