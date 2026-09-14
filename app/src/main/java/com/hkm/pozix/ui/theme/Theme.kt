package com.hkm.pozix.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight
)

@Composable
fun PozixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    fontFamily: String = "default",
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            dynamic.harmonize(darkTheme)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val typography = createTypography(getFontFamily(fontFamily))

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

/**
 * Harmonizes dynamic color schemes extracted from Android 12+ Monet engine.
 *
 * Preserves the user's dynamic wallpaper hue, saturation, and theme accent colors,
 * while safeguarding against OEM Monet bugs (such as Xiaomi HyperOS / MIUI 14
 * inverting container luminance to Tone 90 in dark mode).
 */
private fun ColorScheme.harmonize(isDark: Boolean): ColorScheme {
    return if (isDark) {
        val darkBg = if (background.luminance() > 0.25f) Color(0xFF131218) else background
        val darkSurface = if (surface.luminance() > 0.25f) Color(0xFF131218) else surface
        val lightOnSurface = if (onSurface.luminance() < 0.55f) Color(0xFFE6E1E5) else onSurface
        val lightOnBg = if (onBackground.luminance() < 0.55f) Color(0xFFE6E1E5) else onBackground

        val safePrimaryContainer = ensureDarkContainer(primaryContainer, 0.28f)
        val safeSecondaryContainer = ensureDarkContainer(secondaryContainer, 0.26f)
        val safeTertiaryContainer = ensureDarkContainer(tertiaryContainer, 0.28f)
        val safeErrorContainer = ensureDarkContainer(errorContainer, 0.28f)
        val safeSurfaceVariant = ensureDarkContainer(surfaceVariant, 0.22f)

        copy(
            background = darkBg,
            onBackground = lightOnBg,
            surface = darkSurface,
            onSurface = lightOnSurface,
            surfaceVariant = safeSurfaceVariant,
            onSurfaceVariant = ensureContrastingOn(safeSurfaceVariant, onSurfaceVariant, lightOnSurface),
            primaryContainer = safePrimaryContainer,
            onPrimaryContainer = ensureContrastingOn(safePrimaryContainer, onPrimaryContainer, lightOnSurface),
            secondaryContainer = safeSecondaryContainer,
            onSecondaryContainer = ensureContrastingOn(safeSecondaryContainer, onSecondaryContainer, lightOnSurface),
            tertiaryContainer = safeTertiaryContainer,
            onTertiaryContainer = ensureContrastingOn(safeTertiaryContainer, onTertiaryContainer, lightOnSurface),
            errorContainer = safeErrorContainer,
            onErrorContainer = ensureContrastingOn(safeErrorContainer, onErrorContainer, lightOnSurface),
            surfaceContainerLowest = ensureDarkContainer(surfaceContainerLowest, 0.08f),
            surfaceContainerLow = ensureDarkContainer(surfaceContainerLow, 0.12f),
            surfaceContainer = ensureDarkContainer(surfaceContainer, 0.15f),
            surfaceContainerHigh = ensureDarkContainer(surfaceContainerHigh, 0.20f),
            surfaceContainerHighest = ensureDarkContainer(surfaceContainerHighest, 0.24f)
        )
    } else {
        val lightBg = if (background.luminance() < 0.70f) Color(0xFFFEF7FF) else background
        val lightSurface = if (surface.luminance() < 0.70f) Color(0xFFFEF7FF) else surface
        val darkOnSurface = if (onSurface.luminance() > 0.40f) Color(0xFF1D1B20) else onSurface
        val darkOnBg = if (onBackground.luminance() > 0.40f) Color(0xFF1D1B20) else onBackground

        val safePrimaryContainer = ensureLightContainer(primaryContainer, 0.90f)
        val safeSecondaryContainer = ensureLightContainer(secondaryContainer, 0.90f)
        val safeTertiaryContainer = ensureLightContainer(tertiaryContainer, 0.90f)
        val safeErrorContainer = ensureLightContainer(errorContainer, 0.90f)
        val safeSurfaceVariant = ensureLightContainer(surfaceVariant, 0.90f)

        copy(
            background = lightBg,
            onBackground = darkOnBg,
            surface = lightSurface,
            onSurface = darkOnSurface,
            surfaceVariant = safeSurfaceVariant,
            onSurfaceVariant = ensureContrastingOn(safeSurfaceVariant, onSurfaceVariant, darkOnSurface),
            primaryContainer = safePrimaryContainer,
            onPrimaryContainer = ensureContrastingOn(safePrimaryContainer, onPrimaryContainer, darkOnSurface),
            secondaryContainer = safeSecondaryContainer,
            onSecondaryContainer = ensureContrastingOn(safeSecondaryContainer, onSecondaryContainer, darkOnSurface),
            tertiaryContainer = safeTertiaryContainer,
            onTertiaryContainer = ensureContrastingOn(safeTertiaryContainer, onTertiaryContainer, darkOnSurface),
            errorContainer = safeErrorContainer,
            onErrorContainer = ensureContrastingOn(safeErrorContainer, onErrorContainer, darkOnSurface),
            surfaceContainerLowest = ensureLightContainer(surfaceContainerLowest, 0.98f),
            surfaceContainerLow = ensureLightContainer(surfaceContainerLow, 0.96f),
            surfaceContainer = ensureLightContainer(surfaceContainer, 0.94f),
            surfaceContainerHigh = ensureLightContainer(surfaceContainerHigh, 0.92f),
            surfaceContainerHighest = ensureLightContainer(surfaceContainerHighest, 0.90f)
        )
    }
}

private fun ensureDarkContainer(color: Color, targetBrightness: Float): Color {
    if (color.luminance() <= 0.35f) return color
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
        hsv
    )
    hsv[2] = targetBrightness
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun ensureLightContainer(color: Color, targetBrightness: Float): Color {
    if (color.luminance() >= 0.45f) return color
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
        hsv
    )
    hsv[2] = targetBrightness
    hsv[1] = hsv[1].coerceAtMost(0.40f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun ensureContrastingOn(container: Color, preferredOn: Color, fallbackOn: Color): Color {
    return readableContentColor(
        background = container,
        preferred = preferredOn,
        fallbackA = fallbackOn,
        fallbackB = if (container.luminance() < 0.5f) Color.White else Color(0xFF1B1B1F)
    )
}
