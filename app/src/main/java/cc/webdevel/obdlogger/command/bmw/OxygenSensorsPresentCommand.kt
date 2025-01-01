package cc.webdevel.obdlogger.command.bmw

import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse

class OxygenSensorsPresentCommand : ObdCommand() {

    override val tag = "OXYGEN_SENSORS_PRESENT_COMMAND"
    override val name = "Oxygen Sensors Present"
    override val mode = "01"
    override val pid = "13"

    override val handler = { it: ObdRawResponse ->
        val cleanedResponse = it.bufferedValue.joinToString("") { byte -> "%02X".format(byte) }
        if (cleanedResponse.length >= 4) {
            val sensors = cleanedResponse.substring(2, 4).toInt(16)
            "Oxygen Sensors Present: ${getSensorsDescription(sensors)}"
        } else {
            "Invalid response: $cleanedResponse"
        }
    }

    private fun getSensorsDescription(sensors: Int): String {
        val descriptions = mutableListOf<String>()
        if (sensors and 0x01 != 0) descriptions.add("Bank 1 - Sensor 1") // BMW E46
        if (sensors and 0x02 != 0) descriptions.add("Bank 1 - Sensor 2") // BMW E46
        if (sensors and 0x04 != 0) descriptions.add("Bank 1 - Sensor 3")
        if (sensors and 0x08 != 0) descriptions.add("Bank 1 - Sensor 4") // BMW E46
        if (sensors and 0x10 != 0) descriptions.add("Bank 2 - Sensor 1") // BMW E46
        if (sensors and 0x20 != 0) descriptions.add("Bank 2 - Sensor 2")
        if (sensors and 0x40 != 0) descriptions.add("Bank 2 - Sensor 3")
        if (sensors and 0x80 != 0) descriptions.add("Bank 2 - Sensor 4")
        return descriptions.joinToString(", ")
    }
}