package cc.webdevel.obdlogger

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import cc.webdevel.obdlogger.bluetooth.BluetoothDeviceInterface
import cc.webdevel.obdlogger.bluetooth.BluetoothSocketInterface
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import com.github.eltonvs.obd.command.engine.*
import com.github.eltonvs.obd.command.fuel.*
import com.github.eltonvs.obd.command.pressure.*
import com.github.eltonvs.obd.command.temperature.*
import com.github.eltonvs.obd.command.control.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL

class ConnectThread(
    private val device: BluetoothDeviceInterface,
    private val bluetoothAdapter: BluetoothAdapter,
    private val onStatusUpdate: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val uploadUrl: String,
    private val isToggleOn: Boolean
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
                                    "Relative Throttle Position" to { RelativeThrottlePositionCommand() },

                                    "Fuel Level" to { FuelLevelCommand() },
                                    "Fuel Consumption Rate" to { FuelConsumptionRateCommand() },
                                    "Fuel Type" to { FuelTypeCommand() },
                                    "Fuel Trim SHORT_TERM_BANK_1" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.SHORT_TERM_BANK_1) },
                                    "Fuel Trim SHORT_TERM_BANK_2" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.SHORT_TERM_BANK_2) },
                                    "Fuel Trim LONG_TERM_BANK_1" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.LONG_TERM_BANK_1) },
                                    "Fuel Trim LONG_TERM_BANK_2" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.LONG_TERM_BANK_2) },

                                    "Commanded Equivalence Ratio" to { CommandedEquivalenceRatioCommand() },
                                    "Fuel Air Equivalence Ratio OXYGEN_SENSOR_1" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_1) },
                                    "Fuel Air Equivalence Ratio OXYGEN_SENSOR_2" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_2) },
                                    "Fuel Air Equivalence Ratio OXYGEN_SENSOR_3" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_3) },
                                    "Fuel Air Equivalence Ratio OXYGEN_SENSOR_5" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_5) },
                                    "Fuel Air Equivalence Ratio OXYGEN_SENSOR_6" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_6) },
                                    "Fuel Air Equivalence Ratio OXYGEN_SENSOR_7" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_7) },
                                    "Fuel Air Equivalence Ratio OXYGEN_SENSOR_8" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_8) },

                                    "Barometric Pressure" to { BarometricPressureCommand() },
                                    "Intake Manifold Pressure" to { IntakeManifoldPressureCommand() },
                                    "Fuel Pressure" to { FuelPressureCommand() },
                                    "Fuel Rail Pressure" to { FuelRailPressureCommand() },
                                    "Fuel Rail Gauge Pressure" to { FuelRailGaugePressureCommand() },

                                    "Air Intake Temperature" to { AirIntakeTemperatureCommand() },
                                    "Ambient Air Temperature" to { AmbientAirTemperatureCommand() },
                                    "Engine Coolant Temperature" to { EngineCoolantTemperatureCommand() },
                                    "Oil Temperature" to { OilTemperatureCommand() },

                                    "Module Voltage" to { ModuleVoltageCommand() },
                                    "Timing Advance" to { TimingAdvanceCommand() },
                                    "VIN" to { VINCommand() },

                                    "MIL ON/OFF" to { MILOnCommand() },
                                    "Distance MIL ON" to { DistanceMILOnCommand() },
                                    "Time Since MIL ON" to { TimeSinceMILOnCommand() },
                                    "Distance Since Codes Cleared" to { DistanceSinceCodesClearedCommand() },
                                    "Time Since Codes Cleared" to { TimeSinceCodesClearedCommand() },
                                    "DTC Number" to { DTCNumberCommand() },
                                    "Trouble Codes" to { TroubleCodesCommand() },
                                    "Pending Trouble Codes" to { PendingTroubleCodesCommand() },
                                    "Permanent Trouble Codes" to { PermanentTroubleCodesCommand() }
                                )

                                // Execute commands and update status
                                var statusUpdateMessage = ""
                                val commandResults = mutableMapOf<String, String>()

                                each@ for ((key, value) in commands) {
                                    try {
                                        val commandVal = obdConnection.run(value()).formattedValue
                                        statusUpdateMessage += "$key: $commandVal, \n"
                                        commandResults[key] = commandVal
                                    } catch (e: Exception) {
                                        onError("Error executing command $key: ${e.message}")
                                    }
                                }

                                // Send command results to server if toggle is on
                                if (isToggleOn) {
                                    sendResultsToServer(uploadUrl, commandResults)
                                } else {
                                    onStatusUpdate(statusUpdateMessage)
                                    onError("")
                                }

                            } catch (e: Exception) {
                                onError("Error executing command: ${e.message}")
                            }
                            delay(3000)
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

    private fun sendResultsToServer(url: String, data: Map<String, String>) {
        val maxRetries = 3
        var attempt = 0
        var success = false

        while (attempt < maxRetries && !success) {
            try {
                val json = JSONObject(data).toString()
                val urlConnection = URL(url).openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("Content-Type", "application/json")
                urlConnection.setRequestProperty("Accept", "application/json")
                urlConnection.doOutput = true

                urlConnection.outputStream.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }

                val responseCode = urlConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    onStatusUpdate("Data sent successfully: ${json.take(200)}...")
                    onError("");
                    success = true
                } else {
                    onError("Failed to send data: HTTP $responseCode")
                }
            } catch (e: Exception) {
                onError("Error sending data: ${e.message}")
            }

            attempt++
            if (!success) {
                Thread.sleep(2000) // Wait for 2 seconds before retrying
            }
        }

        if (!success) {
            onError("Failed to send data after $maxRetries attempts")
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