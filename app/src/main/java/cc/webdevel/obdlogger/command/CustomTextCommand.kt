package cc.webdevel.obdlogger.command

import com.github.eltonvs.obd.command.ObdCommand

// Custom OBD Command
class CustomTextCommand(override val mode: String, override val pid: String) : ObdCommand() {

    // Required
    override val tag = "CustomTextCommand"
    override val name = "Custom Text Command"
}