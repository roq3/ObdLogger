package cc.webdevel.obdlogger.mock

import cc.webdevel.obdlogger.bluetooth.BluetoothDeviceInterface
import cc.webdevel.obdlogger.bluetooth.BluetoothSocketInterface
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

    override fun createInsecureRfcommSocketToServiceRecord(uuid: UUID?): BluetoothSocketInterface {
        return MockBluetoothSocket()
    }
}