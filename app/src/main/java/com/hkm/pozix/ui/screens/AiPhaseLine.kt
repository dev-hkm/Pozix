package com.hkm.pozix.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hkm.pozix.R
import com.hkm.pozix.viewmodel.AiPhase
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Android 15 / Material 3 Expressive Morphing AI Indicator.
 * Three fluid, organic shapes that continuously morph corner radii, scale,
 * and subtly rotate in a smooth breathing cycle.
 */
@Composable
fun ExpressiveMorphingAiIndicator(
    modifier: Modifier = Modifier,
    height: Dp = 18.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "expressiveMorph")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "morphPhase"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val sparkColor = Color(0xFF7A8FC8)
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MorphingDot(
            phase = phase,
            phaseOffset = 0f,
            color = primaryColor,
            baseSize = 9.dp
        )
        MorphingDot(
            phase = phase,
            phaseOffset = 0.33f,
            color = sparkColor,
            baseSize = 11.dp
        )
        MorphingDot(
            phase = phase,
            phaseOffset = 0.66f,
            color = tertiaryColor,
            baseSize = 8.dp
        )
    }
}

@Composable
private fun MorphingDot(
    phase: Float,
    phaseOffset: Float,
    color: Color,
    baseSize: Dp
) {
    val localPhase = (phase + phaseOffset) % 1f
    val wave = sin(localPhase * 2 * PI.toFloat())
    val scale = 0.72f + 0.38f * ((wave + 1f) / 2f)

    // Morph corner radius between rounded squircle (25%) and circle (50%)
    val cornerPercent = (26 + (24 * ((cos(localPhase * 2 * PI.toFloat()) + 1f) / 2f))).toInt()
    val rotation = wave * 22f

    Box(
        modifier = Modifier
            .size(baseSize * scale)
            .graphicsLayer {
                rotationZ = rotation
                clip = true
                shape = RoundedCornerShape(cornerPercent)
            }
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        color.copy(alpha = 0.95f),
                        color.copy(alpha = 0.65f)
                    )
                )
            )
    )
}

@Composable
internal fun AiPhaseLine(phase: AiPhase, quizGeneration: Boolean = false) {
    if (phase == AiPhase.IDLE) return

    val infiniteTransition = rememberInfiniteTransition(label = "textShimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExpressiveMorphingAiIndicator()

            Crossfade(
                targetState = phase,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                label = "processingPhase"
            ) { current ->
                Text(
                    text = stringResource(when {
                        quizGeneration && current == AiPhase.RESPONDING -> R.string.ai_phase_quiz_receiving
                        quizGeneration && current == AiPhase.VALIDATING -> R.string.ai_phase_quiz_validating
                        quizGeneration && current == AiPhase.REPAIRING -> R.string.ai_phase_quiz_repairing
                        quizGeneration && current == AiPhase.SAVING -> R.string.ai_phase_quiz_saving
                        current == AiPhase.FETCHING_SOURCE -> R.string.ai_phase_youtube_fetching
                        current == AiPhase.PREPARING_SOURCE -> R.string.ai_phase_youtube_preparing
                        current == AiPhase.REASONING -> R.string.ai_phase_reasoning
                        current == AiPhase.RESPONDING -> R.string.ai_phase_responding
                        current == AiPhase.VALIDATING -> R.string.ai_phase_validating
                        current == AiPhase.REPAIRING -> R.string.ai_phase_repairing
                        current == AiPhase.SAVING -> R.string.ai_phase_saving
                        else -> R.string.ai_phase_waiting
                    }),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
