package com.hkm.pozix.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.pozix.R
import com.hkm.pozix.data.model.Question
import com.hkm.pozix.ui.theme.readableContentColorFor
import com.hkm.pozix.util.HapticUtil
import com.hkm.pozix.viewmodel.ExamState

@Composable
fun ExamResultScreen(
    state: ExamState.Finished,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showStats by remember { mutableStateOf(false) }
    var showReviewButton by remember { mutableStateOf(false) }
    var showReview by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }

    val animatedScore by animateFloatAsState(
        targetValue = state.scoreOutOf10.toFloat(),
        animationSpec = tween(1200, easing = LinearEasing),
        label = "scoreAnim"
    )
    val displayScore = String.format(java.util.Locale.US, "%.1f", animatedScore)

    LaunchedEffect(Unit) {
        HapticUtil.lightTap(context)
        kotlinx.coroutines.delay(300)
        showStats = true
        kotlinx.coroutines.delay(200)
        showReviewButton = true
        kotlinx.coroutines.delay(200)
        showActions = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.quiz_back)
                )
            }
            Text(
                text = stringResource(R.string.exam_result_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$displayScore",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "/ 10.0",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.exam_result_correct_of,
                        state.correctCount,
                        state.totalQuestions
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = when (state.gradeColor) {
                        "green" -> MaterialTheme.colorScheme.primary
                        "blue" -> MaterialTheme.colorScheme.secondary
                        "orange" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    },
                    contentColor = when (state.gradeColor) {
                        "green" -> MaterialTheme.colorScheme.onPrimary
                        "blue" -> MaterialTheme.colorScheme.onSecondary
                        "orange" -> MaterialTheme.colorScheme.onTertiary
                        else -> MaterialTheme.colorScheme.onError
                    },
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = stringResource(R.string.exam_result_grade, state.grade),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        AnimatedVisibility(
            visible = showStats,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val wrongCount = state.totalQuestions - state.correctCount - state.unansweredCount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ResultStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        label = stringResource(R.string.exam_result_correct),
                        value = "${state.correctCount}"
                    )
                    ResultStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Cancel,
                        label = stringResource(R.string.exam_result_wrong),
                        value = "$wrongCount"
                    )
                    ResultStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        label = stringResource(R.string.exam_result_skipped),
                        value = "${state.unansweredCount}"
                    )
                }
                ResultStatCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Timer,
                    label = stringResource(R.string.exam_result_time),
                    value = formatExamTime(state.timeUsedMillis)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = showReviewButton,
            enter = fadeIn(tween(300)) + expandVertically(tween(300))
        ) {
            OutlinedButton(
            onClick = {
                HapticUtil.ultraLightTap(context)
                showReview = !showReview
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (showReview) stringResource(R.string.exam_hide_review)
                else stringResource(R.string.exam_show_review),
                fontWeight = FontWeight.Medium
            )
        }
        }

        AnimatedVisibility(
            visible = showReview,
            enter = fadeIn(tween(300)) + expandVertically(tween(400)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                state.questions.forEachIndexed { index, question ->
                    ReviewQuestionCard(
                        index = index + 1,
                        question = question,
                        selectedAnswer = state.answers[index]
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = showActions,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 }
        ) {
            Column {
                OutlinedButton(onClick = {
                    HapticUtil.lightTap(context)
                    com.hkm.pozix.util.QuizAiFollowUp.queue(context, com.hkm.pozix.util.QuizAiFollowUp.report(
                        state.quizTitle, state.questions, state.answers, state.correctCount, state.timeUsedMillis))
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.ai_review_results))
                }
                Spacer(Modifier.height(12.dp))
                Button(
            onClick = {
                HapticUtil.lightTap(context)
                onRetry()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.exam_retry),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                HapticUtil.ultraLightTap(context)
                onBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = stringResource(R.string.back_to_library),
                fontWeight = FontWeight.Medium
            )
        }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ResultStatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReviewQuestionCard(
    index: Int,
    question: Question,
    selectedAnswer: Int?
) {
    val questionText = question.question
    val correctIndex = when (question) {
        is Question.SingleChoice -> question.correctIndex
        is Question.TrueFalse -> if (question.correctAnswer) 0 else 1
    }
    val options = when (question) {
        is Question.SingleChoice -> question.options
        is Question.TrueFalse -> listOf(
            stringResource(R.string.quiz_true),
            stringResource(R.string.quiz_false)
        )
    }
    val isUnanswered = selectedAnswer == null
    val isCorrect = selectedAnswer == correctIndex
    val explanation = question.explanation

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$index",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = questionText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (isUnanswered) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.exam_result_unanswered_label),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            options.forEachIndexed { optIndex, option ->
                val isThisCorrect = optIndex == correctIndex
                val isThisSelected = optIndex == selectedAnswer

                val bgColor = when {
                    isThisCorrect -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    isThisSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                }
                val txtColor = when {
                    isThisCorrect -> MaterialTheme.colorScheme.onPrimaryContainer
                    isThisSelected && !isCorrect -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
                val readableTxtColor = readableContentColorFor(
                    bgColor.compositeOver(MaterialTheme.colorScheme.background),
                    txtColor
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${('A' + optIndex)}.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = readableTxtColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = option,
                        fontSize = 13.sp,
                        color = readableTxtColor,
                        modifier = Modifier.weight(1f)
                    )
                    if (isThisCorrect) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (explanation?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    val explanationColor = readableContentColorFor(
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                            .compositeOver(MaterialTheme.colorScheme.background),
                        MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = explanationColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = explanationColor,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

fun formatExamTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
