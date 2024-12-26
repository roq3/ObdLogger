package cc.webdevel.obdlogger

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.icu.text.SimpleDateFormat
import cc.webdevel.obdlogger.bluetooth.BluetoothDeviceInterface
import cc.webdevel.obdlogger.bluetooth.BluetoothSocketInterface
import com.github.eltonvs.obd.command.ObdCommand
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
import java.util.Date
import java.util.Locale
import android.location.Location
import android.content.Context
import android.content.pm.PackageManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class ConnectThread(
    private val device: BluetoothDeviceInterface,
    private val bluetoothAdapter: BluetoothAdapter,
    private val onStatusUpdate: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val uploadUrl: String,
    private val isToggleOn: Boolean,
    private val context: Context,
    private val onDataUpdate: (String) -> Unit
) : Thread() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

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
                                    "GPS" to mapOf(
                                        "Latitude" to { getLocationSync()?.latitude ?: 0.0 },
                                        "Longitude" to { getLocationSync()?.longitude ?: 0.0 }
                                    ),
                                    "Main" to mapOf(
                                        "Date" to { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()) },
                                    ),
                                    "Engine" to mapOf(
                                        "Speed" to { SpeedCommand() },
                                        "RPM" to { RPMCommand() },
                                        "Mass Air Flow" to { MassAirFlowCommand() },
                                        "Runtime" to { RuntimeCommand() },
                                        "Load" to { LoadCommand() },
                                        "Absolute Load" to { AbsoluteLoadCommand() },
                                        "Throttle Position" to { ThrottlePositionCommand() },
                                        "Relative Throttle Position" to { RelativeThrottlePositionCommand() }
                                    ),
                                    "Fuel" to mapOf(
                                        "Fuel Level" to { FuelLevelCommand() },
                                        "Fuel Consumption Rate" to { FuelConsumptionRateCommand() },
                                        "Fuel Type" to { FuelTypeCommand() },
                                        "Fuel Trim SHORT_TERM_BANK_1" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.SHORT_TERM_BANK_1) },
                                        "Fuel Trim SHORT_TERM_BANK_2" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.SHORT_TERM_BANK_2) },
                                        "Fuel Trim LONG_TERM_BANK_1" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.LONG_TERM_BANK_1) },
                                        "Fuel Trim LONG_TERM_BANK_2" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.LONG_TERM_BANK_2) }
                                    ),
                                    "Equivalence Ratio" to mapOf(
                                        "Commanded Equivalence Ratio" to { CommandedEquivalenceRatioCommand() },
                                        "Fuel Air Equivalence Ratio OXYGEN_SENSOR_1" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_1) },
                                        "Fuel Air Equivalence Ratio OXYGEN_SENSOR_2" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_2) },
                                        "Fuel Air Equivalence Ratio OXYGEN_SENSOR_3" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_3) },
                                        "Fuel Air Equivalence Ratio OXYGEN_SENSOR_5" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_5) },
                                        "Fuel Air Equivalence Ratio OXYGEN_SENSOR_6" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_6) },
                                        "Fuel Air Equivalence Ratio OXYGEN_SENSOR_7" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_7) },
                                        "Fuel Air Equivalence Ratio OXYGEN_SENSOR_8" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_8) }
                                    ),
                                    "Pressure" to mapOf(
                                        "Barometric Pressure" to { BarometricPressureCommand() },
                                        "Intake Manifold Pressure" to { IntakeManifoldPressureCommand() },
                                        "Fuel Pressure" to { FuelPressureCommand() },
                                        "Fuel Rail Pressure" to { FuelRailPressureCommand() },
                                        "Fuel Rail Gauge Pressure" to { FuelRailGaugePressureCommand() }
                                    ),
                                    "Temperature" to mapOf(
                                        "Air Intake Temperature" to { AirIntakeTemperatureCommand() },
                                        "Ambient Air Temperature" to { AmbientAirTemperatureCommand() },
                                        "Engine Coolant Temperature" to { EngineCoolantTemperatureCommand() },
                                        "Oil Temperature" to { OilTemperatureCommand() }
                                    ),
                                    "Control" to mapOf(
                                        "Module Voltage" to { ModuleVoltageCommand() },
                                        "Timing Advance" to { TimingAdvanceCommand() },
                                        "VIN" to { VINCommand() }
                                    ),
                                    "MIL" to mapOf(
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
                                )

                                // Execute commands and update status
                                var obdDataMessage = ""
                                val commandResults = mutableMapOf<String, MutableMap<String, String>>()

                                commands.forEach { (groupKey, groupCommands) ->
                                    obdDataMessage += "\n$groupKey\n"
                                    groupCommands.forEach { (key, value) ->
                                        try {

                                            val commandVal = when (key) {
                                                "Date", "Latitude", "Longitude" -> value().toString()
                                                else -> obdConnection.run(value() as ObdCommand).formattedValue
                                            }

                                            obdDataMessage += "$key: $commandVal, \n"
                                            commandResults[groupKey] = commandResults[groupKey] ?: mutableMapOf()
                                            commandResults[groupKey]?.set(key, commandVal)
                                        } catch (e: Exception) {
                                            onError("Error executing command $key: ${e.message}")
                                        }
                                    }
                                }

                                // Send command results to server if toggle is on
                                if (isToggleOn) {
                                    sendResultsToServer(uploadUrl, commandResults)
                                    onDataUpdate(obdDataMessage)
                                } else {
                                    onDataUpdate(obdDataMessage)
                                    onStatusUpdate("Data is collected but not sent to server")
                                    onError("")
                                }

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

    private fun sendResultsToServer(url: String, data: MutableMap<String, MutableMap<String, String>>) {
        val maxRetries = 3
        var attempt = 0
        var success = false

        while (attempt < maxRetries && !success) {
            try {
                val json = JSONObject()
                data.forEach { (groupKey, groupData) ->
                    val groupJson = JSONObject()
                    groupData.forEach { (key, value) ->
                        groupJson.put(key, value)
                    }
                    json.put(groupKey, groupJson)
                }

                val urlConnection = URL(url).openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("Content-Type", "application/json")
                urlConnection.setRequestProperty("Accept", "application/json")
                urlConnection.doOutput = true

                urlConnection.outputStream.use { outputStream ->
                    outputStream.write(json.toString().toByteArray())
                }

                val responseCode = urlConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    onStatusUpdate("Data sent successfully: ${json.toString().take(100)}...")
                    onError("")
                    success = true
                } else {
                    onError("Failed to send data: HTTP $responseCode")
                }
            } catch (e: Exception) {
                onError("Error sending data: ${e.message}")
            }

            attempt++
            if (!success) {
                sleep(2000) // Wait for 2 seconds before retrying
            }
        }

        if (!success) {
            onError("Failed to send data after $maxRetries attempts")
        }
    }

    private fun getLocation(onLocationReceived: (Location?) -> Unit) {
        try {
            if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                val cancellationTokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    onLocationReceived(location)
                }.addOnFailureListener { e ->
                    onError("Error getting location: ${e.message}")
                    onLocationReceived(null)
                }
            } else {
                onError("Location permission not granted")
                onLocationReceived(null)
            }
        } catch (e: SecurityException) {
            onError("Location permission not granted: ${e.message}")
            onLocationReceived(null)
        } catch (e: Exception) {
            onError("Error getting location: ${e.message}")
            onLocationReceived(null)
        }
    }

    private fun getLocationSync(): Location? {
        var location: Location? = null
        val latch = CountDownLatch(1)
        getLocation { loc ->
            location = loc
            latch.countDown()
        }
        latch.await(5, TimeUnit.SECONDS) // Wait for up to 5 seconds for the location
        return location
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