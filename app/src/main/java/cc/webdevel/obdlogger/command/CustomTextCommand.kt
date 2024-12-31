package cc.webdevel.obdlogger.command

import com.github.eltonvs.obd.command.ObdCommand

// Custom OBD Command
class CustomTextCommand(override val mode: String, override val pid: String) : ObdCommand() {

    // Required
    override val tag = "CustomTextCommand"
    override val name = "Custom Text Command"
}

// Custom OBD Command
class CustomObdCommand(command: String) : ObdCommand() {

    // Required
    override val tag = "CUSTOM_OBD_COMMAND"
    override val name = "Custom OBD Command"
    override val mode = command
    override val pid = ""
    override val skipDigitCheck = true

    val customRawCommand = command
}