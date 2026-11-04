package br.com.paymentsimulator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = SlateOnSecondary,
    secondaryContainer = SlateSecondaryContainer,
    onSecondaryContainer = SlateOnSecondaryContainer,
    tertiary = GoldTertiary,
    onTertiary = GoldOnTertiary,
    tertiaryContainer = GoldTertiaryContainer,
    onTertiaryContainer = GoldOnTertiaryContainer,
    background = Paper,
    onBackground = Ink,
    surface = SurfaceWhite,
    onSurface = Ink,
    surfaceVariant = SurfaceHighest,
    onSurfaceVariant = InkMuted,
    surfaceTint = GreenPrimary,
    surfaceBright = PaperBright,
    surfaceDim = SurfaceDim,
    surfaceContainerLowest = SurfaceWhite,
    surfaceContainerLow = SurfaceLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHighest,
    error = Danger,
    onError = GreenOnPrimary,
    errorContainer = DangerContainer,
    onErrorContainer = DangerOnContainer,
    outline = Outline,
    outlineVariant = OutlineVariant,
    inverseSurface = NightSurface,
    inverseOnSurface = InkDark,
    inversePrimary = GreenPrimaryDark,
    scrim = Ink,
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = GreenOnPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    onPrimaryContainer = GreenOnPrimaryContainerDark,
    secondary = SlateSecondaryDark,
    onSecondary = SlateOnSecondaryDark,
    secondaryContainer = SlateSecondaryContainerDark,
    onSecondaryContainer = SlateOnSecondaryContainerDark,
    tertiary = GoldTertiaryDark,
    onTertiary = GoldOnTertiaryDark,
    tertiaryContainer = GoldTertiaryContainerDark,
    onTertiaryContainer = GoldOnTertiaryContainerDark,
    background = Night,
    onBackground = InkDark,
    surface = NightSurface,
    onSurface = InkDark,
    surfaceVariant = NightHighest,
    onSurfaceVariant = InkMutedDark,
    surfaceTint = GreenPrimaryDark,
    surfaceBright = NightHighest,
    surfaceDim = Night,
    surfaceContainerLowest = Night,
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightLow,
    surfaceContainerHigh = NightContainer,
    surfaceContainerHighest = NightHigh,
    error = DangerDark,
    onError = DangerOnDark,
    errorContainer = DangerContainerDark,
    onErrorContainer = DangerOnContainerDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = SurfaceLow,
    inverseOnSurface = Ink,
    inversePrimary = GreenPrimary,
    scrim = Night,
)

@Composable
fun PaymentSimulatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
