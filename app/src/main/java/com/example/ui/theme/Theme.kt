package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class LedgerExtendedColors(
    val presentBg: Color,
    val presentText: Color,
    val absentBg: Color,
    val absentText: Color,
    val unmarkedBg: Color,
    val unmarkedText: Color,
    val sundayColumnBg: Color,
    val sundayHeaderBg: Color,
    val sundayText: Color,
    val cardBorder: Color,
    val subtleDivider: Color
)

val LocalLedgerColors = staticCompositionLocalOf {
    LedgerExtendedColors(
        presentBg = EmeraldContainer,
        presentText = EmeraldTertiary,
        absentBg = CrimsonContainer,
        absentText = CrimsonError,
        unmarkedBg = LedgerSurfaceVariant,
        unmarkedText = LedgerTextSecondary,
        sundayColumnBg = SundayColumnLight,
        sundayHeaderBg = SundayHeaderLight,
        sundayText = SundayTextLight,
        cardBorder = LedgerOutline,
        subtleDivider = LedgerOutlineSubtle
    )
}

private val LightLedgerColors = LedgerExtendedColors(
    presentBg = EmeraldContainer,
    presentText = EmeraldTertiary,
    absentBg = CrimsonContainer,
    absentText = CrimsonError,
    unmarkedBg = LedgerSurfaceVariant,
    unmarkedText = LedgerTextSecondary,
    sundayColumnBg = SundayColumnLight,
    sundayHeaderBg = SundayHeaderLight,
    sundayText = SundayTextLight,
    cardBorder = LedgerOutline,
    subtleDivider = LedgerOutlineSubtle
)

private val DarkLedgerColors = LedgerExtendedColors(
    presentBg = EmeraldContainerDark,
    presentText = EmeraldTertiaryDark,
    absentBg = CrimsonContainerDark,
    absentText = CrimsonErrorDark,
    unmarkedBg = LedgerDarkSurfaceVariant,
    unmarkedText = LedgerDarkTextSecondary,
    sundayColumnBg = SundayColumnDark,
    sundayHeaderBg = SundayHeaderDark,
    sundayText = SundayTextDark,
    cardBorder = LedgerDarkOutline,
    subtleDivider = LedgerDarkOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = NavyPrimaryDark,
    onPrimary = OnNavyPrimaryDark,
    primaryContainer = NavyContainerDark,
    onPrimaryContainer = OnNavyContainerDark,
    secondary = AmberSecondaryDark,
    onSecondary = OnAmberSecondaryDark,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = OnAmberContainerDark,
    tertiary = EmeraldTertiaryDark,
    onTertiary = OnEmeraldTertiaryDark,
    tertiaryContainer = EmeraldContainerDark,
    onTertiaryContainer = OnEmeraldContainerDark,
    error = CrimsonErrorDark,
    onError = OnCrimsonErrorDark,
    errorContainer = CrimsonContainerDark,
    onErrorContainer = OnCrimsonContainerDark,
    background = LedgerDarkBg,
    onBackground = LedgerDarkTextPrimary,
    surface = LedgerDarkSurface,
    onSurface = LedgerDarkTextPrimary,
    surfaceVariant = LedgerDarkSurfaceVariant,
    onSurfaceVariant = LedgerDarkTextSecondary,
    outline = LedgerDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = OnNavyPrimary,
    primaryContainer = NavyContainer,
    onPrimaryContainer = OnNavyContainer,
    secondary = AmberSecondary,
    onSecondary = OnAmberSecondary,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = OnAmberContainer,
    tertiary = EmeraldTertiary,
    onTertiary = OnEmeraldTertiary,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = OnEmeraldContainer,
    error = CrimsonError,
    onError = OnCrimsonError,
    errorContainer = CrimsonContainer,
    onErrorContainer = OnCrimsonContainer,
    background = LedgerPaperBg,
    onBackground = LedgerTextPrimary,
    surface = LedgerSurface,
    onSurface = LedgerTextPrimary,
    surfaceVariant = LedgerSurfaceVariant,
    onSurfaceVariant = LedgerTextSecondary,
    outline = LedgerOutline
)

@Composable
fun JslLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = if (darkTheme) DarkLedgerColors else LightLedgerColors

    CompositionLocalProvider(LocalLedgerColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
