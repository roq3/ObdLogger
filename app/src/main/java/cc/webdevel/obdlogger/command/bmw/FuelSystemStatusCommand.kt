package cc.webdevel.obdlogger.command.bmw

import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse

class FuelSystemStatusCommand : ObdCommand() {

    override val tag = "FUEL_SYSTEM_STATUS_COMMAND"
    override val name = "Fuel System Status Command"
    override val mode = "01"
    override val pid = "03"

    override val handler = { it: ObdRawResponse ->
        val cleanedResponse = it.bufferedValue.joinToString("") { byte -> "%02X".format(byte) }
        if (cleanedResponse.length >= 8) {
            val statusBank1 = cleanedResponse.substring(4, 6)
            val statusBank2 = cleanedResponse.substring(6, 8)
            "Fuel System Status - Bank 1: ${getStatusDescription(statusBank1)}, Bank 2: ${getStatusDescription(statusBank2)}"
        } else {
            "Invalid response: $cleanedResponse"
        }
    }

    private fun getStatusDescription(status: String): String {
        return when (status.toInt(16)) {
            0 -> "The motor is off"
            1 -> "Open loop due to insufficient engine temperature"
            2 -> "Closed loop, using oxygen sensor feedback to determine fuel mix"
            4 -> "Open loop due to engine load OR fuel cut due to deceleration"
            8 -> "Open loop due to system failure"
            16 -> "Closed loop, using at least one oxygen sensor but there is a fault in the feedback system"
            else -> "Unknown status"
        }
    }
}