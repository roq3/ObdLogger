package cc.webdevel.obdlogger

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable

@Composable
fun rememberStatusBarHeight(): Int {
    val systemBarsInsets = WindowInsets.systemBars
    val density = LocalDensity.current
    return with(density) { systemBarsInsets.getTop(density).toInt() }
}