package cc.webdevel.obdlogger.bluetooth

import java.io.InputStream
import java.io.OutputStream

interface BluetoothSocketInterface {
    fun getInputStream(): InputStream
    fun getOutputStream(): OutputStream
    fun connect()
    fun close()
}