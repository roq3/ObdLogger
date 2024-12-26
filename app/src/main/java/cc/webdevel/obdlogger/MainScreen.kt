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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    statusBarHeight: Int,
    statusMessage: String,
    errorMessage: String,
    obdData: String,
    pairedDevicesMessage: String,
    onConnectClick: () -> Unit,
    isConnected: Boolean,
    onUploadUrlChange: (String) -> Unit,
    onToggleChange: (Boolean) -> Unit,
    uploadUrlString: String
) {
    val scrollState = rememberScrollState()
    var uploadUrl by remember { mutableStateOf(uploadUrlString) }
    var isToggleOn by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(
                top = statusBarHeight.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            )
            .verticalScroll(scrollState)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Button(onClick = onConnectClick) {
                Text(text = if (isConnected) "Disconnect" else "Connect")
            }

            Spacer(modifier = Modifier.width(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(text = statusMessage, modifier = Modifier.padding(top = 16.dp))
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