package cc.webdevel.obdlogger

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import cc.webdevel.obdlogger.ui.theme.ObdLoggerTheme
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val TAG = "BluetoothApp"

        public val OBD_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Włącz tryb edge-to-edge i obsługuj notch
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Pobierz BluetoothAdapter za pomocą BluetoothManager
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        setContent {
            ObdLoggerTheme {
                val view = LocalView.current
                val statusBarHeight = rememberStatusBarHeight()

                var statusMessage by remember { mutableStateOf("Ready to connect...") }
                var errorMessage by remember { mutableStateOf("") }
                var pairedDevicesMessage by remember { mutableStateOf("") }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val onConnectClick = {

                        if (bluetoothAdapter == null) {
                            statusMessage = "Device doesn't support Bluetooth"
                        } else {
                            if (!bluetoothAdapter!!.isEnabled) {
                                val enableBtIntent =
                                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                            } else {
                                connectToDevice(
                                    onStatusUpdate = { message ->
                                        statusMessage = message
                                    },
                                    onError = { error ->
                                        errorMessage = error
                                    },
                                    onPairedDevicesUpdate = { pairedDevices ->
                                        pairedDevicesMessage = pairedDevices
                                    }
                                )
                            }
                        }
                    }

                    MainScreen(
                        statusBarHeight = statusBarHeight,
                        statusMessage = statusMessage,
                        errorMessage = errorMessage,
                        pairedDevicesMessage = pairedDevicesMessage,
                        onConnectClick = onConnectClick
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth został włączony, kontynuuj połączenie
                connectToDevice(
                    onStatusUpdate = { message ->
                        // Zaktualizuj UI lub obsłuż komunikat statusu
                    },
                    onError = { error ->
                        // Obsłuż komunikat o błędzie
                    },
                    onPairedDevicesUpdate = { pairedDevices ->
                        // Zaktualizuj UI lub obsłuż sparowane urządzenia
                    }
                )
            } else {
                // Bluetooth nie został włączony
                // Zaktualizuj UI, aby wyświetlić komunikat o błędzie
            }
        }
    }

    private fun connectToDevice(
        onStatusUpdate: (String) -> Unit,
        onError: (String) -> Unit,
        onPairedDevicesUpdate: (String) -> Unit
    ) {
        try {
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            val mockDevice = MockBluetoothDevice()

            if (pairedDevices == null || pairedDevices.isEmpty()) {
                onStatusUpdate("Connecting to 'V-LINK'...")
                ConnectThread(mockDevice, bluetoothAdapter!!, onStatusUpdate, onError).start()
                return
            }

            if (pairedDevices.isNullOrEmpty()) {
                onError("No paired Bluetooth devices found")
                return
            }

            val pairedDevicesMessage = pairedDevices.joinToString(separator = "\n") { device ->
                "Paired device: ${device.name}, ${device.address}"
            }
            onPairedDevicesUpdate(pairedDevicesMessage)

            // Znajdź urządzenie o nazwie "V-LINK"
            val device = pairedDevices.firstOrNull { it.name == "V-LINK" }
            if (device != null) {
                onStatusUpdate("Connecting to 'V-LINK'...")
                ConnectThread(RealBluetoothDevice(device), bluetoothAdapter!!, onStatusUpdate, onError).start()
            } else {
                onError("Device 'V-LINK' not found")
            }
        } catch (e: Exception) {
            onError("Error while connecting to device: ${e.message}")
        }
    }
}