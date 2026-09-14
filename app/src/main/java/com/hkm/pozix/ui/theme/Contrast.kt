package com.hkm.pozix.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Picks the most readable color for a rendered surface.
 *
 * Dynamic color providers are allowed to produce different container tones
 * across devices. Using a semantic `on*Container` pair blindly can therefore
 * result in light text on a light container (or the inverse). Keep the
 * semantic color when it has enough contrast, otherwise choose the better of
 * the two theme-aware surface foregrounds.
 */
internal fun readableContentColor(
    background: Color,
    preferred: Color,
    fallbackA: Color,
    fallbackB: Color
): Color {
    if (contrastRatio(background, preferred) >= 4.5f) {
        return preferred
    }
    val bestFallback = if (contrastRatio(background, fallbackA) >= contrastRatio(background, fallbackB)) {
        fallbackA
    } else {
        fallbackB
    }
    if (contrastRatio(background, bestFallback) >= 4.5f) {
        return bestFallback
    }
    return if (contrastRatio(background, Color.White) >= contrastRatio(background, Color(0xFF1B1B1F))) {
        Color.White
    } else {
        Color(0xFF1B1B1F)
    }
}

@Composable
internal fun readableContentColorFor(
    background: Color,
    preferred: Color
): Color = readableContentColor(
    background = background,
    preferred = preferred,
    fallbackA = MaterialTheme.colorScheme.onSurface,
    fallbackB = MaterialTheme.colorScheme.inverseOnSurface
)

internal fun contrastRatio(first: Color, second: Color): Float {
    val firstLuminance = first.luminance()
    val secondLuminance = second.luminance()
    val lighter = maxOf(firstLuminance, secondLuminance)
    val darker = minOf(firstLuminance, secondLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}
