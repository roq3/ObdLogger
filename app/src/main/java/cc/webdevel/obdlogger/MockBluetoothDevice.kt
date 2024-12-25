package cc.webdevel.obdlogger

import java.util.*

class MockBluetoothDevice(
    private val name: String = "Mock Device",
    private val address: String = "00:11:22:33:44:55"
) : BluetoothDeviceInterface {
    override fun getName(): String {
        return name
    }

    override fun getAddress(): String {
        return address
    }

    override fun createRfcommSocketToServiceRecord(uuid: UUID?): BluetoothSocketInterface {
        return MockBluetoothSocket()
    }
}