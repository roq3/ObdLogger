package cc.webdevel.obdlogger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    statusBarHeight: Dp,
    statusMessage: String,
    errorMessage: String,
    obdData: String,
    pairedDevicesMessage: String,
    onConnectClick: () -> Unit,
    isConnected: Boolean,
    onUploadUrlChange: (String) -> Unit,
    onToggleChange: (Boolean) -> Unit,
    uploadUrlString: String,
    onCustomCommand: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var uploadUrl by remember { mutableStateOf(uploadUrlString) }
    var isToggleOn by remember { mutableStateOf(false) }
    var customCommand by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(
                top = statusBarHeight,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            )
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Button(onClick = onConnectClick) {
                Text(text = if (isConnected) "Disconnect" else "Connect")
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(text = "Enable Upload")
            Switch(
                checked = isToggleOn,
                onCheckedChange = {
                    isToggleOn = it
                    onToggleChange(it)
                },
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (isToggleOn) {
            TextField(
                value = uploadUrl,
                onValueChange = {
                    uploadUrl = it
                    onUploadUrlChange(it)
                },
                label = { Text("Upload URL") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add custom command input and button in a Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            TextField(
                value = customCommand,
                onValueChange = { customCommand = it },
                label = { Text("Custom Command") },
                placeholder = { Text("01 0D") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onCustomCommand(customCommand) }) {
                Text("Send Command")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Text(text = statusMessage, modifier = Modifier.padding(top = 0.dp))
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            if (pairedDevicesMessage.isNotEmpty()) {
                Text(text = pairedDevicesMessage, modifier = Modifier.padding(top = 16.dp))
            }
            Text(text = obdData, modifier = Modifier.padding(top = 16.dp))
        }
    }
}