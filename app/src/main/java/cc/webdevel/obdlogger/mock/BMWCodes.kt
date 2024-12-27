package cc.webdevel.obdlogger.mock

import cc.webdevel.obdlogger.command.bmw.FuelSystemStatusCommand
import cc.webdevel.obdlogger.command.bmw.OBDStandardsCommand
import cc.webdevel.obdlogger.command.bmw.OxygenSensorAirFuelCommand
import cc.webdevel.obdlogger.command.bmw.OxygenSensorCommand
import cc.webdevel.obdlogger.command.bmw.OxygenSensorsPresentCommand
import cc.webdevel.obdlogger.command.bmw.SecondaryAirStatusCommand
import com.github.eltonvs.obd.command.control.*
import com.github.eltonvs.obd.command.engine.*
import com.github.eltonvs.obd.command.fuel.*
import com.github.eltonvs.obd.command.temperature.*

// BMW E46 318i N42 OBD-II Codes List
class BMWCodes {

    val codesMap = mapOf(
        "01 01" to "Number of trouble codes and I/M info",
        "01 03" to "Fuel system status",
        "01 04" to "Calculated engine load value",
        "01 05" to "Engine coolant temperature",
        "01 06" to "Short term fuel % trim - Bank1",
        "01 07" to "Long term fuel % trim - Bank1",
        "01 08" to "Short term fuel % trim - Bank2",
        "01 09" to "Long term fuel % trim - Bank2",
        "01 0C" to "Engine RPM",
        "01 0D" to "Vehicle speed",
        "01 0E" to "Timing advance",
        "01 0F" to "Intake air temperature",
        "01 10" to "MAF air flow rate",
        "01 11" to "Throttle position",
        "01 12" to "Sec. air status",
        "01 13" to "Oxygen sensors present",
        "01 14" to "Bank 1, Sensor 2:Oxygen sensor & Short Term Fuel Trim",
        "01 15" to "Bank 1, Sensor 3:Oxygen sensor & Short Term Fuel Trim",
        "01 1C" to "OBD standards this vehicle conforms to",
        "01 21" to "Distance traveled with malfunction indicator lamp on",
        "01 24" to "O2 S1 Equiv. Ratio and/or Current",
        "01 25" to "O2 S5 Equiv. Ratio and/or Current"
    )

    fun getCodes(): Map<String, () -> Any> {
        return mapOf(
            "Number of trouble codes and I/M info" to { MILOnCommand() },
            "Fuel system status" to { FuelSystemStatusCommand() },
            "Calculated engine load value" to { LoadCommand() },
            "Engine coolant temperature" to { EngineCoolantTemperatureCommand() },
            "Short term fuel % trim - Bank1" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.SHORT_TERM_BANK_1) },
            "Long term fuel % trim - Bank1" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.LONG_TERM_BANK_1) },
            "Short term fuel % trim - Bank2" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.SHORT_TERM_BANK_2) },
            "Long term fuel % trim - Bank2" to { FuelTrimCommand(FuelTrimCommand.FuelTrimBank.LONG_TERM_BANK_2) },
            "Engine RPM" to { RPMCommand() },
            "Vehicle speed" to { SpeedCommand() },
            "Timing advance" to { TimingAdvanceCommand() },
            "Intake air temperature" to { AirIntakeTemperatureCommand() },
            "MAF air flow rate" to { MassAirFlowCommand() },
            "Throttle position" to { ThrottlePositionCommand() },
            "Sec. air status" to { SecondaryAirStatusCommand() },
             "Oxygen sensors present" to { OxygenSensorsPresentCommand() },
            "Bank 1, Sensor 2:Oxygen sensor & Short Term Fuel Trim" to { OxygenSensorCommand(
                OxygenSensorCommand.OxygenSensor.BANK_1_SENSOR_2) },
            "Bank 1, Sensor 3:Oxygen sensor & Short Term Fuel Trim" to { OxygenSensorCommand(
                OxygenSensorCommand.OxygenSensor.BANK_1_SENSOR_3) },
            "OBD standards this vehicle conforms to" to { OBDStandardsCommand() },
            "Distance traveled with malfunction indicator lamp on" to { DistanceMILOnCommand() },
            "O2 S1 Equiv. Ratio and/or Current" to { OxygenSensorAirFuelCommand(
                OxygenSensorAirFuelCommand.OxygenSensor.O2_S1) },
            "O2 S5 Equiv. Ratio and/or Current" to { OxygenSensorAirFuelCommand(
                OxygenSensorAirFuelCommand.OxygenSensor.O2_S5) }
        )
    }
}