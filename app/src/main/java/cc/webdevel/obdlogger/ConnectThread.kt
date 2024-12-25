package cc.webdevel.obdlogger

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import com.github.eltonvs.obd.command.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID

class ConnectThread(
    private val device: BluetoothDeviceInterface,
    private val bluetoothAdapter: BluetoothAdapter,
    private val onStatusUpdate: (String) -> Unit,
    private val onError: (String) -> Unit
) : Thread() {

    companion object {
        private val OBD_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val mmSocket: BluetoothSocketInterface? by lazy(LazyThreadSafetyMode.NONE) {
        device.createRfcommSocketToServiceRecord(OBD_UUID)
    }
    private var isRunning = true

    @SuppressLint("MissingPermission")
    override fun run() {

        onStatusUpdate("Connected to '${device.getName()}'")

        try {
            bluetoothAdapter.cancelDiscovery()

            mmSocket?.let { socket ->
                try {

                    socket.connect()
                    val obdConnection = ObdDeviceConnection(socket.getInputStream(), socket.getOutputStream())

                    CoroutineScope(Dispatchers.IO).launch {
                        while (isRunning) {
                            try {

                                // Commands to be executed
                                val commands = mapOf(
                                    "Speed" to { SpeedCommand() },
                                    "RPM" to { RPMCommand() },
                                    "Mass Air Flow" to { MassAirFlowCommand() },
                                    "Runtime" to { RuntimeCommand() },
                                    "Load" to { LoadCommand() },
                                    "Absolute Load" to { AbsoluteLoadCommand() },
                                    "Throttle Position" to { ThrottlePositionCommand() },
                                    "Relative Throttle Position" to { RelativeThrottlePositionCommand() }
                                )

                                // Execute commands and update status
                                var statusUpdateMessage = ""

                                each@ for ((key, value) in commands) {
                                    try {
                                        val commandVal = obdConnection.run(value()).formattedValue
                                        statusUpdateMessage += "$key: $commandVal, \n"
                                    } catch (e: Exception) {
                                        onError("Error executing command $key: ${e.message}")
                                    }
                                }

                                // Update status
                                onStatusUpdate(statusUpdateMessage)

                            } catch (e: Exception) {
                                onError("Error executing command: ${e.message}")
                            }
                            delay(1000)
                        }
                    }

                } catch (e: Exception) {
                    onError("Could not connect to device: ${e.message}")
                }
            }
        } catch (e: Exception) {
            onError("Error in connection thread: ${e.message}")
        }
    }

    fun cancel() {
        isRunning = false
        try {
            mmSocket?.close()
        } catch (e: Exception) {
            onError("Could not close the client socket: ${e.message}")
        }
    }
}