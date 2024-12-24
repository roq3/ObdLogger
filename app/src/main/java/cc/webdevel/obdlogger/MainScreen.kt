package cc.webdevel.obdlogger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(
    statusBarHeight: Int,
    statusMessage: String,
    errorMessage: String,
    pairedDevicesMessage: String,
    onConnectClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(
            top = statusBarHeight.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp
        )
    ) {
        Text(text = "Bluetooth Connection")
        Button(onClick = onConnectClick, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = "Connect")
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
    }
}