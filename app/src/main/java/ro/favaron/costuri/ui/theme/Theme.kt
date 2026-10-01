package ro.favaron.costuri.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ro.favaron.costuri.R

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

/**
 * App font: Arial Narrow look (Liberation Sans Narrow, metric-compatible with
 * Arial Narrow). To use the original Arial Narrow instead, replace the two
 * files in res/font with ARIALN.TTF / ARIALNB.TTF renamed to
 * app_font_regular.ttf / app_font_bold.ttf.
 */
val AppFont = FontFamily(
    Font(R.font.app_font_regular, FontWeight.Normal),
    Font(R.font.app_font_bold, FontWeight.Bold),
)

/** Every text style uses the narrow font at 12 sp; titles are bold. */
private val AppTypography: Typography = Typography().let { base ->
    fun TextStyle.app(bold: Boolean = false) = copy(
        fontFamily = AppFont,
        fontSize = APP_FONT_SIZE,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    )
    base.copy(
        displayLarge = base.displayLarge.app(true),
        displayMedium = base.displayMedium.app(true),
        displaySmall = base.displaySmall.app(true),
        headlineLarge = base.headlineLarge.app(true),
        headlineMedium = base.headlineMedium.app(true),
        headlineSmall = base.headlineSmall.app(true),
        titleLarge = base.titleLarge.app(true),
        titleMedium = base.titleMedium.app(true),
        titleSmall = base.titleSmall.app(true),
        bodyLarge = base.bodyLarge.app(),
        bodyMedium = base.bodyMedium.app(),
        bodySmall = base.bodySmall.app(),
        labelLarge = base.labelLarge.app(true),
        labelMedium = base.labelMedium.app(),
        labelSmall = base.labelSmall.app(),
    )
}

val APP_FONT_SIZE = 12.sp

@Composable
fun FavaronTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        content = content,
    )
}
