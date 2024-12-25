package cc.webdevel.obdlogger

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import cc.webdevel.obdlogger.ui.theme.ObdLoggerTheme
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var connectThread: ConnectThread? = null
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Bluetooth has been enabled, proceed with connection
                connectToDevice(
                    onStatusUpdate = { },
                    onError = { },
                    onPairedDevicesUpdate = { }
                )
            }
        }

        setContent {
            ObdLoggerTheme {
                val statusBarHeight = rememberStatusBarHeight()

                var statusMessage by remember { mutableStateOf("Ready to connect...") }
                var errorMessage by remember { mutableStateOf("") }
                var pairedDevicesMessage by remember { mutableStateOf("") }
                var isConnected by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val onConnectClick = {
                        if (connectThread != null) {
                            connectThread?.cancel()
                            connectThread = null
                            statusMessage = "Disconnected. Ready to connect..."
                            isConnected = false
                        } else {
                            if (bluetoothAdapter == null) {
                                statusMessage = "Device doesn't support Bluetooth"
                            } else {
                                if (!bluetoothAdapter!!.isEnabled) {
                                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                    enableBluetoothLauncher.launch(enableBtIntent)
                                } else {
                                    connectToDevice(
                                        onStatusUpdate = { message -> statusMessage = message },
                                        onError = { error -> errorMessage = error },
                                        onPairedDevicesUpdate = { pairedDevices -> pairedDevicesMessage = pairedDevices }
                                    )
                                    isConnected = true
                                }
                            }
                        }
                    }

                    MainScreen(
                        statusBarHeight = statusBarHeight,
                        statusMessage = statusMessage,
                        errorMessage = errorMessage,
                        pairedDevicesMessage = pairedDevicesMessage,
                        onConnectClick = onConnectClick,
                        isConnected = isConnected
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(
        onStatusUpdate: (String) -> Unit,
        onError: (String) -> Unit,
        onPairedDevicesUpdate: (String) -> Unit
    ) {
        try {
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            val mockDevice = MockBluetoothDevice()

            if (pairedDevices.isNullOrEmpty()) {
                onStatusUpdate("Connecting to 'Mockup V-LINK'...")
                connectThread = ConnectThread(mockDevice, bluetoothAdapter!!, onStatusUpdate, onError)
                connectThread?.start()
                return
            }

            if (pairedDevices.isEmpty()) {
                onError("No paired Bluetooth devices found")
                return
            }

            val pairedDevicesMessage = pairedDevices.joinToString(separator = "\n") { device ->
                "Paired device: ${device.name}, ${device.address}"
            }
            onPairedDevicesUpdate(pairedDevicesMessage)

            val device = pairedDevices.firstOrNull { it.name == "V-LINK" }
            if (device != null) {
                onStatusUpdate("Connecting to 'V-LINK'...")

                connectThread = ConnectThread(RealBluetoothDevice(device), bluetoothAdapter!!, onStatusUpdate, onError)
//                connectThread = ConnectThread(mockDevice, bluetoothAdapter!!, onStatusUpdate, onError)
                // uncommnet the line above to use the mock device
                connectThread?.start()
            } else {
                onError("Device 'V-LINK' not found")
            }
        } catch (e: Exception) {
            onError("Error while connecting to device: ${e.message}")
        }
    }
}