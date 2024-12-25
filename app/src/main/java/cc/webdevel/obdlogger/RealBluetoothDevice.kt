package cc.webdevel.obdlogger

import android.bluetooth.BluetoothDevice
import java.util.*

class RealBluetoothDevice(private val device: BluetoothDevice) : BluetoothDeviceInterface {
    override fun getName(): String {
        return device.name
    }

    override fun getAddress(): String {
        return device.address
    }

    override fun createRfcommSocketToServiceRecord(uuid: UUID?): BluetoothSocketInterface {
        return RealBluetoothSocket(device.createRfcommSocketToServiceRecord(uuid))
    }
}