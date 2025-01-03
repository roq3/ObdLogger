package cc.webdevel.obdlogger

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.icu.text.SimpleDateFormat
import cc.webdevel.obdlogger.bluetooth.BluetoothDeviceInterface
import cc.webdevel.obdlogger.bluetooth.BluetoothSocketInterface
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import com.github.eltonvs.obd.command.engine.*
import com.github.eltonvs.obd.command.at.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.Locale
import android.location.Location
import android.content.Context
import android.content.pm.PackageManager
import cc.webdevel.obdlogger.command.*
import cc.webdevel.obdlogger.mock.BMWCodes
import com.github.eltonvs.obd.command.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import android.util.Log
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.Job
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

// Thread to connect to the OBD device
class ConnectThread(
    private val device: BluetoothDeviceInterface,
    private val bluetoothAdapter: BluetoothAdapter,
    private val onStatusUpdate: (String) -> Unit,
    private val onError: (String) -> Unit,
    private var uploadUrl: String,
    private var isToggleOn: Boolean,
    private val context: Context,
    private val onDataUpdate: (String) -> Unit,
    private val onFetchDataReady: () -> Unit,
    private var isConnected: MutableState<Boolean>,
) : Thread() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient // Location client
    private var isRunning = true // Flag to keep the thread running
    private lateinit var obdConnection: ObdDeviceConnection // OBD connection
    private val mutex = Mutex() // Mutex to prevent concurrent command execution
    private var mmSocket: BluetoothSocketInterface? = null // Bluetooth socket
    private var initialConfigResults = StringBuilder() // To store initial config command results
    private var fetchDataJob: Job? = null

    val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    // UUID for OBD-II devices
    companion object {
        private val OBD_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    // Connection state sealed class
    sealed class ConnectionState {
        class Connecting(val bluetoothDevice: BluetoothDeviceInterface) : ConnectionState()
        class Connected(val socket: BluetoothSocketInterface): ConnectionState()
        class ConnectionFailed(val failureReason: String): ConnectionState()
        data object Disconnected: ConnectionState()
    }

    fun updateToggleState(isOn: Boolean) {
        isToggleOn = isOn
    }

    fun updateUploadUrl(newUrl: String) {
        uploadUrl = newUrl
    }

    // Update the disconnect method to set the flag
    fun disconnect() {
        cancel()
    }

    // Cancel the thread
    private fun cancel() {
        isRunning = false
        try {
            mmSocket?.close()
        } catch (e: Exception) {
            onError("Could not close the client socket: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    // Connect to the device
    fun connectToDevice(bluetoothDevice: BluetoothDeviceInterface) = flow {
        emit(ConnectionState.Connecting(bluetoothDevice))
        bluetoothAdapter.cancelDiscovery()
        try {
            val socket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(OBD_UUID).also {
                it.connect()
                mmSocket = it
            }
            emit(ConnectionState.Connected(socket))
        } catch (e: Exception) {
            emit(ConnectionState.ConnectionFailed(e.message ?: "Failed to connect"))
        }
    }.flowOn(Dispatchers.IO)

    // Connect to the device and start the thread
    @SuppressLint("MissingPermission")
    override fun run() {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

                connectToDevice(device).collect { state ->
                    _connectionState.value = state
                    when (state) {
                        is ConnectionState.Connecting -> {
                            onStatusUpdate("Connecting to '${state.bluetoothDevice.getName()}'")
                            delay(1000)
                        }
                        is ConnectionState.Connected -> {
                            onStatusUpdate("Connected to '${device.getName()}'")
                            isConnected.value = true

                            startObdCommandFlow(state.socket).collect { data ->

                                var onDataUpdateStatus = initialConfigResults.toString()
                                if(data.isNotEmpty()) {
                                    onDataUpdateStatus+= "\n" + data
                                }

                                onDataUpdate(onDataUpdateStatus) // Display initial config results above obdData
                            }
                        }
                        is ConnectionState.ConnectionFailed -> {
                            onError("Error: ${state.failureReason}")
                        }
                        is ConnectionState.Disconnected -> {
                            onStatusUpdate("Disconnected from '${device.getName()}'")
                            onError("")
                            initialConfigResults.clear()
                            onDataUpdate("")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ConnectThread", "Unexpected error in run method", e)
            onError("Unexpected error: $e")
        }
    }

    // Restart the thread
    // Will be used in future versions to handle reconnection
    private suspend fun restart() {

        delay(1000)
        onError("")
        onStatusUpdate("Reconnecting to '${device.getName()}'")
        delay(1000)

        isRunning = false // Stop the current thread
        cancel() // Cancel the current thread
        ConnectThread(
            device,
            bluetoothAdapter,
            onStatusUpdate,
            onError,
            uploadUrl,
            isToggleOn,
            context,
            onDataUpdate,
            onFetchDataReady,
            isConnected
        ).start() // Start a new thread instance
    }

    // Start the OBD command flow
    private fun startObdCommandFlow(socket: BluetoothSocketInterface) = flow {
        if (!isRunning) return@flow
        try {
            obdConnection = ObdDeviceConnection(socket.getInputStream(), socket.getOutputStream())

            try {
                if (!::obdConnection.isInitialized) {
                    onError("OBD connection is not initialized")
                    return@flow
                }
            } catch (e: Exception) {
                onError("Error initializing OBD connection: ${e.message}")
                return@flow
            }

            initialConfigCommands.forEachIndexed { index, it ->
                if (!isRunning) return@flow

                onStatusUpdate("Running command: ${it.name}")

                try {

                    val result = runCommandSafely {
                        obdConnection.run(
                            it,
                            true,
                            1,
                            0
                        )
                    }

                    val commandName = result.command.name
                    val commandSend = result.command.rawCommand
                    val commandFormattedValue = result.formattedValue
                    val resultRawValue = result.rawResponse.value

                    var resultString =
                        "$commandName ($commandSend): ($resultRawValue) $commandFormattedValue"

                    // if not first command, add a new line
                    if (index > 0) {
                        resultString = "\n$resultString"
                    }

                    initialConfigResults.append(resultString) // Append result to the StringBuilder
                    emit("\nLast Response: $resultString") // Emit the result of each initial command

                    if (it is AvailablePIDsCustomCommand) {
                        val availableCommands = commandFormattedValue.split(",")
                            .filter {
                            it.isNotEmpty()
                        }

                        if (availableCommands.size > 1) {
                            delay(500)
                            onFetchDataReady() // Notify that the initial commands are done
                        }
                    }

                    delay(500)

                } catch (e: Exception) {
                    onError("Error running command: ${it.name} - ${e.toString()}")
                    return@flow
                }
            }
        } catch (e: Exception) {
            Log.e("ConnectThread", "Unexpected error in startObdCommandFlow", e)
            onError("Unexpected error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO) // all operations happen on IO thread

    fun fetchData() {
        fetchDataJob?.cancel() // Cancel the previous job if it is still active

        fetchDataJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isRunning) {
                    val startTime = System.currentTimeMillis()

                    onStatusUpdate("Fetching data...")
                    val commandResults = mutableMapOf<String, MutableMap<String, String>>()
                    val obdDataMessage = StringBuilder()

                    commandList.forEach { (groupKey, groupCommands) ->

                        val groupKeyName: String = if(groupKey == "GPS") {
                            "Location:\n"
                        } else {
                            "\n\n$groupKey:\n"
                        }
                        obdDataMessage.append(groupKeyName)

                        val deferredResults = groupCommands.map { (key, value) ->
                            CoroutineScope(Dispatchers.IO).async {
                                try {
                                    val commandVal = withTimeoutOrNull(2000) { // 2 seconds timeout
                                        when (key) {
                                            "Date", "Latitude", "Longitude" -> value().toString()
                                            else -> {
                                                when (val commandResult = value()) {
                                                    is ObdCommand -> runCommandSafely {
                                                        obdConnection.run(
                                                            commandResult,
                                                            true,
                                                            1,
                                                            0
                                                        ).formattedValue
                                                    }
                                                    else -> commandResult.toString()
                                                }
                                            }
                                        }
                                    } ?: "Timeout"
                                    key to commandVal
                                } catch (e: Exception) {
                                    key to "Error: $e"
                                }
                            }
                        }

                        val results = deferredResults.map { it.await() }
                        results.forEachIndexed { index, (key, commandVal) ->

                            var lineKey = key

                            if (
                                lineKey in listOf(
                                    "Short term - Bank1",
                                    "Engine RPM",
                                    "Intake air temperature",
                                    "Oxygen sensors present",
                                    "OBD standard"
                                )
                            ) {
                                lineKey = "\n$lineKey"
                            }

                            var line = "$lineKey: $commandVal"

                            // if not last command, add a comma
                            if(index < results.size - 1) {
                                line = "$lineKey: $commandVal,\n"
                            }

                            obdDataMessage.append(line)

                            commandResults[groupKey] = commandResults[groupKey] ?: mutableMapOf()
                            commandResults[groupKey]?.set(key!!, commandVal)
                        }
                    }

                    if (isToggleOn) {
                        sendResultsToServer(uploadUrl, commandResults)
                    }
                    onDataUpdate(obdDataMessage.toString())

                    val elapsedTime = System.currentTimeMillis() - startTime
                    val delayTime = 1000 - elapsedTime
                    if (delayTime > 0) {
                        delay(delayTime)
                    }
                }
            } catch (e: IOException) {
                onError("Connection lost: ${e.message}")
                isRunning = false
            } catch (e: CancellationException) {
                onStatusUpdate("${e.message}")
            } catch (e: Exception) {
                onError("Error: ${e.message}")
            }
        }
    }

    // List of initial configuration commands
    private val initialConfigCommands
        get() = listOf(

            // default commands
            SetDefaultsCommand(), // AT D
            SetEchoCommand(Switcher.OFF), // AT E0
            SetHeadersCommand(Switcher.OFF), // AT H0
            SelectProtocolCommand(ObdProtocols.ISO_14230_4_KWP_FAST), // AT SP 5

            // PIDs commands
            AvailablePIDsCustomCommand(AvailablePIDsCustomCommand.AvailablePIDsRanges.PIDS_01_TO_20), // 01 00
            AvailablePIDsCustomCommand(AvailablePIDsCustomCommand.AvailablePIDsRanges.PIDS_21_TO_40), // 01 20
        )

    // List of commands to be executed
    private val commandList
        get() = mapOf(
            "GPS" to mapOf(
                "Latitude" to { getLocationSync()?.latitude ?: 0.0 },
                "Longitude" to { getLocationSync()?.longitude ?: 0.0 }
            ),
            "Main" to mapOf(
                "Date" to { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()) },
            ),
            "BMW" to BMWCodes().getCodes()
        )

    // Run a command safely using a mutex to prevent concurrent command execution
    private suspend fun <T> runCommandSafely(command: suspend () -> T): T {
        return mutex.withLock {
            command()
        }
    }

    // Send a custom command
    fun sendCustomCommand(command: String, isLoading: MutableState<Boolean>) {
        CoroutineScope(Dispatchers.IO).launch {
            isLoading.value = true // Set loading state

            try {
                if (!::obdConnection.isInitialized) {
                    onError("OBD connection is not initialized")
                    isLoading.value = false
                    return@launch
                }

                if (command.isEmpty()) {
                    onError("Custom command is empty")
                    isLoading.value = false
                    return@launch
                }

                onDataUpdate("Running custom command: $command")

                val commandForCustom: ObdCommand = when (command) {
                    "01 0C" -> RPMCommand()
                    "01 0D" -> SpeedCommand()
                    "01 00" -> AvailablePIDsCustomCommand(AvailablePIDsCustomCommand.AvailablePIDsRanges.PIDS_01_TO_20)
                    "01 20" -> AvailablePIDsCustomCommand(AvailablePIDsCustomCommand.AvailablePIDsRanges.PIDS_21_TO_40)
                    "01 40" -> AvailablePIDsCustomCommand(AvailablePIDsCustomCommand.AvailablePIDsRanges.PIDS_41_TO_60)
                    "01 60" -> AvailablePIDsCustomCommand(AvailablePIDsCustomCommand.AvailablePIDsRanges.PIDS_61_TO_80)
                    "01 80" -> AvailablePIDsCustomCommand(AvailablePIDsCustomCommand.AvailablePIDsRanges.PIDS_81_TO_A0)
                    else -> CustomObdCommand(command)
                }

                val result = runCommandSafely {
                    obdConnection.run(commandForCustom)
                }

                val commandName = result.command.name
                val commandSend = result.command.rawCommand
                val commandFormattedValue = result.formattedValue
                val resultRawValue = result.rawResponse.value

                val resultString =
                    "$commandName ($commandSend): ($resultRawValue) $commandFormattedValue"

                onDataUpdate("Custom Command Result: $resultString") // Display initial config results above obdData
                onStatusUpdate("Custom Command Result: $resultString")
            } catch (e: Exception) {
                onError("Error executing custom command: $e")
                isLoading.value = false
            } finally {
                isLoading.value = false // Reset loading state
            }
        }
    }

    private fun getLocation(onLocationReceived: (Location?) -> Unit) {
        try {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                val cancellationTokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    onLocationReceived(location)
                }.addOnFailureListener { e ->
                    onError("Error getting location: ${e.message}")
                    onLocationReceived(null)
                }
            } else {
                onError("Location permission not granted")
                onLocationReceived(null)
            }
        } catch (e: SecurityException) {
            onError("Location permission not granted: ${e.message}")
            onLocationReceived(null)
        } catch (e: Exception) {
            onError("Error getting location: ${e.message}")
            onLocationReceived(null)
        }
    }

    private fun getLocationSync(): Location? {
        var location: Location? = null
        val latch = CountDownLatch(1)
        getLocation { loc ->
            location = loc
            latch.countDown()
        }
        latch.await(5, TimeUnit.SECONDS) // Wait for up to 5 seconds for the location
        return location
    }

    private fun sendResultsToServer(url: String, data: MutableMap<String, MutableMap<String, String>>) {
        val maxRetries = 1
        var attempt = 0
        var success = false

        while (attempt < maxRetries && !success) {
            try {
                val json = JSONObject()
                data.forEach { (groupKey, groupData) ->
                    val groupJson = JSONObject()
                    groupData.forEach { (key, value) ->
                        groupJson.put(key, value)
                    }
                    json.put(groupKey, groupJson)
                }

                val urlConnection = URL(url).openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("Content-Type", "application/json")
                urlConnection.setRequestProperty("Accept", "application/json")
                urlConnection.doOutput = true

                urlConnection.outputStream.use { outputStream ->
                    outputStream.write(json.toString().toByteArray())
                }

                val responseCode = urlConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    onStatusUpdate("Data sent successfully: ${json.toString().take(20)}...")
                    onError("")
                    success = true
                } else {
                    onError("Failed to send data: HTTP $responseCode")
                }
            } catch (e: Exception) {
                onError("Error sending data: ${e.message}")
            }

            attempt++
            if (!success) {
                sleep(2000) // Wait for 2 seconds before retrying
            }
        }

        if (!success) {
            onError("Failed to send data after $maxRetries attempts")
        }
    }
}