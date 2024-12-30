package cc.webdevel.obdlogger

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import cc.webdevel.obdlogger.ui.theme.*

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
    onCustomCommand: (String) -> Unit,
    onFetchDataClick: () -> Unit,
    showFetchDataButton: Boolean,
    isLoading: MutableState<Boolean>
) {
    val scrollState = rememberScrollState()
    var uploadUrl by remember { mutableStateOf(uploadUrlString) }
    var isToggleOn by remember { mutableStateOf(false) }
    var customCommand by remember { mutableStateOf("01 00") }
    var isCustomCommandEnabled by remember { mutableStateOf(false) }

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Button(onClick = onConnectClick) {
                Text(text = if (isConnected) "Disconnect" else "Connect")
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isConnected && showFetchDataButton) {
                Button(onClick = onFetchDataClick) {
                    Text(text = "Fetch Data")
                }
            }
        }
        if (isConnected) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(text = "Custom Command", fontSize = 16.sp)
                Switch(
                    checked = isCustomCommandEnabled,
                    onCheckedChange = { isCustomCommandEnabled = it },
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(text = "Upload", fontSize = 16.sp)
                Switch(
                    checked = isToggleOn,
                    onCheckedChange = {
                        isToggleOn = it
                        onToggleChange(it)
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
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

        if (isCustomCommandEnabled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
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
                Button(
                    onClick = {
                        isLoading.value = true
                        onCustomCommand(customCommand)
                    },
                    enabled = !isLoading.value
                ) {
                    if (isLoading.value) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send Command")
                    }
                }
            }
        }

//        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            if (statusMessage.isNotEmpty()) {
                Text(
                    text = "Status:\n$statusMessage",
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .padding(bottom = 0.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AlertSuccess.copy(alpha = 1f))
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()

                )
            }
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .padding(bottom = 0.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AlertError.copy(alpha = 1f))
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()

                )
            }
            if (isConnected && obdData.isNotEmpty()) {
                Text(
                    text = obdData,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .padding(bottom = 0.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            }
            if (pairedDevicesMessage.isNotEmpty()) {
                Text(
                    text = pairedDevicesMessage,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .padding(bottom = 0.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AlertSuccess.copy(alpha = 1f))
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                    ,
                )
            }
        }
    }
}