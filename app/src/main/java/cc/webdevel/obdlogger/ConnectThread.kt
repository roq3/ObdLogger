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
import com.github.eltonvs.obd.command.control.*
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

// Thread to connect to the OBD device
class ConnectThread(
    private val device: BluetoothDeviceInterface,
    private val bluetoothAdapter: BluetoothAdapter,
    private val onStatusUpdate: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val uploadUrl: String,
    private val isToggleOn: Boolean,
    private val context: Context,
    private val onDataUpdate: (String) -> Unit,
    private val onFetchDataReady: () -> Unit,
) : Thread() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient // Location client
    private var isRunning = true // Flag to keep the thread running
    private lateinit var obdConnection: ObdDeviceConnection // OBD connection
    private val mutex = Mutex() // Mutex to prevent concurrent command execution
    private var mmSocket: BluetoothSocketInterface? = null // Bluetooth socket
    private var initialConfigResults = StringBuilder() // To store initial config command results

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
            var connectedDevice = bluetoothDevice
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
                    when (state) {
                        is ConnectionState.Connecting -> {
                            onStatusUpdate("Connecting to '${device.getName()}'")
                        }
                        is ConnectionState.Connected -> {
                            onStatusUpdate("Connected to '${device.getName()}'")

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
                            delay(1000)
                            onError("")
                            onStatusUpdate("Reconnecting to '${device.getName()}'")
                            delay(1000)
                            this@ConnectThread.restart() // Restart the thread
                        }
                        is ConnectionState.Disconnected -> {
                            onError("Disconnected from '${device.getName()}'")
                            onError("")
                            initialConfigResults.clear()
                            onDataUpdate("")
                        }
                        else -> {}
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ConnectThread", "Unexpected error in run method", e)
            onError("Unexpected error: ${e.toString()}")
        }
    }

    private fun restart() {
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
            onFetchDataReady
        ).start() // Start a new thread instance
    }

    // Start the OBD command flow
    private fun startObdCommandFlow(socket: BluetoothSocketInterface) = flow {
        if (!isRunning) return@flow
        try {
            obdConnection = ObdDeviceConnection(socket.getInputStream(), socket.getOutputStream())

            initialConfigCommands.forEachIndexed { index, it ->
                if (!isRunning) return@flow

                val result = runCommandSafely { obdConnection.run(it) }

                val commandName = result.command.name
                val commandSend = result.command.rawCommand
                val commandFormattedValue = result.formattedValue
                val resultRawValue = result.rawResponse.value

                var resultString = "$commandName ($commandSend): ($resultRawValue) $commandFormattedValue"

                // if not first command, add a new line
                if (index > 0) {
                    resultString = "\n$resultString"
                }

                initialConfigResults.append(resultString) // Append result to the StringBuilder
                emit("") // Emit the result of each initial command

                if (it is ResetAdapterCommand) {
                    delay(500)
                }

                if (it is AvailablePIDsCommand) {
                    val availableCommands = commandFormattedValue.split(",")
                        .filter {
                        it.isNotEmpty()
                    }

                    if (availableCommands.size > 1) {
                        delay(500)
                        onFetchDataReady() // Notify that the initial commands are done
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ConnectThread", "Unexpected error in startObdCommandFlow", e)
            onError("Unexpected error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO) // all operations happen on IO thread

    fun fetchData() {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isRunning) {
                    val commandResults = mutableMapOf<String, MutableMap<String, String>>()
                    val obdDataMessage = StringBuilder()

                    commandList.forEach { (groupKey, groupCommands) ->

                        var groupKeyName = groupKey
                        if(groupKey == "GPS") {
                            groupKeyName = "Location:\n"
                        } else {
                            groupKeyName = "\n\n$groupKey:\n"
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
                                                    is ObdCommand -> runCommandSafely { obdConnection.run(commandResult).formattedValue }
                                                    else -> commandResult.toString()
                                                }
                                            }
                                        }
                                    } ?: "Timeout"
                                    key to commandVal
                                } catch (e: Exception) {
                                    key to "Error: ${e.toString()}"
                                }
                            }
                        }

                        val results = deferredResults.map { it.await() }
                        results.forEachIndexed() { index, (key, commandVal) ->

                            // if not last command, add a comma
                            if(index < results.size - 1) {
                                obdDataMessage.append("$key: $commandVal,\n")
                            } else {
                                obdDataMessage.append("$key: $commandVal")
                            }

                            commandResults[groupKey] = commandResults[groupKey] ?: mutableMapOf()
                            commandResults[groupKey]?.set(key, commandVal)
                        }
                    }

                    if (isToggleOn) {
                        sendResultsToServer(uploadUrl, commandResults)
                    }
                    onDataUpdate(obdDataMessage.toString())
                }
            } catch (e: IOException) {
                onError("Connection lost: ${e.message}")
                isRunning = false
            } catch (e: Exception) {
                onError("Error: ${e.message}")
            }
        }
    }

    // List of initial configuration commands
    private val initialConfigCommands
        get() = listOf(
//            ResetAdapterCommand(),
            SetEchoCommand(Switcher.OFF),
            SetLineFeedCommand(Switcher.OFF),
            IdentifyCommand(),
            SetTimeoutCommand(2000),
            DisableAutoFormattingCommand(),
            SelectProtocolCommand(ObdProtocols.ISO_14230_4_KWP_FAST),
            IsoBaudCommand(10),
            SetHeadersCommand(Switcher.ON),
            SetHeaderCommand("7E0"),
            ReadVoltageCommand(),
            AvailablePIDsCommand(AvailablePIDsCommand.AvailablePIDsRanges.PIDS_01_TO_20)
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
    fun sendCustomCommand(command: String) {

        CoroutineScope(Dispatchers.IO).launch {

            mmSocket?.let {
                val socket = it
                obdConnection = ObdDeviceConnection(socket.getInputStream(), socket.getOutputStream())
            }

            try {
                if (!::obdConnection.isInitialized) {
                    onError("OBD connection is not initialized")
                    return@launch
                }

                val commandForCustom: ObdCommand = when (command) {
                    "01 0C" -> RPMCommand()
                    "01 0D" -> SpeedCommand()
                    else -> CustomObdCommand(command)
                }
                val result = runCommandSafely { obdConnection.run(commandForCustom).formattedValue }
                onStatusUpdate("Custom Command Result: $result")
            } catch (e: Exception) {
                onError("Error executing custom command: ${e.toString()}")
            }
        }
    }

    // Cancel the thread
    fun cancel() {
        isRunning = false
        try {
            mmSocket?.close()
        } catch (e: Exception) {
            onError("Could not close the client socket: ${e.message}")
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
        val maxRetries = 3
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
                    onStatusUpdate("Data sent successfully: ${json.toString().take(100)}...")
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