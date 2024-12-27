package cc.webdevel.obdlogger.command.bmw

import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse

class OxygenSensorCommand(private val sensor: OxygenSensor) : ObdCommand() {

    enum class OxygenSensor(val pid: String) {
        BANK_1_SENSOR_2("14"),
        BANK_1_SENSOR_3("15")
    }

    override val tag = "OXYGEN_SENSOR_COMMAND"
    override val name = "Oxygen Sensor ${sensor.name} & Short Term Fuel Trim"
    override val mode = "01"
    override val pid = sensor.pid

    override val handler = { it: ObdRawResponse ->
        val cleanedResponse = it.bufferedValue.joinToString("") { byte -> "%02X".format(byte) }
        if (cleanedResponse.length >= 8) {
            val sensorValue = cleanedResponse.substring(4, 6).toInt(16) / 200.0
            val fuelTrim = cleanedResponse.substring(6, 8).toInt(16) / 1.28 - 100
            "Oxygen Sensor ${sensor.name}: $sensorValue V, Short Term Fuel Trim: $fuelTrim%"
        } else {
            "Invalid response: $cleanedResponse"
        }
    }
}