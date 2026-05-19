package vn.kba2018.attendance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF16A34A)
private val GreenDark = Color(0xFF15803D)

private val Light = lightColorScheme(primary = Green, secondary = GreenDark)
private val Dark = darkColorScheme(primary = Green, secondary = GreenDark)

@Composable
fun KBATheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, content = content)
}
