package cc.webdevel.obdlogger

import android.bluetooth.BluetoothAdapter
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import com.github.eltonvs.obd.command.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class ConnectThread(
    private val device: BluetoothDeviceInterface,
    private val bluetoothAdapter: BluetoothAdapter,
    private val onStatusUpdate: (String) -> Unit,
    private val onError: (String) -> Unit
) : Thread() {
    private val mmSocket: BluetoothSocketInterface? by lazy(LazyThreadSafetyMode.NONE) {
        device.createRfcommSocketToServiceRecord(MainActivity.OBD_UUID)
    }
    private var isRunning = true

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
                                var speedVal = ""
                                try { speedVal = obdConnection.run(SpeedCommand()).formattedValue } catch (e: Exception) { onError("Error executing command Speed: ${e.toString()}") }

                                var rpmVal = ""
                                try { rpmVal = obdConnection.run(RPMCommand()).formattedValue } catch (e: Exception) { onError("Error executing command RPM: ${e.toString()}") }

                                var massAirFlowVal = ""
                                try { massAirFlowVal = obdConnection.run(MassAirFlowCommand()).value } catch (e: Exception) { onError("Error executing command Mass Air Flow: ${e.toString()}") }

                                var runtimeVal = ""
                                try { runtimeVal = obdConnection.run(RuntimeCommand()).value } catch (e: Exception) { onError("Error executing command Runtime: ${e.toString()}") }

                                var loadVal = ""
                                try { loadVal = obdConnection.run(LoadCommand()).value } catch (e: Exception) { onError("Error executing command Load: ${e.toString()}") }

                                var absoluteLoadVal = ""
                                try { absoluteLoadVal = obdConnection.run(AbsoluteLoadCommand()).value } catch (e: Exception) { onError("Error executing command Absolute Load: ${e.message}") }

                                var throttlePositionVal = ""
                                try { throttlePositionVal = obdConnection.run(ThrottlePositionCommand()).value } catch (e: Exception) { onError("Error executing command Throttle Position: ${e.toString()}") }

                                var relativeThrottlePositionVal = ""
                                try { relativeThrottlePositionVal = obdConnection.run(RelativeThrottlePositionCommand()).value } catch (e: Exception) { onError("Error executing command Relative Throttle Position: ${e.toString()}") }

                                val statusUpdateMessage =
                                    "Speed: $speedVal, \n" +
                                    "RPM: $rpmVal, \n" +
                                    "Mass Air Flow: $massAirFlowVal, \n" +
                                    "Runtime: $runtimeVal, \n" +
                                    "Load: $loadVal, \n" +
                                    "Absolute Load: $absoluteLoadVal, \n" +
                                    "Throttle Position: $throttlePositionVal, \n" +
                                    "Relative Throttle Position: $relativeThrottlePositionVal"

                                onStatusUpdate(statusUpdateMessage)

                            } catch (e: Exception) {
                                onError("Error executing command: ${e.message}")
                            }
                            delay(1000)
                        }
                    }

                } catch (e: Exception) {
                    onError("Could not connect to device: ${e.message}")
                    try {
                        socket.close()
                    } catch (closeException: Exception) {
                        onError("Could not close the client socket: ${closeException.message}")
                    }
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