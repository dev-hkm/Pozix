package com.hkm.pozix.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AnswerButton(
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean?,
    isAnswered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected && !isAnswered) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isAnswered && isSelected && isCorrect == true -> MaterialTheme.colorScheme.primaryContainer
            isAnswered && isSelected && isCorrect == false -> MaterialTheme.colorScheme.errorContainer
            isSelected -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 300),
        label = "backgroundColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            isAnswered && isSelected && isCorrect == true -> MaterialTheme.colorScheme.onPrimaryContainer
            isAnswered && isSelected && isCorrect == false -> MaterialTheme.colorScheme.onErrorContainer
            isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 300),
        label = "contentColor"
    )
    
    val borderColor by animateColorAsState(
        targetValue = when {
            isAnswered && isSelected && isCorrect == true -> MaterialTheme.colorScheme.primary
            isAnswered && isSelected && isCorrect == false -> MaterialTheme.colorScheme.error
            isSelected -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(durationMillis = 300),
        label = "borderColor"
    )
    
    OutlinedButton(
        onClick = onClick,
        enabled = !isAnswered,
        modifier = modifier
            .scale(scale),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor,
            disabledContentColor = contentColor
        ),
        border = BorderStroke(2.dp, borderColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
        )
    }
}
