package cc.webdevel.obdlogger.mock

import cc.webdevel.obdlogger.command.bmw.FuelSystemStatusCommand
import cc.webdevel.obdlogger.command.bmw.OBDStandardsCommand
import cc.webdevel.obdlogger.command.bmw.OxygenSensorAirFuelCommand
import cc.webdevel.obdlogger.command.bmw.OxygenSensorCommand
import cc.webdevel.obdlogger.command.bmw.OxygenSensorsPresentCommand
import cc.webdevel.obdlogger.command.bmw.SecondaryAirStatusCommand
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.control.*
import com.github.eltonvs.obd.command.engine.*
import com.github.eltonvs.obd.command.fuel.*
import com.github.eltonvs.obd.command.temperature.*

// BMW E46 318i N42 OBD-II Codes List from OBD2 Torque Pro
class BMWCodes {

    // BMW E46 318i N42 OBD-II Codes List
    private val codesMap = mapOf(
        "01 01" to "Check Engine", // Number of trouble codes and I/M info
        "01 03" to "Fuel system status", // Fuel system status
        "01 04" to "Calculated engine load value", // Calculated engine load value
        "01 05" to "Engine coolant temperature", // Engine coolant temperature
        "01 06" to "Short term - Bank1", // Short term fuel % trim - Bank1
        "01 07" to "Long term - Bank1", // Long term fuel % trim - Bank1
        "01 08" to "Short term - Bank2", // Short term fuel % trim - Bank2
        "01 09" to "Long term - Bank2", // Long term fuel % trim - Bank2
        "01 0C" to "Engine RPM", // Engine RPM
        "01 0D" to "Vehicle speed", // Vehicle speed
        "01 0E" to "Timing advance", // Timing advance
        "01 0F" to "Intake air temperature", // Intake air temperature
        "01 10" to "MAF air flow rate", // MAF air flow rate
        "01 11" to "Throttle position", // Throttle position
        "01 12" to "Sec. air status", // Sec. air status
        "01 13" to "Oxygen sensors present", // Oxygen sensors present
        "01 14" to "Bank 1, Sensor 2", // Bank 1, Sensor 2:Oxygen sensor & Short Term Fuel Trim
        "01 15" to "Bank 1, Sensor 3", // Bank 1, Sensor 3:Oxygen sensor & Short Term Fuel Trim
        "01 1C" to "OBD standard", // OBD standards this vehicle conforms to
        "01 21" to "Distance traveled with malfunction indicator lamp on", // Distance traveled with malfunction indicator lamp on
        "01 24" to "O2 S1", // O2 S1 Equiv. Ratio and/or Current
        "01 25" to "O2 S5" // O2 S5 Equiv. Ratio and/or Current
    )

    // BMW E46 318i N42 OBD-II Codes List
    fun getCodes(): Map<String?, () -> ObdCommand> {
        return mapOf(
            codesMap["01 01"] to { MILOnCommand() },
            codesMap["01 03"] to { FuelSystemStatusCommand() },
            codesMap["01 04"] to { LoadCommand() },
            codesMap["01 05"] to { EngineCoolantTemperatureCommand() },
            codesMap["01 06"] to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.SHORT_TERM_BANK_1) },
            codesMap["01 07"] to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.LONG_TERM_BANK_1) },
            codesMap["01 08"] to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.SHORT_TERM_BANK_2) },
            codesMap["01 09"] to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.LONG_TERM_BANK_2) },
            codesMap["01 0C"] to { RPMCommand() },
            codesMap["01 0D"] to { SpeedCommand() },
            codesMap["01 0E"] to { TimingAdvanceCommand() },
            codesMap["01 0F"] to { AirIntakeTemperatureCommand() },
            codesMap["01 10"] to { MassAirFlowCommand() },
            codesMap["01 11"] to { ThrottlePositionCommand() },
            codesMap["01 12"] to { SecondaryAirStatusCommand() },
            codesMap["01 13"] to { OxygenSensorsPresentCommand() },
            codesMap["01 14"] to { OxygenSensorCommand(OxygenSensorCommand.OxygenSensor.BANK_1_SENSOR_2) },
            codesMap["01 15"] to { OxygenSensorCommand(OxygenSensorCommand.OxygenSensor.BANK_1_SENSOR_3) },
            codesMap["01 1C"] to { OBDStandardsCommand() },
            codesMap["01 21"] to { DistanceMILOnCommand() },
            codesMap["01 24"] to { OxygenSensorAirFuelCommand(OxygenSensorAirFuelCommand.OxygenSensor.O2_S1) },
            codesMap["01 25"] to { OxygenSensorAirFuelCommand(OxygenSensorAirFuelCommand.OxygenSensor.O2_S5) }
        )
    }
}