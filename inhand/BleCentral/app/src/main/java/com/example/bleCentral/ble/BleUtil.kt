package com.example.bleCentral.ble

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

object BleUtil {

    private const val TAG = "BleUtil"
    const val blePermission = 335
    const val bleEnable = 336
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothManager: BluetoothManager? = null
    val scanTime: Long = 10000
    lateinit var bleUuid: BleUuid
    val setInstance = { activity: Activity, bleUuid: BleUuid ->
        this.bleUuid = bleUuid
        if (bluetoothAdapter == null) {
            bluetoothManager =
                activity.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
            Log.d(TAG, "bluetoothManager : $bluetoothManager")
            bluetoothAdapter = bluetoothManager?.adapter
            Log.d(TAG, "bluetoothAdapter : $bluetoothAdapter")

        }
        if (bluetoothAdapter == null) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(activity, "블루투스를 지원하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            BleCentral.setInstance(bluetoothAdapter!!, bleUuid)
        }
    }

    var getBleEnable = { bluetoothAdapter!!.isEnabled }

    val addCentralListener = { listener: BleListener -> BleCentral.addListener(listener) }

    val removeCentralListener = { listener: BleListener -> BleCentral.removeListener(listener) }

    val removeAllCentralListener = BleCentral.removeAllListener()

    val addPeripheralListener = { listener: BleListener -> BlePeripheral.addListener(listener) }

    val removePeripheralListener =
        { listener: BleListener -> BlePeripheral.removeListener(listener) }

    val removeAllPeripheralListener = BlePeripheral.removeAllListener()

    val startBleScan = { BleCentral.startBleScan(scanTime) }

    val requestNotificationPermission =
        { it: Activity -> BlePermission.requestNotificationPermission(it) }

    val checkBlePermission: (Activity) -> Boolean = { BlePermission.checkBlePermission(it) }

    val changeBluetoothEnable = { activity: Activity ->
        BlePermission.changeBluetoothEnable(activity, bluetoothAdapter!!)
    }

    val startAdvertising: (activity: Activity, name: String) -> Boolean =
        { activity: Activity, name: String ->
            BlePeripheral.startAdvertising(bluetoothAdapter!!, bluetoothManager!!, activity, name)
        }

    val stopAdvertising = { BlePeripheral.stopAdvertising() }

    val connect = { activity: Activity, device: BluetoothDevice, autoConnect: Boolean ->
        BleCentral.connectToDevice(activity, device, autoConnect)
    }

    val disconnect = { BleCentral.disconnect() }

    val writeCentral = { message: String -> BleCentral.writeData(message) }

    val writePeripheral = { message: String -> BlePeripheral.writeData(message) }

}