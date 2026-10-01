package ro.favaron.costuri.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

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
    surfaceContainer = Color(0xFFF7E6EA),
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
    surfaceContainer = Color(0xFF2A2124),
)

/** Text size multiplier over the Material 3 defaults (system font, slightly bigger). */
const val FONT_SCALE = 1.15f

private val AppTypography: Typography = Typography().let { base ->
    fun TextStyle.bigger() = copy(fontSize = fontSize * FONT_SCALE, lineHeight = lineHeight * FONT_SCALE)
    base.copy(
        displayLarge = base.displayLarge.bigger(),
        displayMedium = base.displayMedium.bigger(),
        displaySmall = base.displaySmall.bigger(),
        headlineLarge = base.headlineLarge.bigger(),
        headlineMedium = base.headlineMedium.bigger(),
        headlineSmall = base.headlineSmall.bigger(),
        titleLarge = base.titleLarge.bigger(),
        titleMedium = base.titleMedium.bigger(),
        titleSmall = base.titleSmall.bigger(),
        bodyLarge = base.bodyLarge.bigger(),
        bodyMedium = base.bodyMedium.bigger(),
        bodySmall = base.bodySmall.bigger(),
        labelLarge = base.labelLarge.bigger(),
        labelMedium = base.labelMedium.bigger(),
        labelSmall = base.labelSmall.bigger(),
    )
}

@Composable
fun FavaronTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        content = content,
    )
}
