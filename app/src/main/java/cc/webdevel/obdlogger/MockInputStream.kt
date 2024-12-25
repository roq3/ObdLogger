package cc.webdevel.obdlogger

import java.io.InputStream

class MockInputStream : InputStream() {
    private val commandResponses = mapOf(
        "01 0D" to "41 0D 001E>", // Response for SpeedCommand (30 Km/h)
        "01 0C" to "41 0C 1AF8>", // Response for RPMCommand (3000 RPM)
        "01 10" to "41 10 1234>", // Response for MassAirFlowCommand
        "01 0F" to "41 1F 5678>", // Response for RuntimeCommand
        "01 04" to "41 04 9ABC>", // Response for LoadCommand
        "01 43" to "41 43 DEF0>", // Response for AbsoluteLoadCommand
        "01 11" to "41 11 1357>", // Response for ThrottlePositionCommand
        "01 45" to "41 45 2468>"  // Response for RelativeThrottlePositionCommand
    )

    private var currentResponse: String? = null
    private var index = 0

    fun setCommand(command: String) {
        currentResponse = commandResponses[command]
        index = 0
    }

    override fun available(): Int {
        return currentResponse?.let {
            it.length - index
        } ?: 0
    }

    override fun read(): Int {
        return currentResponse?.let {
            if (index < it.length) {
                it[index++].code
            } else {
                -1 // End of stream
            }
        } ?: -1
    }
}