package com.hkm.pozix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.hkm.pozix.R
import com.hkm.pozix.data.model.QuizReviewPayload
import com.hkm.pozix.ui.theme.readableContentColorFor

@Composable
fun QuizReviewDraftCard(
    payload: QuizReviewPayload,
    enabled: Boolean,
    onSend: () -> Unit,
    onDismiss: () -> Unit
) {
    val cardColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
    val cardTextColor = readableContentColorFor(
        cardColor.compositeOver(MaterialTheme.colorScheme.background),
        MaterialTheme.colorScheme.onPrimaryContainer
    )
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = MaterialTheme.shapes.large,
        color = cardColor,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RateReview,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.ai_review_card_title),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cardTextColor
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.ai_review_card_dismiss),
                        tint = cardTextColor
                    )
                }
            }
            Text(
                text = payload.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = cardTextColor
            )
            Text(
                text = listOf(
                    stringResource(R.string.ai_review_card_score, payload.score, payload.totalQuestions),
                    stringResource(R.string.ai_review_card_correct, payload.items.count { it.isCorrect }),
                    stringResource(R.string.ai_review_card_unanswered, payload.items.count { it.selectedIndex == null })
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = cardTextColor.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 3.dp)
            )
            Button(
                onClick = onSend,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.ai_review_card_send))
            }
        }
    }
}
