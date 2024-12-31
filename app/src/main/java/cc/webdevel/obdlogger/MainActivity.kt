package cc.webdevel.obdlogger

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
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
import cc.webdevel.obdlogger.bluetooth.*
import cc.webdevel.obdlogger.mock.MockBluetoothDevice
import android.content.SharedPreferences

class MainActivity : ComponentActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var connectThread: ConnectThread? = null
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>
    private var isConnected: MutableState<Boolean> = mutableStateOf(false)
    private var errorMessage: String by mutableStateOf("") // Define errorMessage here
    private var obdData: String by mutableStateOf("") // Define obdData here
    private var pairedDevicesMessage: String by mutableStateOf("") // Define pairedDevicesMessage here
    private var showFetchDataButton: Boolean by mutableStateOf(false) // Define showFetchDataButton here
    private lateinit var sharedPreferences: SharedPreferences
    private var isLoading: MutableState<Boolean> = mutableStateOf(false)

    // onCreate method
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedUploadUrl = sharedPreferences.getString("upload_url", getString(R.string.upload_url))

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
                    onPairedDevicesUpdate = { },
                    uploadUrl = "http://localhost:3000",
                    onDataUpdate = { },
                    isToggleOn = false,
                    onFetchDataReady = { }
                )
            }
        }

        setContent {
            MainContent(savedUploadUrl, isLoading)
        }
    }

    @Composable
    // MainContent composable function
    fun MainContent(savedUploadUrl: String?, isLoading: MutableState<Boolean>) {
        ObdLoggerTheme {
            val statusBarHeight = rememberStatusBarHeight()
            var statusMessage by remember { mutableStateOf("Ready to connect...") }
            var errorMessage by remember { mutableStateOf("") }
            var obdData by remember { mutableStateOf("") }
            var pairedDevicesMessage by remember { mutableStateOf("") }
            var isConnected by remember { mutableStateOf(false) }
            var uploadUrl by remember { mutableStateOf(savedUploadUrl ?: getString(R.string.upload_url)) }
            var isToggleOn by remember { mutableStateOf(false) }
            var showFetchDataButton by remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {

                val onConnectClick = {

                    statusMessage = ""
                    errorMessage = ""
                    pairedDevicesMessage = ""
                    obdData = ""

                    if (isConnected) {
                        disconnectDevice { message -> statusMessage = message }
                        isConnected = false
                    } else {
                        if (bluetoothAdapter == null) {
                            statusMessage = "Device doesn't support Bluetooth"
                        } else {
                            if (!bluetoothAdapter!!.isEnabled) {
                                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                enableBluetoothLauncher.launch(enableBtIntent)
                            } else {
                                val connectedToDevice = connectToDevice(
                                    onStatusUpdate = { message ->
                                        statusMessage = message
                                        errorMessage = ""
                                 },
                                    onError = { error -> errorMessage = error },
                                    onPairedDevicesUpdate = { pairedDevices -> pairedDevicesMessage = pairedDevices },
                                    uploadUrl = uploadUrl,
                                    isToggleOn = isToggleOn,
                                    onDataUpdate = { data -> obdData = data },
                                    onFetchDataReady = {
                                        showFetchDataButton = true
                                        statusMessage = "Connected to ECU"
                                    }
                                )

                                if (connectedToDevice) {
                                    isConnected = true
                                }
                            }
                        }
                    }
                }

                // Call the MainScreen composable function
                MainScreen(
                    statusBarHeight = statusBarHeight,
                    statusMessage = statusMessage,
                    errorMessage = errorMessage,
                    pairedDevicesMessage = pairedDevicesMessage,
                    onConnectClick = onConnectClick,
                    isConnected = isConnected,
                    onUploadUrlChange = { url ->
                        uploadUrl = url
                        saveUploadUrl(url)
                        connectThread?.updateUploadUrl(url)
                    },
                    onToggleChange = { isOn ->
                        isToggleOn = isOn
                        connectThread?.updateToggleState(isOn)
                    },
                    uploadUrlString = uploadUrl,
                    obdData = obdData,
                    onCustomCommand = { command ->
                        isLoading.value = true
                        connectThread?.sendCustomCommand(command, isLoading)
                    },
                    onFetchDataClick = {
                        connectThread?.fetchData()
                    },
                    showFetchDataButton = showFetchDataButton,
                    isLoading = isLoading
                )
            }
        }
    }

    private fun saveUploadUrl(url: String) {
        with(sharedPreferences.edit()) {
            putString("upload_url", url)
            apply()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(
        onStatusUpdate: (String) -> Unit,
        onError: (String) -> Unit,
        onPairedDevicesUpdate: (String) -> Unit,
        uploadUrl: String,
        isToggleOn: Boolean = false,
        onDataUpdate: (String) -> Unit,
        onFetchDataReady: () -> Unit
    ): Boolean {
        try {

            if (resources.getBoolean(R.bool.use_mock_device)) {
                return connectToThread(MockBluetoothDevice(), bluetoothAdapter!!, onStatusUpdate, onError, uploadUrl, isToggleOn, onDataUpdate, onFetchDataReady)
            }

            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

            if (pairedDevices.isNullOrEmpty()) {
                onError("No paired Bluetooth devices found")
                return false
            }

            val pairedDevicesMessage = pairedDevices.joinToString(separator = "\n") { device ->
                "Paired device: ${device.name}, ${device.address}"
            }
            onPairedDevicesUpdate(pairedDevicesMessage)

            val device = pairedDevices.firstOrNull { it.name == "V-LINK" }

            device?.let {
                return connectToThread(
                    RealBluetoothDevice(it),
                    bluetoothAdapter!!,
                    onStatusUpdate,
                    onError,
                    uploadUrl,
                    isToggleOn,
                    onDataUpdate,
                    onFetchDataReady)
            } ?: run {
                onError("No paired device with name V-LINK found")
            }

        } catch (e: Exception) {
            onError("Error while connecting to device: ${e.message}")
        }

        return false
    }

    // Connect to the device
    private fun connectToThread(
        device: BluetoothDeviceInterface,
        bluetoothAdapter: BluetoothAdapter,
        onStatusUpdate: (String) -> Unit,
        onError: (String) -> Unit,
        uploadUrl: String,
        isToggleOn: Boolean,
        onDataUpdate: (String) -> Unit,
        onFetchDataReady: () -> Unit
    ): Boolean {

        try {
            if (connectThread != null) {
                connectThread?.disconnect()
                connectThread = null
                return false
            }
        } catch (e: Exception) {
            onError("Error while disconnecting from device: ${e.message}")
            return false
        }

        connectThread = ConnectThread(
            device,
            bluetoothAdapter,
            onStatusUpdate,
            onError,
            uploadUrl,
            isToggleOn,
            this@MainActivity,
            onDataUpdate,
            onFetchDataReady,
            isConnected
        )
        connectThread?.start()
        return true
    }

    private fun disconnectDevice(onStatusUpdate: (String) -> Unit) {
        connectThread?.disconnect() // Call the new disconnect method
        connectThread = null
        isConnected.value = false
        obdData = ""
        errorMessage = ""
        pairedDevicesMessage = ""
        showFetchDataButton = false
        onStatusUpdate("Disconnected. Ready to connect...")
    }
}