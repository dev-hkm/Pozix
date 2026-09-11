package com.hkm.pozix.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hkm.pozix.R
import com.hkm.pozix.viewmodel.AiPhase

@Composable
internal fun AiPhaseLine(phase: AiPhase) {
    if (phase == AiPhase.IDLE) return
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
        Crossfade(phase, animationSpec = tween(180), label = "processingPhase") { current ->
            Text(stringResource(when (current) {
                AiPhase.REASONING -> R.string.ai_phase_reasoning
                AiPhase.RESPONDING -> R.string.ai_phase_responding
                AiPhase.VALIDATING -> R.string.ai_phase_validating
                AiPhase.REPAIRING -> R.string.ai_phase_repairing
                AiPhase.SAVING -> R.string.ai_phase_saving
                else -> R.string.ai_phase_waiting
            }), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
