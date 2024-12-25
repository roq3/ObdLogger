package cc.webdevel.obdlogger

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import com.github.eltonvs.obd.command.at.*
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
                                    "Relative Throttle Position" to { RelativeThrottlePositionCommand() },

                                    "\nFuel Level" to { FuelLevelCommand() },
                                    "Fuel Consumption Rate" to { FuelConsumptionRateCommand() },
                                    "Fuel Type" to { FuelTypeCommand() },
                                    "Fuel Trim SHORT_TERM_BANK_1" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.SHORT_TERM_BANK_1) },
                                    "Fuel Trim SHORT_TERM_BANK_2" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.SHORT_TERM_BANK_2) },
                                    "Fuel Trim LONG_TERM_BANK_1" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.LONG_TERM_BANK_1) },
                                    "Fuel Trim LONG_TERM_BANK_2" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.LONG_TERM_BANK_2) },

                                    "\nCommanded Equivalence Ratio" to { CommandedEquivalenceRatioCommand() },
                                    "Fuel Air Equivalence Ratio OXYGEN_SENSOR_1" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_1) },
                                    "Fuel Air Equivalence Ratio OXYGEN_SENSOR_2" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_2) },
                                    "Fuel Air Equivalence Ratio OXYGEN_SENSOR_3" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_3) },
                                    "Fuel Air Equivalence Ratio OXYGEN_SENSOR_5" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_5) },
                                    "Fuel Air Equivalence Ratio OXYGEN_SENSOR_6" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_6) },
                                    "Fuel Air Equivalence Ratio OXYGEN_SENSOR_7" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_7) },
                                    "Fuel Air Equivalence Ratio OXYGEN_SENSOR_8" to { FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_8) },

                                    "\nBarometric Pressure" to { BarometricPressureCommand() },
                                    "Intake Manifold Pressure" to { IntakeManifoldPressureCommand() },
                                    "Fuel Pressure" to { FuelPressureCommand() },
                                    "Fuel Rail Pressure" to { FuelRailPressureCommand() },
                                    "Fuel Rail Gauge Pressure" to { FuelRailGaugePressureCommand() },

                                    "\nAir Intake Temperature" to { AirIntakeTemperatureCommand() },
                                    "Ambient Air Temperature" to { AmbientAirTemperatureCommand() },
                                    "Engine Coolant Temperature" to { EngineCoolantTemperatureCommand() },
                                    "Oil Temperature" to { OilTemperatureCommand() },

                                    "\nModuleVoltage" to { ModuleVoltageCommand() },
                                    "TimingAdvance" to { TimingAdvanceCommand() },
                                    "VIN" to { VINCommand() },

                                    "\nMIL ON/OFF" to { MILOnCommand() },
                                    "Distance MIL ON" to { DistanceMILOnCommand() },
                                    "Time Since MIL ON" to { TimeSinceMILOnCommand() },
                                    "DistanceSinceCodesCleared" to { DistanceSinceCodesClearedCommand() },
                                    "TimeSinceCodesCleared" to { TimeSinceCodesClearedCommand() },
                                    "DTCNumberCommand" to { DTCNumberCommand() },
                                    "TroubleCodes" to { TroubleCodesCommand() },
                                    "PendingTroubleCodes" to { PendingTroubleCodesCommand() },
                                    "PermanentTroubleCodes" to { PermanentTroubleCodesCommand() }
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