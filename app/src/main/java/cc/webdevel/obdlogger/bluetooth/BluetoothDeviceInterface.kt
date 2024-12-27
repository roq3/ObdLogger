package cc.webdevel.obdlogger.bluetooth

import java.util.*

interface BluetoothDeviceInterface {
    fun getName(): String
    fun getAddress(): String
    fun createRfcommSocketToServiceRecord(uuid: UUID?): BluetoothSocketInterface
    fun createInsecureRfcommSocketToServiceRecord(uuid: UUID?): BluetoothSocketInterface
}