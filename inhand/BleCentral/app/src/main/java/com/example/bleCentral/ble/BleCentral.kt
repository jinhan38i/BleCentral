package com.example.bleCentral.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.nio.charset.StandardCharsets

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
@SuppressLint("MissingPermission")
class BleCentral(private var bluetoothAdapter: BluetoothAdapter, private var bleUuid: BleUuid) {

    companion object {
        private const val TAG = "BleCentral"
    }

    private var isScanning = false
    val listeners = ArrayList<BleListener>()
    private var connectedGatt: BluetoothGatt? = null
    var connectedChar: BluetoothGattCharacteristic? = null
    var isConnected = false

    fun addListener(bleListener: BleListener) {
        listeners.add(bleListener)
    }

    fun removeListener(bleListener: BleListener) {
        val newListener = ArrayList<BleListener>()
        if (listeners.size == 1) {
            removeAllListener()
            return
        }
        for (i in 0 until listeners.size) {
            if (listeners[i] == bleListener) {
                continue
            }
            newListener.add(listeners[i])
        }
        listeners.clear()
        listeners.addAll(newListener)
    }

    fun removeAllListener() {
        listeners.clear()
    }

    /**
     * Ble 스캔 콜백
     */
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(
                TAG,
                "onScanResult: result name : ${result.device.name}, address : ${result.device.address}"
            )
            if (!result.device.name.isNullOrEmpty()) {
                for (bleListener in listeners) {
                    bleListener.scannedDevice(result)
                }
            }
        }
    }

    /**
     * 스캔 시작
     */
    fun startBleScan(scanTime: Long = 10000) {
        if (isScanning) return
        if (bluetoothAdapter.bluetoothLeScanner == null) return
        if (!bluetoothAdapter.isEnabled) return
        bluetoothAdapter.bluetoothLeScanner!!.startScan(leScanCallback)

        Handler(Looper.getMainLooper()).postDelayed({
            stopBleScan()
        }, scanTime)
    }

    /**
     * 스캔 중지
     */
    fun stopBleScan() {
        isScanning = false
        if (bluetoothAdapter.bluetoothLeScanner == null) return
        if (!bluetoothAdapter.isEnabled) return
        bluetoothAdapter.bluetoothLeScanner!!.stopScan(leScanCallback)
        for (bleListener in listeners) {
            bleListener.stopScan()
        }
    }

    /**
     * gatt 연결 상태 콜백
     */
    private val bluetoothGattCallback = object : BluetoothGattCallback() {

        /**
         * 연결 상태 변경되면 진입
         */
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d(
                TAG,
                "onConnectionStateChange: gatt : $gatt, status : $status, newState : $newState"
            )
            when (newState) {

                /**
                 * 연결 성공
                 */
                BluetoothProfile.STATE_CONNECTED -> {
                    stopBleScan()
                    gatt?.discoverServices()
                }


                /**
                 * 연결 해제 or 연결 실패
                 * connectedGatt?.disconnect() 호출 성공하면 진입
                 * peripheral의 address가 변경되는 디바이스와 자동연결을 시도하면 진입
                 */
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedGatt = null
                    isConnected = false
                    for (listener in listeners) {
                        listener.disConnect(gatt!!.device)
                    }
                }
            }
        }

        /**
         * gatt?.discoverServices() 호출하면 진입
         */
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    for (service in gatt!!.services) {
                        if (service.uuid.toString() == bleUuid.serviceUuid) {
                            if (!isConnected) {
                                for (characteristic in service.characteristics) {
                                    if (characteristic.uuid.toString() == bleUuid.charUuid) {
                                        gatt.setCharacteristicNotification(characteristic, true)
                                        connectedGatt = gatt
                                        connectedChar = characteristic
                                        isConnected = true
                                        for (listener in listeners) {
                                            listener.connect(gatt.device)
                                        }
                                    }
                                }
                            } else {
                                gatt.disconnect()
                            }
                        }
                    }
                }

                BluetoothGatt.GATT_FAILURE -> {
                    for (listener in listeners) {
                        listener.disConnect(gatt!!.device)
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (gatt != null && characteristic != null) {
                onCharacteristicChanged(gatt, characteristic, characteristic.value)
            }
        }

        /**
         * Peripheral 에서 notification 했을 때 진입
         */
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val message = changeToUTF8(value)
            if (message == "peripheralDisconnect") {
                disconnect()
            } else {
                for (listener in listeners) {
                    listener.readMessage(value, message, gatt.device.address)
                }
            }
        }
    }

    /**
     * 디바이스와 BLE 연결
     */
    fun connectToDevice(activity: Activity, device: BluetoothDevice, autoConnect: Boolean) {
        bluetoothAdapter.getRemoteDevice(device.address)
            ?.connectGatt(activity, autoConnect, bluetoothGattCallback)
    }

    /**
     * 연결 해제, 연결 해제에는 gatt 필요함
     */
    fun disconnect() {
        connectedGatt?.disconnect()
    }

    /**
     * 데이터 쓰기
     */
    fun writeData(message: String) {
        val data = changeToByte(message)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            connectedGatt?.writeCharacteristic(
                connectedChar!!,
                data,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
        } else {
            connectedChar?.value = data
            connectedGatt?.writeCharacteristic(connectedChar)
        }
        for (listener in listeners) {
            listener.writeMessage(message)
        }
    }

    fun changeToHex(value: ByteArray): String {
        var hexString = ""
        for (byte in value) {
            hexString += String.format("%02X ", byte)
        }
        return hexString
    }

    fun changeToUTF8(value: ByteArray): String {
        return String(value, StandardCharsets.UTF_8)
    }

    private fun changeToByte(message: String): ByteArray {
        return message.toByteArray(StandardCharsets.UTF_8)
    }

}