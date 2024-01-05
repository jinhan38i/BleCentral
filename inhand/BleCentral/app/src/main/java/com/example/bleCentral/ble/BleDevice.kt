package com.example.bleCentral.ble

data class BleDevice(
    val name: String, val address: String,  val isBonded: Boolean,
)