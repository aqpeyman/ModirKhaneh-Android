package ir.modirkhaneh.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(primary = Color(0xFF0F766E), onPrimary = Color.White, background = Color(0xFFF4F6F8), onBackground = Color(0xFF17212B), surface = Color.White, onSurface = Color(0xFF17212B))
private val Dark = darkColorScheme(primary = Color(0xFF5EEAD4), onPrimary = Color(0xFF06231F), background = Color(0xFF10161C), onBackground = Color(0xFFE8EDF2), surface = Color(0xFF19212A), onSurface = Color(0xFFE8EDF2))
@Composable fun ModirKhanehTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
