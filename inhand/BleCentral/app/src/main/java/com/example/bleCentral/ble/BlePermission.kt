package com.example.bleCentral.ble

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

@SuppressLint("MissingPermission")
object BlePermission {

    private const val TAG = "BlePermission"

    /**
     * 블루투스 활성화/비활성화 설정
     */
    val changeBluetoothEnable = { activity: Activity ->
        val bluetoothManager =
            activity.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Log.d(TAG, "블루투스를 지원하지 않습니다.")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(activity, "블루투스를 지원하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                when (bluetoothAdapter.isEnabled) {
                    true -> bluetoothAdapter.disable()
                    false -> bluetoothAdapter.enable()
                }
            } else {
                when (bluetoothAdapter.isEnabled) {
                    true -> activity.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    false -> {
                        activity.startActivityForResult(
                            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                            BleUtil.bleEnable
                        )
                    }
                }

            }
        }
    }

    var getBleEnable: (Activity) -> Boolean = { activity: Activity ->
        val bluetoothManager =
            activity.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        bluetoothAdapter!!.isEnabled
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