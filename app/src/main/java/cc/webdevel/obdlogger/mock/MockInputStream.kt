package cc.webdevel.obdlogger.mock

import java.io.InputStream
import kotlin.random.Random

class MockInputStream : InputStream() {
    private val commandResponses = mapOf(
        "01 0D" to { generateRandomHexValue(2) }, // SpeedCommand
        "01 0C" to { generateRandomHexValue(2) }, // RPMCommand (0 RPM)
        "01 10" to { generateRandomHexValue(4) }, // MassAirFlowCommand
        "01 0F" to { generateRandomHexValue(4) }, // RuntimeCommand
        "01 04" to { generateRandomHexValue(4) }, // LoadCommand
        "01 43" to { generateRandomHexValue(4) }, // AbsoluteLoadCommand
        "01 11" to { generateRandomHexValue(4) }, // ThrottlePositionCommand
        "01 45" to { generateRandomHexValue(4) }, // RelativeThrottlePositionCommand

        "01 2F" to { generateRandomHexValue(2) }, // FuelLevelCommand
        "01 5E" to { generateRandomHexValue(4) }, // FuelConsumptionRateCommand
        "01 51" to { generateRandomHexValue(2) }, // FuelTypeCommand
        "01 06" to { generateRandomHexValue(2) }, // FuelTrimCommand (SHORT_TERM_BANK_1)
        "01 07" to { generateRandomHexValue(2) }, // FuelTrimCommand (SHORT_TERM_BANK_2)
        "01 08" to { generateRandomHexValue(2) }, // FuelTrimCommand (LONG_TERM_BANK_1)
        "01 09" to { generateRandomHexValue(2) },  // FuelTrimCommand (LONG_TERM_BANK_2)

        "01 44" to { generateRandomHexValue(4) }, // CommandedEquivalenceRatioCommand
        "01 34" to { generateRandomHexValue(4) }, // FuelAirEquivalenceRatioCommand (OXYGEN_SENSOR_1)
        "01 35" to { generateRandomHexValue(4) }, // FuelAirEquivalenceRatioCommand (OXYGEN_SENSOR_2)
        "01 36" to { generateRandomHexValue(4) }, // FuelAirEquivalenceRatioCommand (OXYGEN_SENSOR_3)
        "01 37" to { generateRandomHexValue(4) }, // FuelAirEquivalenceRatioCommand (OXYGEN_SENSOR_4)
        "01 38" to { generateRandomHexValue(4) }, // FuelAirEquivalenceRatioCommand (OXYGEN_SENSOR_5)
        "01 39" to { generateRandomHexValue(4) }, // FuelAirEquivalenceRatioCommand (OXYGEN_SENSOR_6)
        "01 3A" to { generateRandomHexValue(4) }, // FuelAirEquivalenceRatioCommand (OXYGEN_SENSOR_7)
        "01 3B" to { generateRandomHexValue(4) }, // FuelAirEquivalenceRatioCommand (OXYGEN_SENSOR_8)

        "01 33" to { generateRandomHexValue(4) }, // BarometricPressureCommand
        "01 0B" to { generateRandomHexValue(4) }, // IntakeManifoldPressureCommand
        "01 0A" to { generateRandomHexValue(4) }, // FuelPressureCommand
        "01 23" to { generateRandomHexValue(4) }, // FuelRailPressureCommand
        "01 22" to { generateRandomHexValue(4) }, // FuelRailGaugePressureCommand

        "01 0F" to { generateRandomHexValue(2) }, // AirIntakeTemperatureCommand
        "01 46" to { generateRandomHexValue(2) }, // AmbientAirTemperatureCommand
        "01 05" to { generateRandomHexValue(2) }, // EngineCoolantTemperatureCommand
        "01 5C" to { generateRandomHexValue(2) }, // OilTemperatureCommand

        "01 42" to { generateRandomHexValue(4) }, // ModuleVoltageCommand
        "01 0E" to { generateRandomHexValue(4) },  // TimingAdvanceCommand
        "09 02" to { "41 02 49 4E 20 56 49 4E 20 31 32 33 34 35 36 37 38 39>" },  // VINCommand

//        "01 01" to { "41 01 80 00 00 00>" }, // MILOnCommand
        "01 01" to { "41 01 80 00 00 00>" }, // MILStatusCommand
        "01 21" to { generateRandomHexValue(4) }, // DistanceMILOnCommand
        "01 4D" to { generateRandomHexValue(4) }, // TimeSinceMILOnCommand
        "01 31" to { generateRandomHexValue(4) }, // DistanceSinceCCCommand
        "01 4E" to { generateRandomHexValue(4) }, // TimeSinceCCCommand
        "03" to { generateRandomHexValue(2) }, // TroubleCodesCommand
        "07" to { generateRandomHexValue(2) }, // PendingTroubleCodesCommand
        "0A" to { generateRandomHexValue(2) }, // PermanentTroubleCodesCommand

        "01 00" to { "41 00 BF 9F EC 11 41 00 80 00 00 00>" }, // AvailablePIDsCommand (01 20 80 00 00 00) PIDs from 01 to 20
        // BMW E46 318i N42 response: 01,03,04,05,06,07,08,09,0C,0D,0E,0F,10,11,12,13,15,16,1C,20,01
        "01 20" to { "41 20 80 00 00 00>" }, // AvailablePIDsCommand (01 20 80 00 00 00) PIDs from 21 to 40
        "01 40" to { "41 40 80 00 00 00>" }, // AvailablePIDsCommand (01 20 80 00 00 00) PIDs from 41 to 60
        "01 60" to { "41 60 80 00 00 00>" }, // AvailablePIDsCommand (01 20 80 00 00 00) PIDs from 61 to 80
        "01 80" to { "41 80 80 00 00 00>" }, // AvailablePIDsCommand (01 20 80 00 00 00) PIDs from 81 to A0

        "AT SP 0" to { "OK" }, // Set Protocol to Automatic
        "AT SP 3" to { "OK" }, // Set Protocol to ISO 9141-2
        "AT SP3" to { "OK" }, // Set Protocol to ISO 9141-2
        "AT SP 4" to { "OK" }, // Set Protocol to ISO ISO 14230-4 (KWP 5BAUD)
        "AT SP 5" to { "OK" }, // Set Protocol to ISO 14230-4 (KWP FAST)
        "At Sp A" to { "OK" }, // Set Protocol to SAE J1850 PWM
        "AT E0" to { "OK" }, // Turn off echo
        "AT E1" to { "OK" }, // Turn on echo
        "AT Z" to { "ELM327 v2.1" }, // Reset
        "AT I" to { "ELM327 v2.1" }, // Print the version ID
        "AT L1" to { "Line Feed ON" }, // Turn on line feed
        "AT L0" to { "Line Feed OFF" }, // Turn off line feed
        "AT IB 10" to { "10400" }, // Set ISO Baud rate to 10400
        "AT IB 96" to { "9600" }, // Set ISO Baud rate to 9600
        "AT IB 48" to { "4800" }, // Set ISO Baud rate to 4800
        "AT PPS" to { "PPS Summary" }, // Print a PP Summary
        "AT RV" to { "12.3V" }, // Read the voltage
        "AT ST 2a" to { "OK" }, // Set the timeout to 42 (42 * 4 = 168 ms)
        "AT ST d0" to { "OK" }, // Set the timeout to 208 (208 * 4 = 832 ms)
        "AT ST 1F4" to { "OK" }, // Set the timeout to 500 (500 * 4 = 2000 ms)
        "AT CAF0" to { "OK" }, // Disable automatic formatting
        "AT H1" to { "OK" }, // Turn on headers HEX (H1)
        "AT H0" to { "OK" }, // Turn off headers ASCII (H0)
        "AT SH 7E0" to { "OK" }, // Set the header to 7E0 Engine
        "AT SH 7E1" to { "OK" }, // Set the header to 7E1 Transmission
        "AT D" to { "OK" }, // Turn off the headers and set the protocol to automatic
        "AT S0" to { "OK" }, // Turn off spaces

        // BMW E46 318i N42 OBD-II Codes List
        "01 03" to { "41 03 01 02>" }, // Bank 1: 01, Bank 2: 02
        "01 12" to { "41 12 01>" }, // SecondaryAirStatusCommand
        "01 1C" to { "41 1C 01>" }, // OBDStandardsCommand
        "01 24" to { "41 24 1234>" }, // Response for O2 S1 Equiv. Ratio and/or Current
        "01 25" to { "41 25 5678>" }, // Response for O2 S5 Equiv. Ratio and/or Current
        "01 14" to { "41 14 1234>" }, // Response for Bank 1, Sensor 2:Oxygen sensor & Short Term Fuel Trim
        "01 15" to { "41 15 5678>" }, // Response for Bank 1, Sensor 3:Oxygen sensor & Short Term Fuel Trim
        "01 13" to { "41 13 01>" }, // Response for OxygenSensorsPresentCommand
    )

//        "01 0D" to "41 0D 001E>", // Response for SpeedCommand (30 Km/h)
//        "01 0C" to "41 0C 1AF8>", // Response for RPMCommand (3000 RPM)
//        "01 10" to "41 10 1234>", // Response for MassAirFlowCommand
//        "01 0F" to "41 1F 5678>", // Response for RuntimeCommand
//        "01 04" to "41 04 9ABC>", // Response for LoadCommand
//        "01 43" to "41 43 DEF0>", // Response for AbsoluteLoadCommand
//        "01 11" to "41 11 1357>", // Response for ThrottlePositionCommand
//        "01 45" to "41 45 2468>"  // Response for RelativeThrottlePositionCommand

    private var currentResponse: String? = null
    private var index = 0

    fun setCommand(command: String) {
        currentResponse = commandResponses[command]?.invoke()
        index = 0
    }

    private fun generateRandomHexValue(length: Int): String {
        val randomValue = (1..length).joinToString("") { Random.nextInt(0, 16).toString(16).padStart(2, '0').uppercase() }
        return "41 ${randomValue}>"
    }

    override fun available(): Int {
        return currentResponse?.let {
            it.length - index
        } ?: 0
    }

    override fun read(): Int {
        return currentResponse?.let {
            if (index < it.length) {
                it[index++].code
            } else {
                -1 // End of stream
            }
        } ?: -1
    }
}