package cc.webdevel.obdlogger.command.bmw

import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse

class OBDStandardsCommand : ObdCommand() {

    override val tag = "OBD_STANDARDS_COMMAND"
    override val name = "OBD Standards Command"
    override val mode = "01"
    override val pid = "1C"

    override val handler = { it: ObdRawResponse ->
        val cleanedResponse = it.bufferedValue.joinToString("") { byte -> "%02X".format(byte) }
        if (cleanedResponse.length >= 2) {
            val value = cleanedResponse.takeLast(2).toInt(16)
            getStandardDescription(value)
        } else {
            "Invalid response: $cleanedResponse"
        }
    }

    private fun getStandardDescription(value: Int): String {
        return when (value) {
            1 -> "OBD-II as defined by the CARB"
            2 -> "OBD as defined by the EPA"
            3 -> "OBD and OBD-II"
            4 -> "OBD-I"
            5 -> "Not OBD compliant"
            6 -> "EOBD (Europe)"
            7 -> "EOBD and OBD-II"
            8 -> "EOBD and OBD"
            9 -> "EOBD, OBD and OBD II"
            10 -> "JOBD (Japan)"
            11 -> "JOBD and OBD II"
            12 -> "JOBD and EOBD"
            13 -> "JOBD, EOBD, and OBD II"
            14 -> "Reserved"
            15 -> "Reserved"
            16 -> "Reserved"
            17 -> "Engine Manufacturer Diagnostics (EMD)"
            18 -> "Engine Manufacturer Diagnostics Enhanced (EMD+)"
            19 -> "Heavy Duty On-Board Diagnostics (Child/Partial) (HD OBD-C)"
            20 -> "Heavy Duty On-Board Diagnostics (HD OBD)"
            21 -> "World Wide Harmonized OBD (WWH OBD)"
            22 -> "Reserved"
            23 -> "Heavy Duty Euro OBD Stage I without NOx control (HD EOBD-I)"
            24 -> "Heavy Duty Euro OBD Stage I with NOx control (HD EOBD-I N)"
            25 -> "Heavy Duty Euro OBD Stage II without NOx control (HD EOBD-II)"
            26 -> "Heavy Duty Euro OBD Stage II with NOx control (HD EOBD-II N)"
            27 -> "Reserved"
            28 -> "Brazil OBD Phase 1 (OBDBr-1)"
            29 -> "Brazil OBD Phase 2 (OBDBr-2)"
            30 -> "Korean OBD (KOBD)"
            31 -> "India OBD I (IOBD I)"
            32 -> "India OBD II (IOBD II)"
            33 -> "Heavy Duty Euro OBD Stage VI (HD EOBD-IV)"
            in 34..250 -> "Reserved"
            in 251..255 -> "Not available for assignment (SAE J1939 special meaning)"
            else -> "Unknown standard"
        }
    }
}