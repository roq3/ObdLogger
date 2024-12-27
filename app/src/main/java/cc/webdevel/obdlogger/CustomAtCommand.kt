package cc.webdevel.obdlogger

import com.github.eltonvs.obd.command.ATCommand
import com.github.eltonvs.obd.command.ObdCommand

class IdentifyCommand : ATCommand() {

    // Required
    override val tag = "IDENTIFY_COMMAND"
    override val name = "Identify Command"
    override val mode = "AT"
    override val pid = "I"
    override val skipDigitCheck = true
}

// https://www.matthewsvolvosite.com/forums/viewtopic.php?t=67588&start=14
// https://www.sparkfun.com/datasheets/Widgets/ELM327_AT_Commands.pdf
// https://www.letsnurture.com/blog/obd-2-bluetooth-communication-in-android-with-kotlin.html
class IsoBaudCommand(value: Int) : ATCommand() {

    // Required
    override val tag = "ISO_BAUD_COMMAND"
    override val name = "ISO Baud Command"
    override val mode = "AT"
    override val pid = "IB $value"
    override val skipDigitCheck = true

    // 10 => 10400
    // 96 => 9600
    // 48 => 4800
}

// Print a PP Summary
class PPSCommand() : ATCommand() {

    // Required
    override val tag = "PPS_COMMAND"
    override val name = "PPS Command"
    override val mode = "AT"
    override val pid = "PPS"
    override val skipDigitCheck = true
}

// Read Voltage
class ReadVoltageCommand() : ATCommand() {

    // Required
    override val tag = "READ_VOLTAGE_COMMAND"
    override val name = "Read Voltage Command"
    override val mode = "AT"
    override val pid = "RV"
    override val skipDigitCheck = true
}

class CustomObdCommand(command: String) : ObdCommand() {

    // Required
    override val tag = "CUSTOM_OBD_COMMAND"
    override val name = "Custom OBD Command"
    override val mode = ""
    override val pid = command
    override val skipDigitCheck = true
}

class TimeoutCommand(timeout: Int) : ATCommand() {

    // Required
    override val tag = "TIMEOUT_COMMAND"
    override val name = "Timeout Command"
    override val mode = "AT"
    override val pid = "ST ${Integer.toHexString(0xFF and timeout)}"
    override val skipDigitCheck = true

}