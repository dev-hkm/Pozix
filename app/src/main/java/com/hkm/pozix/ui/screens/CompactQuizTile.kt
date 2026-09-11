package com.hkm.pozix.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hkm.pozix.R
import com.hkm.pozix.data.model.SavedQuizSet

@Composable
internal fun CompactQuizTile(quiz: SavedQuizSet, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Quiz, null, tint = MaterialTheme.colorScheme.primary)
            Text(quiz.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                minLines = 3, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.saved_quiz_sets_question_count, quiz.questionCount),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress = { (quiz.progressPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.quiz_open_actions), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}
