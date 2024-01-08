package com.example.bleCentral.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("StaticFieldLeak")
class BleUtil {

    companion object {
        private const val TAG = "BleUtil"
        const val blePermission = 335
        const val bleEnable = 336
        private var bleUtil: BleUtil? = null
        private lateinit var activity: Activity
        private lateinit var bleUuid: BleUuid

        fun getInstance(_activity: Activity, _bleUuid: BleUuid): BleUtil {
            if (bleUtil == null) {
                activity = _activity
                bleUuid = _bleUuid
                bleUtil = BleUtil()
            }
            return bleUtil!!
        }
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothManager: BluetoothManager? = null
    private val scanTime: Long = 10000
    var bleCentral: BleCentral? = null
    private var blePeripheral: BlePeripheral? = null

    init {
        bluetoothManager =
            activity.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            Log.d(TAG, "블루투스를 지원하지 않습니다.")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(activity, "블루투스를 지원하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            bleCentral = BleCentral(bluetoothAdapter!!, bleUuid)
            blePeripheral = BlePeripheral(bluetoothAdapter!!, bluetoothManager!!, bleUuid)
        }
    }


    // Common
    //================================================================================================
    var getBleEnable = { bluetoothAdapter!!.isEnabled }
    //================================================================================================


    // Central
    //================================================================================================

    val addCentralListener = { listener: BleListener -> bleCentral?.addListener(listener) }

    val removeCentralListener = { listener: BleListener -> bleCentral?.removeListener(listener) }

    val removeAllCentralListener = bleCentral?.removeAllListener()

    val startBleScan = { bleCentral?.startBleScan(scanTime) }

    val stopBleScan = { bleCentral?.stopBleScan() }

    val connectCentral = { activity: Activity, device: BluetoothDevice, autoConnect: Boolean ->
        bleCentral?.connectToDevice(activity, device, autoConnect)
    }

    val disconnectCentral = { bleCentral?.disconnect() }

    val writeCentral = { message: String -> bleCentral?.writeData(message) }

    //================================================================================================


    // Peripheral
    //================================================================================================
    val addPeripheralListener = { listener: BleListener -> blePeripheral?.addListener(listener) }

    val removePeripheralListener =
        { listener: BleListener -> blePeripheral?.removeListener(listener) }

    val removeAllPeripheralListener = blePeripheral?.removeAllListener()

    val startAdvertising = { activity: Activity, name: String ->
        blePeripheral?.startAdvertising(activity, name)
    }

    val stopAdvertising = { blePeripheral?.stopAdvertising() }

    val writePeripheral = { message: String -> blePeripheral?.writeData(message) }

    val disconnectPeripheral = { blePeripheral?.disconnect() }

    //================================================================================================
}