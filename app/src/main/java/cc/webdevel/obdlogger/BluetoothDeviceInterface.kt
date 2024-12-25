package cc.webdevel.obdlogger

import java.util.*
import java.io.InputStream
import java.io.OutputStream

interface BluetoothDeviceInterface {
    fun getName(): String
    fun getAddress(): String
    fun createRfcommSocketToServiceRecord(uuid: UUID?): BluetoothSocketInterface
}