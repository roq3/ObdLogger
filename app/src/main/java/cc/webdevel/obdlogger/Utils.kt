package cc.webdevel.obdlogger

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@Composable
fun rememberStatusBarHeight(): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    val insets = ViewCompat.getRootWindowInsets(view)
    val statusBarHeight = insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
    return with(density) { statusBarHeight.toDp() }
}