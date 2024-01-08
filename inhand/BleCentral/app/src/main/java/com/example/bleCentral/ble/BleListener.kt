package com.example.bleCentral.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult

interface BleListener {
    fun scannedDevice(device: ScanResult)
    fun bondedDevice(bondedDevice: BluetoothDevice)
    fun stopScan()
    fun connect(device: BluetoothDevice)
    fun disConnect(device: BluetoothDevice)
    fun writeMessage(message: String)
    fun readMessage(byte: ByteArray, message: String, address: String)
}