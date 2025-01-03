package cc.webdevel.obdlogger.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import java.io.InputStream
import java.io.OutputStream

class RealBluetoothSocket(private val socket: BluetoothSocket) : BluetoothSocketInterface {
    override fun getInputStream(): InputStream {
        return socket.inputStream
    }

    override fun getOutputStream(): OutputStream {
        return socket.outputStream
    }

    @SuppressLint("MissingPermission")
    override fun connect() {
        socket.connect()
    }

    override fun close() {
        socket.close()
    }
}