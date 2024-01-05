package com.example.bleCentral.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import java.nio.charset.StandardCharsets
import java.util.UUID

@SuppressLint("MissingPermission")
object BlePeripheral {

    private const val TAG = "BlePeripheral"
    private const val chattingGattUuid = "fec26ec4-6d71-4442-9f81-55bc21d658d0"
    private const val chattingCharacteristicUuid = "fec26ec4-6d71-4442-9f81-55bc21d658d1"
    private const val descriptorUuid = "00002902-0000-1000-8000-00805f9b34fb"
    val listeners = ArrayList<BleListener>()
    var bluetoothAdapter: BluetoothAdapter? = null
    private var connectedChar: BluetoothGattCharacteristic? = null
    var serverGattServer: BluetoothGattServer? = null
    private var serverService: BluetoothGattService? = null
    private var serverDescriptor: BluetoothGattDescriptor? = null
    var connectDevice: BluetoothDevice? = null
    var isConnected = false
    var isAdvertising = false
    private var settings: AdvertiseSettings? = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setConnectable(true)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        .build()

    /**
     * advertising으로 보낼 수 있는 데이터의 길이는 31 바이트다
     * UUID와 name을 전부 보내면 31바이트가 넘어가기 때문에 이름은 안보냈다.
     * ServiceUuid를 추가해준 이유는 백그라운드 스캔을 위해서다.
     * sleep모드에서 Ble 스캔을 하기 위해서는 ScanFilter를 추가해야 한다.
     */
    private var advertiseData = AdvertiseData.Builder()
        .addServiceUuid(ParcelUuid(UUID.fromString(chattingGattUuid)))
        .setIncludeDeviceName(true)
        .setIncludeTxPowerLevel(false)
        .build()

    var advertiseCallback: AdvertiseCallback? = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.e("advertiseCallback", "Peripheral mode start")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("advertiseCallback", "Failed to add BLE advertisement, reason: $errorCode")
        }
    }

    fun startAdvertising(
        adapter: BluetoothAdapter,
        bluetoothManager: BluetoothManager,
        activity: Activity,
        name: String,
    ): Boolean {
        this.bluetoothAdapter = adapter

        if (!isConnected && !isAdvertising) {
            isAdvertising = true

            // 이름이 길면 31바이트를 넘어가기 때문에 줄였다.
            bluetoothAdapter!!.name = name
            serverGattServer = bluetoothManager.openGattServer(activity, advertiseGattCallback)

            serverService = BluetoothGattService(
                UUID.fromString(chattingGattUuid),
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            connectedChar = BluetoothGattCharacteristic(
                UUID.fromString(chattingCharacteristicUuid),
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            serverDescriptor = BluetoothGattDescriptor(
                UUID.fromString(descriptorUuid),
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )

            connectedChar?.addDescriptor(serverDescriptor)
            serverService?.addCharacteristic(connectedChar)
            serverGattServer?.addService(serverService)

            bluetoothAdapter!!.bluetoothLeAdvertiser?.startAdvertising(
                settings,
                advertiseData,
                advertiseCallback
            )
            Log.d(TAG, "startAdvertising: pppp advertising")
            return true
        } else {
            return false
        }
    }

    fun stopAdvertising() {
        bluetoothAdapter!!.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
    }

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

    @JvmStatic
    fun removeAllListener() {
        listeners.clear()
        Log.d(TAG, "removeAllListener() called listener : $listeners")
    }

    /**
     * BLE Peripheral Mode 기능들
     */
    private val advertiseGattCallback = object : BluetoothGattServerCallback() {

        /**
         * 연결 상태 변경되면 진입
         */
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            Log.d(
                TAG,
                "onConnectionStateChange() called with: device = $device, status = $status, newState = $newState"
            )
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                val bluetoothLeAdvertiser = bluetoothAdapter!!.bluetoothLeAdvertiser
                bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
                connectDevice = device
                isConnected = true
                isAdvertising = false
                for (listener in listeners) {
                    listener.connect(device!!)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                serverGattServer?.clearServices()
                serverGattServer?.close()
                isConnected = false
                for (listener in listeners) {
                    listener.disConnect(device!!)
                }
            }
        }

        /**
         * Central 에서 데이터를 Write 하면 진입
         */
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {

            value?.let {
                val message = changeToUTF8(it)
                for (listener in listeners) {
                    listener.readMessage(value, message, device!!.address)
                }
            }
            serverGattServer?.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                value
            )
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            serverGattServer?.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                value
            )
        }
    }

    /**
     * Advertising 이름 변경
     */
    fun changeAdvertisingName(name: String) {
        bluetoothAdapter?.name = name
    }

    /**
     * Peripheral 에서 데이터 보낼 때 사용
     */
    fun writeData(message: String) {
        val data = changeToByte(message)
        connectDevice?.let { device ->
            connectedChar?.let { character ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    serverGattServer?.notifyCharacteristicChanged(device, character, false, data)
                } else {
                    character.value = data
                    serverGattServer?.notifyCharacteristicChanged(device, character, false)
                }
            }
            for (listener in listeners) {
                listener.writeMessage(message)
            }
        }
    }

    fun changeToUTF8(value: ByteArray): String {
        return String(value, StandardCharsets.UTF_8)
    }

    private fun changeToByte(message: String): ByteArray {
        return message.toByteArray(StandardCharsets.UTF_8)
    }
}