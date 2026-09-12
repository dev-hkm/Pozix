package com.hkm.pozix.ui.screens

import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.core.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun Modifier.composerIdleGlow(enabled: Boolean, corner: Dp): Modifier {
    if (!enabled) return this
    val motion = rememberInfiniteTransition(label = "idleComposerGlow")
    val angle by motion.animateFloat(0f, 360f,
        infiniteRepeatable(tween(5500, easing = LinearEasing)), label = "glowAngle")
    val color = MaterialTheme.colorScheme.primary
    return drawWithCache {
        val matrix = Matrix()
        val shader = SweepGradient(size.width / 2, size.height / 2,
            intArrayOf(color.copy(alpha = 0f).toArgb(), color.copy(alpha = 0f).toArgb(),
                color.copy(alpha = 0.65f).toArgb(), color.copy(alpha = 0f).toArgb()),
            floatArrayOf(0f, 0.65f, 0.83f, 1f))
        val brush = ShaderBrush(shader)
        onDrawWithContent {
            drawContent()
            matrix.setRotate(angle, size.width / 2, size.height / 2)
            shader.setLocalMatrix(matrix)
            val inset = 1.dp.toPx()
            drawRoundRect(brush, topLeft = Offset(inset, inset),
                size = Size((size.width - inset * 2).coerceAtLeast(0f), (size.height - inset * 2).coerceAtLeast(0f)),
                cornerRadius = CornerRadius((corner.toPx() - inset).coerceAtLeast(0f)),
                style = Stroke(1.2.dp.toPx()))
        }
    }
}
