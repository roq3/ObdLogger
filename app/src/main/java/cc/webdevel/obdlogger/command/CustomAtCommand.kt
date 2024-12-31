package cc.webdevel.obdlogger.command

import com.github.eltonvs.obd.command.ATCommand
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse
import com.github.eltonvs.obd.command.control.AvailablePIDsCommand
import com.github.eltonvs.obd.command.getBitAt

// https://www.matthewsvolvosite.com/forums/viewtopic.php?t=67588&start=14
// https://www.sparkfun.com/datasheets/Widgets/ELM327_AT_Commands.pdf
// https://www.letsnurture.com/blog/obd-2-bluetooth-communication-in-android-with-kotlin.html

class IdentifyCommand : ATCommand() {

    // Required
    override val tag = "IDENTIFY_COMMAND"
    override val name = "Identify Command"
    override val pid = "I"
}

class IsoBaudCommand(value: Int) : ATCommand() {

    // Required
    override val tag = "ISO_BAUD_COMMAND"
    override val name = "ISO Baud Command"
    override val pid = "IB $value"

    // 10 => 10400
    // 96 => 9600
    // 48 => 4800
}

// Print a PP Summary
class PPSCommand() : ATCommand() {

    // Required
    override val tag = "PPS_COMMAND"
    override val name = "PPS Command"
    override val pid = "PPS"
}

// Read Voltage
class ReadVoltageCommand() : ATCommand() {

    // Required
    override val tag = "READ_VOLTAGE_COMMAND"
    override val name = "Read Voltage Command"
    override val pid = "RV"
}

// Custom AT Command
class CustomATCommand(override val pid: String) : ATCommand() {

    // Required
    override val tag = "CUSTOM_AT_COMMAND"
    override val name = "Custom AT Command"
}

// Disable automatic formatting
// Command for CAN only
class DisableAutoFormattingCommand() : ATCommand() {

    // Required
    override val tag = "DISABLE_AUTOMATIC_FORMATTING_COMMAND"
    override val name = "Disable Automatic Formatting Command"
    override val pid = "CAF0"
}

// Turn on headers
class SetHeaderCommand(header: String) : ATCommand() {

    // Required
    override val tag = "SET_HEADER_COMMAND"
    override val name = "Set Header Command"
    override val pid = "SH $header"
}

// Set All to Defaults
class SetDefaultsCommand() : ATCommand() {

    // Required
    override val tag = "SET_DEFAULTS_COMMAND"
    override val name = "Set Defaults Command"
    override val pid = "D"
}

class AvailablePIDsCustomCommand(private val range: AvailablePIDsCustomCommand.AvailablePIDsRanges) : ObdCommand() {
    override val tag = "AVAILABLE_COMMANDS_${range.name}"
    override val name = "Available Commands - ${range.displayName}"
    override val mode = "01"
    override val pid = range.pid

    override val defaultUnit = ""
    override val handler = { it: ObdRawResponse ->
        parseCustomPIDs(it.processedValue).joinToString(",") { "%02X".format(it) }
    }

    private fun parsePIDs(rawValue: String): IntArray {
        val value = rawValue.toLong(radix = 16)
        val initialPID = range.pid.toInt(radix = 16)
        return (1..33).fold(intArrayOf()) { acc, i ->
            if (value.getBitAt(i) == 1) acc.plus(i + initialPID) else acc
        }
    }

    private fun parseCustomPIDs(rawValue: String): IntArray {
        val responses = rawValue.chunked(12).filter { it.startsWith("4100") }
        val pids = mutableListOf<Int>()

        responses.forEach { response ->
            parsePIDs(response).forEach { pid ->
                pids.add(pid)
            }
        }

        return pids.toIntArray()
    }

    enum class AvailablePIDsRanges(val displayName: String, internal val pid: String) {
        PIDS_01_TO_20("PIDs from 01 to 20", "00"),
        PIDS_21_TO_40("PIDs from 21 to 40", "20"),
        PIDS_41_TO_60("PIDs from 41 to 60", "40"),
        PIDS_61_TO_80("PIDs from 61 to 80", "60"),
        PIDS_81_TO_A0("PIDs from 81 to A0", "80")
    }
}