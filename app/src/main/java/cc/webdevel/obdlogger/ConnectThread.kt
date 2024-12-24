package cc.webdevel.obdlogger

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.github.eltonvs.obd.command.ObdResponse
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import com.github.eltonvs.obd.command.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.InputStream
import java.io.OutputStream

class ConnectThread(
    private val device: BluetoothDevice,
    private val bluetoothAdapter: BluetoothAdapter,
    private val onStatusUpdate: (String) -> Unit,
    private val onError: (String) -> Unit
) : Thread() {
    private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        device.createRfcommSocketToServiceRecord(MainActivity.OBD_UUID)
    }
    private var isRunning = true

    override fun run() {
        try {
            bluetoothAdapter.cancelDiscovery()

            mmSocket?.let { socket ->
                try {
                    socket.connect()
                    val inputStream = socket.inputStream
                    val outputStream = socket.outputStream

                    // Teraz możesz utworzyć połączenie OBD
                    val obdConnection = ObdDeviceConnection(inputStream, outputStream)
                    // Użyj obdConnection zgodnie z potrzebami
                    onStatusUpdate("Connected to 'V-LINK'")

                    CoroutineScope(Dispatchers.IO).launch {
                        while (isRunning) {
                            try {
                                val response = mutableMapOf<String, Any>()

                                var speedVal = ""
                                try { speedVal = obdConnection.run(SpeedCommand()).formattedValue } catch (e: Exception) { onError("Error executing command Speed: ${e.message}") }

                                var rpmVal = ""
                                try { rpmVal = obdConnection.run(RPMCommand()).formattedValue } catch (e: Exception) { onError("Error executing command RPM: ${e.message}") }

                                var massAirFlowVal = ""
                                try { massAirFlowVal = obdConnection.run(MassAirFlowCommand()).formattedValue } catch (e: Exception) { onError("Error executing command Mass Air Flow: ${e.message}") }

                                var runtimeVal = ""
                                try { runtimeVal = obdConnection.run(RuntimeCommand()).formattedValue } catch (e: Exception) { onError("Error executing command Runtime: ${e.message}") }

                                var loadVal = ""
                                try { loadVal = obdConnection.run(LoadCommand()).formattedValue } catch (e: Exception) { onError("Error executing command Load: ${e.message}") }

                                var absoluteLoadVal = ""
                                try { absoluteLoadVal = obdConnection.run(AbsoluteLoadCommand()).formattedValue } catch (e: Exception) { onError("Error executing command Absolute Load: ${e.message}") }

                                var throttlePositionVal = ""
                                try { throttlePositionVal = obdConnection.run(ThrottlePositionCommand()).formattedValue } catch (e: Exception) { onError("Error executing command Throttle Position: ${e.message}") }

                                var relativeThrottlePositionVal = ""
                                try { relativeThrottlePositionVal = obdConnection.run(RelativeThrottlePositionCommand()).formattedValue } catch (e: Exception) { onError("Error executing command Relative Throttle Position: ${e.message}") }

                                // Utwórz pojedynczy komunikat aktualizacji statusu
                                val statusUpdateMessage =
                                    "Speed: $speedVal, " +
                                    "RPM: $rpmVal, " +
                                    "Mass Air Flow: $massAirFlowVal, " +
                                    "Runtime: $runtimeVal, " +
                                    "Load: $loadVal, " +
                                    "Absolute Load: $absoluteLoadVal, " +
                                    "Throttle Position: $throttlePositionVal, " +
                                    "Relative Throttle Position: $relativeThrottlePositionVal"

                                onStatusUpdate(statusUpdateMessage)

                            } catch (e: Exception) {
                                onError("Error executing command: ${e.message}")
                            }
                            delay(1000) // Opóźnienie o 1 sekundę
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