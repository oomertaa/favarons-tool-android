package ro.favaron.costuri.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF8B4A62),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E3),
    onPrimaryContainer = Color(0xFF3A0720),
    secondary = Color(0xFF7A5734),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCBE),
    onSecondaryContainer = Color(0xFF2C1600),
    tertiary = Color(0xFF55633A),
    tertiaryContainer = Color(0xFFD8E9B4),
    background = Color(0xFFFFF8F6),
    surface = Color(0xFFFFF8F6),
    surfaceVariant = Color(0xFFF3DDE1),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB0CA),
    onPrimary = Color(0xFF541D34),
    primaryContainer = Color(0xFF6F334A),
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary = Color(0xFFEDBD92),
    secondaryContainer = Color(0xFF5F401F),
    tertiary = Color(0xFFBCCD9A),
    tertiaryContainer = Color(0xFF3E4B25),
)

@Composable
fun FavaronTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
