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
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import com.example.bleCentral.foreground.ForegroundUtil
import java.nio.charset.StandardCharsets

/**
 * 자동 연결 로직
 * 1. 사용자가 연결을 직접 끊은 것인지 아닌지 확인
 * 2. 사용자가 연결을 직접 끊은 것이면 연결 해제 된 상태로 마무리, connectedGatt = null 처리
 * 3. 거리가 멀어지거나 BLE on/off, power on/off 등의 이유로 끊어졌다면 다시 연결 필요,  connectedGatt 유지
 * 4. 스캔 시도 실행 connectedGatt와 동일한 이름의 advertising이 있다면 연결 시도
 * 5. 스캔을 무한히 시도를 할 수는 없다. 그래서 주기적으로 워치와 연결이 끊어 졌다는 알림을 보낸다.
 *    그리고 그 알림을 클릭해서 연결 해제를 하면 스캔 중단. connectedGatt = null 처리
 * 6. foreground 서비스를 종료한 경우 -> 연결 해제 처리 connectedGatt = null 처리
 */
@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
@SuppressLint("MissingPermission")
class BleCentral(
    private var context: Context,
    private var bluetoothAdapter: BluetoothAdapter,
    private var bleUuid: BleUuid
) {

    companion object {
        private const val TAG = "BleCentral"
    }

    val listeners = ArrayList<BleListener>()

    /**
     * 스캔중인지 아닌지
     */
    private var isScanning = false

    /**
     * 연결된 device gatt
     */
    var connectedGatt: BluetoothGatt? = null

    /**
     * 연결된 device Characteristic
     */
    private var connectedChar: BluetoothGattCharacteristic? = null

    /**
     * 연결이 됐는지 아닌지
     */
    var isConnected = false

    /**
     * 자동연결을 시도중인지 아닌지  체크
     */
    private var tryAutoConnecting = false


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
                "onScanResult: name : ${result.device.name}, address : ${result.device.address}"
            
            )

            if (!result.device.name.isNullOrEmpty()) {
                if (tryAutoConnecting) {
                    if (connectedGatt != null && result.device.name == connectedGatt!!.device.name) {
                        stopBleScan()
                        Log.d(TAG, "onConnectionStateChange: scan stop 4")
                        connectToDevice(context, result.device, false)
                    }
                    return
                }

                for (bleListener in listeners) {
                    bleListener.scannedDevice(result)
                }
            }

        }
    }

    /**
     * 스캔 시작
     * https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner#startScan(android.bluetooth.le.ScanCallback)
     * sleep 모드에서 scan을 하기 위해서는 scanFilter에 service를 추가해야 한다.
     * SCAN_MODE_LOW_POWER - 저전력 모드에서 Bluetooth LE 스캔을 수행. 전력을 가장 적게 소모하는 기본 스캔 모드. 스캐너는 0.5초 동안 스캔하고 4.5초 동안 정지.
     * 만약 Bluetooth LE 장치가 이 모드에서 발견되기 위해서는 advertising 주기를 빠르게 해야 함(최소 100ms당 한 번)
     * 그렇지 않으면 검색 간격에서 일부 또는 모든 광고 이벤트를 놓칠 수 있음. 이 모드는 백그라운드에서 스캔할 때 사용
     * SCAN_MODE_BALANCED - 균형 잡힌 전원 모드에서 Bluetooth LE 스캔을 수행. 스캔 결과는 스캔 주파수와 전력 소비 사이에 적절한 균형을 제공하는 속도로 반환.
     * 스캐너는 2초 동안 스캔한 후 3초 동안 정지.
     * SCAN_MODE_LOW_LATENCY - 가장 높은 듀티 사이클을 사용하여 스캔합. 애플리케이션이 포그라운드에서 실행 중일 때만 이 모드를 사용하는 것을 추천
     * SCAN_MODE_OPPORTUNISTIC - 특별한 Bluetooth LE 스캔 모드. 이 스캔 모드를 사용하는 애플리케이션은 BLE 스캔 자체를 시작하지 않고 수동적으로 다른 스캔 결과를 수신.
     */
    fun startBleScan(scanTime: Long = 10000, auto: Boolean = false) {
        Log.d(
            TAG,
            "startBleScan() called with: scanTime = $scanTime, tryAutoConnecting : $tryAutoConnecting"
        )
        if (bluetoothAdapter.bluetoothLeScanner == null) return
        if (!bluetoothAdapter.isEnabled) return

        if (isScanning) {
            stopBleScan()
            Log.d(TAG, "onConnectionStateChange: scan stop 2")
        }

        // 스캔 필터 설정
        // 특정 serviceUuid만 스캔할 때 사용
        // 백그라운드 스캔을 위해서는 반드시 설정해야 한다.
        val filterList = listOf<ScanFilter>(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(bleUuid.serviceUuid))
                .build(),
        )
        Log.d(TAG, "startBleScan: filter 설정 tryAutoConnecting : $tryAutoConnecting")

        // 자동연결 로직, 백그라운드에서도 돌아가야 함
        if (tryAutoConnecting) {
            Log.d(TAG, "startBleScan: 자동연결 스캔 실행 ")
            bluetoothAdapter.bluetoothLeScanner!!.startScan(
                filterList,
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build(),
                leScanCallback
            )
            return
        }

        Log.d(TAG, "startBleScan: 일반 스캔 진행")
        // 자동연결이 아닌 경우
        // 백그라운드에서 실행 불가
        bluetoothAdapter.bluetoothLeScanner!!.startScan(
            filterList,
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
            leScanCallback
        )
        Handler(Looper.getMainLooper()).postDelayed({
            stopBleScan()
            Log.d(TAG, "onConnectionStateChange: scan stop 3")
        }, scanTime)

    }

    /**
     * 스캔 중지
     */
    fun stopBleScan() {
        Log.d(TAG, "stopBleScan() called")
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
            when (newState) {

                /**
                 * 연결 성공
                 */
                BluetoothProfile.STATE_CONNECTED -> {
                    stopBleScan()
                    Log.d(TAG, "onConnectionStateChange: scan stop 1")
                    gatt?.discoverServices()
                }

                /**
                 * 연결 해제 or 연결 실패
                 * 1. connectedGatt?.disconnect() 호출 성공하면 진입
                 * 2. peripheral의 address가 변경되는 디바이스와 자동연결을 시도하면 진입
                 * 3. 사용자의 action 없이 power off, bluetooth off, 거리 멀어진 경우에도 진입
                 *
                 * 3번의 경우 자동연결 로직 실행
                 */
                BluetoothProfile.STATE_DISCONNECTED -> {
                    isConnected = false
                    for (listener in listeners) {
                        listener.disConnect(gatt!!.device)
                    }

                    Log.d(TAG, "onConnectionStateChange: connectedGatt : $connectedGatt")
                    // connectedGatt 값이 있는 경우 자동연결 실행
                    if (connectedGatt != null) {
                        tryAutoConnecting = true
                        autoConnect()
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
                    Log.d(TAG, "onServicesDiscovered: GATT_SUCCESS")
                    for (service in gatt!!.services) {
                        Log.d(TAG, "onServicesDiscovered: service : ${service.uuid}")
                        if (service.uuid.toString() == bleUuid.serviceUuid) {
                            Log.d(
                                TAG,
                                "onServicesDiscovered() called with: isConnected : $isConnected"
                            )
                            if (!isConnected) {
                                for (characteristic in service.characteristics) {
                                    if (characteristic.uuid.toString() == bleUuid.charUuid) {
                                        gatt.setCharacteristicNotification(characteristic, true)
                                        Log.d(TAG, "onServicesDiscovered: startBleScan try false 처리 1111")
                                        connectedGatt = gatt
                                        connectedChar = characteristic
                                        isConnected = true
                                        for (listener in listeners) {
                                            listener.connect(gatt.device)
                                        }
                                    }
                                }
                            }
//                            else {
//                                gatt.disconnect()
//                            }
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
            when (val message = changeToUTF8(value)) {
                BleUtil.BLE_MESSAGE_PERIPHERAL_DISCONNECT -> disconnect()
                BleUtil.BLE_MESSAGE_LAUNCH_APP_NOTIFICATION -> ForegroundUtil.appLaunchNotification(
                    context,
                    "inhandLaunch"
                )

                else -> {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                    for (listener in listeners) {
                        listener.readMessage(value, message, gatt.device.address)
                    }
                }
            }
        }
    }

    /**
     * 디바이스와 BLE 연결
     */
    fun connectToDevice(context: Context, device: BluetoothDevice, autoConnect: Boolean) {
        bluetoothAdapter.getRemoteDevice(device.address)
            ?.connectGatt(context, autoConnect, bluetoothGattCallback)
    }

    /**
     * 디바이스와 BLE 연결
     */
    fun connectToDeviceByName(context: Context, deviceAddress: String, autoConnect: Boolean) {
        bluetoothAdapter.getRemoteDevice(deviceAddress)
            ?.connectGatt(context, autoConnect, bluetoothGattCallback)
    }

    /**
     * 연결 해제, 연결 해제에는 gatt 필요함
     * disconnect 함수를 호출 했다는 것은 사용자가 직접 연결을 해제한 경우,
     * connectedGatt = null 처리
     */
    fun disconnect() {
        connectedGatt?.disconnect()
        connectedGatt = null
        tryAutoConnecting = false
        Log.d(TAG, "onServicesDiscovered: startBleScan try false 처리 2222")
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
        Log.d(TAG, "writeData() called with: message = $message")
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


    /**
     * 자동연결 로직
     */
    private fun autoConnect() {
        Log.d(TAG, "autoConnect() called connectedGatt : $connectedGatt")
        if (connectedGatt == null) return
        startBleScan(auto = true)
    }
}