package cc.webdevel.obdlogger.mock

import java.io.OutputStream

class MockOutputStream(private val inputStream: MockInputStream) : OutputStream() {
    private val commandBuffer = StringBuilder()

    override fun write(b: Int) {
        if (b == '\r'.code || b == '\n'.code) {
            inputStream.setCommand(commandBuffer.toString().trim())
            commandBuffer.clear()
        } else {
            commandBuffer.append(b.toChar())
        }
    }
}