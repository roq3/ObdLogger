package cc.webdevel.obdlogger.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import java.util.*

class RealBluetoothDevice(private val device: BluetoothDevice) : BluetoothDeviceInterface {
    @SuppressLint("MissingPermission")
    override fun getName(): String {
        return device.name
    }

    override fun getAddress(): String {
        return device.address
    }

    @SuppressLint("MissingPermission")
    override fun createRfcommSocketToServiceRecord(uuid: UUID?): BluetoothSocketInterface {
        return RealBluetoothSocket(device.createRfcommSocketToServiceRecord(uuid))
    }
}