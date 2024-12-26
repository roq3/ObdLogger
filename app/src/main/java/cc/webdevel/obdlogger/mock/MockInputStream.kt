package cc.webdevel.obdlogger.mock

import java.io.InputStream
import kotlin.random.Random

class MockInputStream : InputStream() {
    private val commandResponses = mapOf(
        "01 0D" to { generateRandomHexValue(2) }, // SpeedCommand
        "01 0C" to { generateRandomHexValue(4) }, // RPMCommand
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

        "01 01" to { "41 01 80 00 00 00>" }, // MILOnCommand
        "01 21" to { generateRandomHexValue(4) }, // DistanceMILOnCommand
        "01 4D" to { generateRandomHexValue(4) }, // TimeSinceMILOnCommand
        "01 31" to { generateRandomHexValue(4) }, // DistanceSinceCCCommand
        "01 4E" to { generateRandomHexValue(4) }, // TimeSinceCCCommand
        "01 01" to { generateRandomHexValue(2) }, // DTCNumberCommand
        "03" to { generateRandomHexValue(2) }, // TroubleCodesCommand
        "07" to { generateRandomHexValue(2) }, // PendingTroubleCodesCommand
        "0A" to { generateRandomHexValue(2) }, // PermanentTroubleCodesCommand

        "AT SP 3" to { "OK" }, // Set Protocol to ISO 9141-2 BMW
        "AT SP 4" to { "OK" }, // Set Protocol to ISO 14230-4 (KWP2000 Fast) BMW
        "AT SP 5" to { "OK" }, // Set Protocol to ISO 14230-4 (KWP2000) BMW
        "AT E0" to { "OK OFF" }, // Turn off echo
        "AT E1" to { "OK ON" }, // Turn on echo
        "AT Z" to { "ELM327 v1.5" }, // Reset
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
//        currentResponse = commandResponses[command]
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