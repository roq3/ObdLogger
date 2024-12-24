package cc.webdevel.obdlogger

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
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
                                // Uruchom komendy OBD i zapisz odpowiedzi w mapie
                                response["speed"] = obdConnection.run(SpeedCommand()).value
                                response["rpm"] = obdConnection.run(RPMCommand()).value
                                response["massairflow"] = obdConnection.run(MassAirFlowCommand()).value
                                response["runtime"] = obdConnection.run(RuntimeCommand()).value
                                response["load"] = obdConnection.run(LoadCommand()).value
                                response["absoluteload"] = obdConnection.run(AbsoluteLoadCommand()).value
                                response["throttleposition"] = obdConnection.run(ThrottlePositionCommand()).value
                                response["relativethrottleposition"] = obdConnection.run(RelativeThrottlePositionCommand()).value

                                // Utwórz pojedynczy komunikat aktualizacji statusu
                                val statusUpdateMessage =
                                    "Speed: ${response["speed"]}, " +
                                            "RPM: ${response["rpm"]}, " +
                                            "Mass Air Flow: ${response["massairflow"]}, " +
                                            "Runtime: ${response["runtime"]}, " +
                                            "Load: ${response["load"]}, " +
                                            "Absolute Load: ${response["absoluteload"]}, " +
                                            "Throttle Position: ${response["throttleposition"]}, " +
                                            "Relative Throttle Position: ${response["relativethrottleposition"]}"

                                // Przetwórz odpowiedzi
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