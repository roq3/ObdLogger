package cc.webdevel.obdlogger.command

import com.github.eltonvs.obd.command.ATCommand
import com.github.eltonvs.obd.command.ObdCommand

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

// Custom OBD Command
class CustomObdCommand(command: String) : ObdCommand() {

    // Required
    override val tag = "CUSTOM_OBD_COMMAND"
    override val name = "Custom OBD Command"
    override val mode = ""
    override val pid = command
    override val skipDigitCheck = true
}

// Disable automatic formatting
class DisableAutoFormattingCommand() : ATCommand() {

    // Required
    override val tag = "DISABLE_AUTOMATIC_FORMATTING_COMMAND"
    override val name = "Disable Automatic Formatting Command"
    override val pid = "CAF0"
}