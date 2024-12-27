package cc.webdevel.obdlogger.command.bmw

import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse

class OxygenSensorAirFuelCommand(private val sensor: OxygenSensor) : ObdCommand() {

    enum class OxygenSensor(val pid: String) {
        O2_S1("24"),
        O2_S5("25")
    }

    override val tag = "OXYGEN_SENSOR_COMMAND"
    override val name = "Oxygen Sensor ${sensor.name} Equivalence Ratio and/or Current"
    override val mode = "01"
    override val pid = sensor.pid

    override val handler = { it: ObdRawResponse ->
        val cleanedResponse = it.bufferedValue.joinToString("") { byte -> "%02X".format(byte) }
        if (cleanedResponse.length >= 8) {
            val equivRatio = cleanedResponse.substring(4, 8).toInt(16) / 32768.0
            "Oxygen Sensor ${sensor.name} Equivalence Ratio: $equivRatio"
        } else {
            "Invalid response: $cleanedResponse"
        }
    }
}