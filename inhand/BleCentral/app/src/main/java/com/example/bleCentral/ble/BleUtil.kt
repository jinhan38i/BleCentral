package com.example.bleCentral.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bleCentral.foreground.ForegroundUtil
import java.lang.Exception

@SuppressLint("StaticFieldLeak")
class BleUtil {

    companion object {
        private const val TAG = "BleUtil"
        const val blePermission = 335
        const val bleEnable = 336
        var bleUtil: BleUtil? = null
        private lateinit var context: Context
        private lateinit var bleUuid: BleUuid
        const val BLE_MESSAGE_LAUNCH_APP_NOTIFICATION = "launchNotification"
        const val BLE_MESSAGE_PERIPHERAL_DISCONNECT = "peripheralDisconnect"

        fun getInstance(_context: Context? = null, _bleUuid: BleUuid? = null): BleUtil {
            if (bleUtil == null && _context != null && _bleUuid != null) {
                context = _context
                bleUuid = _bleUuid
                bleUtil = BleUtil()
            }
            if (bleUtil == null) {
                throw Exception("bleUtil을 생성해주세요.")
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
            context.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "블루투스를 지원하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            bleCentral = BleCentral(context, bluetoothAdapter!!, bleUuid)
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

    val connectCentral = { context: Context, device: BluetoothDevice, autoConnect: Boolean ->
        bleCentral?.connectToDevice(context, device, autoConnect)
    }

    val connectCentralByAddress = { context: Context, deviceAddress: String, autoConnect: Boolean ->
        bleCentral?.connectToDeviceByName(context, deviceAddress, autoConnect)
    }

    val disconnectCentral = { bleCentral?.disconnect() }

    val writeCentral = { message: String -> bleCentral?.writeData(message) }

    val getConnectedGatt = {
        if (bleCentral == null || !bleCentral!!.isConnected) {
            null
        } else {
            bleCentral?.connectedGatt
        }

    }

    val connectInfo = { context: Context -> ForegroundUtil.connectInfo(context) }
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

    val appLaunchNotificationPeripheral =
        { blePeripheral?.writeData(BLE_MESSAGE_LAUNCH_APP_NOTIFICATION) }

    //================================================================================================
}