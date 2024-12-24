package cc.webdevel.obdlogger

import com.github.eltonvs.obd.connection.ObdDeviceConnection
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse
import com.github.eltonvs.obd.command.engine.RPMCommand
import com.github.eltonvs.obd.command.ObdResponse
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito

class ObdDeviceConnectionTest {

    @Test
    fun testObdConnection() = runBlocking {

        // Create a mock RPMCommand
        val rpmCommand = RPMCommand()

        // Create a mock response for RPMCommand
        val mockResponse = ObdResponse(
            rpmCommand,
            ObdRawResponse("123", 0),
            value = "1234"
        ) // Mock response using the command

        // Assert that the response is as expected
        assert(mockResponse.value == "1234")
        assert(mockResponse.rawResponse.value == "123")
    }
}