package cc.webdevel.obdlogger.command.bmw

import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse

class SecondaryAirStatusCommand : ObdCommand() {

    override val tag = "SECONDARY_AIR_STATUS_COMMAND"
    override val name = "Secondary Air Status Command"
    override val mode = "01"
    override val pid = "12"

    override val handler = { it: ObdRawResponse ->
        val cleanedResponse = it.bufferedValue.joinToString("") { byte -> "%02X".format(byte) }
        if (cleanedResponse.length >= 4) {
            val status = cleanedResponse.substring(4, 6)
            "Secondary Air Status: ${getStatusDescription(status)}"
        } else {
            "Invalid response: $cleanedResponse"
        }
    }

    private fun getStatusDescription(status: String): String {
        return when (status.toInt(16)) {
            1 -> "Upstream"
            2 -> "Downstream of catalytic converter"
            4 -> "From the outside atmosphere or off"
            8 -> "Pump commanded on for diagnostics"
            else -> "Invalid response"
        }
    }
}