package cc.webdevel.obdlogger.mock

import cc.webdevel.obdlogger.bluetooth.BluetoothSocketInterface
import java.io.InputStream
import java.io.OutputStream

class MockBluetoothSocket : BluetoothSocketInterface {
    private val mockInputStream = MockInputStream()
    private val mockOutputStream = MockOutputStream(mockInputStream)

    override fun getInputStream(): InputStream {
        return mockInputStream
    }

    override fun getOutputStream(): OutputStream {
        return mockOutputStream
    }

    override fun connect() {
        // Simulate a successful connection
    }

    override fun close() {
        // Simulate closing the connection
    }
}