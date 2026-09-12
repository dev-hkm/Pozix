package com.hkm.pozix.ui.screens

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

/** Brief arrival treatment, never replayed on token updates or settled history. */
@Composable
internal fun Modifier.softStreamEntry(streaming: Boolean): Modifier {
    val progress = remember { Animatable(if (streaming) 0f else 1f) }
    LaunchedEffect(Unit) { if (progress.value < 1f) progress.animateTo(1f, tween(170)) }
    return graphicsLayer {
        val value = progress.value
        alpha = 0.72f + 0.28f * value
        if (Build.VERSION.SDK_INT >= 31) {
            val radius = (1f - value) * 1.4f
            renderEffect = if (radius > 0.05f) android.graphics.RenderEffect.createBlurEffect(
                radius, radius, android.graphics.Shader.TileMode.CLAMP
            ).asComposeRenderEffect() else null
        }
    }
}
