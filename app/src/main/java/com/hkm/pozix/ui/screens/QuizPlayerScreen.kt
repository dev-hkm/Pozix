package com.hkm.pozix.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkm.pozix.R
import com.hkm.pozix.data.model.Question
import com.hkm.pozix.ui.components.media.QuestionMediaContent
import com.hkm.pozix.ui.components.QuizResultDialog
import com.hkm.pozix.ui.components.richcontent.RichContentText
import com.hkm.pozix.util.HapticUtil
import com.hkm.pozix.viewmodel.QuizPlayerViewModel
import com.hkm.pozix.viewmodel.QuizState

@Composable
fun QuizPlayerScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuizPlayerViewModel = viewModel()
) {
    val quizState by viewModel.quizState.collectAsState()
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }
    
    BackHandler {
        if (quizState is QuizState.Playing) {
            showExitDialog = true
        } else {
            onNavigateBack()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = quizState) {
            is QuizState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.quiz_loading),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            
            is QuizState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.quiz_error),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text(stringResource(R.string.quiz_go_back))
                        }
                    }
                }
            }
            
            is QuizState.Playing -> {
                PlayingContent(
                    state = state,
                    onNavigateBack = {
                        showExitDialog = true
                    },
                    onSelectAnswer = { index ->
                        viewModel.selectAnswer(index)
                        HapticUtil.lightTap(context)
                    },
                    onNext = {
                        viewModel.nextQuestion()
                        HapticUtil.selectionTick(context)
                    },
                    onPrevious = {
                        viewModel.previousQuestion()
                        HapticUtil.veryLightTap(context)
                    }
                )
            }
            
            is QuizState.Finished -> {
                QuizResultDialog(
                    onReviewWithAi = {
                        HapticUtil.lightTap(context)
                        com.hkm.pozix.util.QuizAiFollowUp.queue(context, com.hkm.pozix.util.QuizAiFollowUp.report(
                            state.quizTitle, state.questions, state.selectedAnswers, state.score, state.elapsedTimeMillis))
                    },
                    quizTitle = state.quizTitle,
                    score = state.score,
                    totalQuestions = state.totalQuestions,
                    onReplay = {
                        viewModel.restartQuiz()
                        HapticUtil.lightTap(context)
                    },
                    onHome = {
                        onNavigateBack()
                        HapticUtil.veryLightTap(context)
                    }
                )
            }
        }
    }
    
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.quiz_quit_title)) },
            text = { Text(stringResource(R.string.quiz_quit_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.quiz_quit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.quiz_continue))
                }
            }
        )
    }
}

@Composable
fun PlayingContent(
    state: QuizState.Playing,
    onNavigateBack: () -> Unit,
    onSelectAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit = {}
) {
    val currentQuestion = state.questions[state.currentQuestionIndex]
    val context = LocalContext.current
    val showBottomActions = state.isAnswered
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val answerContentPadding = PaddingValues(
        top = 4.dp,
        bottom = if (showBottomActions) 84.dp + navBarBottom else 24.dp + navBarBottom
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // ZONE A, B, C in Main Full-Height Column (Answers flow underneath floating buttons)
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ZONE A: TOP STATS AND PROGRESS
                    TopHeaderBar(
                        currentQuestionIndex = state.currentQuestionIndex,
                        totalQuestions = state.questions.size,
                        score = state.score,
                        elapsedTimeMillis = state.elapsedTimeMillis,
                        onClose = onNavigateBack
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    SmoothProgressBar(
                        currentQuestionIndex = state.currentQuestionIndex,
                        totalQuestions = state.questions.size
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // ZONE B: QUESTION CARD (Adaptive height, crisp & flat)
                    AdaptiveQuestionCard(
                        questionNumber = state.currentQuestionIndex + 1,
                        questionText = currentQuestion.question,
                        media = currentQuestion.media,
                        readOnly = state.isAnswered
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // ZONE C: ANSWER AREA (Adaptive layout - options scroll seamlessly down to bottom)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        when (currentQuestion) {
                            is Question.SingleChoice -> {
                                SmartAnswerLayout(
                                    options = currentQuestion.options,
                                    selectedIndex = state.selectedAnswerIndex,
                                    isAnswered = state.isAnswered,
                                    correctIndex = currentQuestion.correctIndex,
                                    onSelectAnswer = onSelectAnswer,
                                    explanation = currentQuestion.explanation,
                                    showExplanation = state.showExplanation,
                                    contentPadding = answerContentPadding
                                )
                            }
                            is Question.TrueFalse -> {
                                SmartAnswerLayout(
                                    options = listOf(
                                        stringResource(R.string.quiz_true),
                                        stringResource(R.string.quiz_false)
                                    ),
                                    selectedIndex = state.selectedAnswerIndex,
                                    isAnswered = state.isAnswered,
                                    correctIndex = if (currentQuestion.correctAnswer) 0 else 1,
                                    onSelectAnswer = onSelectAnswer,
                                    explanation = currentQuestion.explanation,
                                    showExplanation = state.showExplanation,
                                    contentPadding = answerContentPadding
                                )
                            }
                        }
                    }
                }

                // ZONE D: FLOATING COMPACT ACTION BUTTONS (Floating island over scrolling cards - NO background layer!)
                AnimatedVisibility(
                    visible = showBottomActions,
                    enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = 0.78f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetY = { it }
                    ),
                    exit = fadeOut(tween(140, easing = FastOutSlowInEasing)) + slideOutVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        targetOffsetY = { it }
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    BottomActionStack(
                        isLastQuestion = state.currentQuestionIndex == state.questions.size - 1,
                        onNext = onNext,
                        canNext = state.isAnswered,
                        canPrevious = state.currentQuestionIndex - 1 in state.selectedAnswers,
                        onPrevious = onPrevious
                    )
                }
            }
        }
    }
}

@Composable
fun TopHeaderBar(
    currentQuestionIndex: Int,
    totalQuestions: Int,
    score: Int,
    elapsedTimeMillis: Long,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            StatChip(
                label = stringResource(R.string.quiz_stat_question),
                value = "${currentQuestionIndex + 1} / $totalQuestions",
                iconVector = Icons.Default.FormatListNumbered,
                modifier = Modifier.weight(1f)
            )

            StatChip(
                label = stringResource(R.string.quiz_stat_score),
                value = "$score",
                iconVector = Icons.Default.Star,
                modifier = Modifier.weight(1f),
                valueColor = MaterialTheme.colorScheme.primary
            )
            
            StatChip(
                label = stringResource(R.string.quiz_stat_time),
                value = formatTime(elapsedTimeMillis),
                iconVector = Icons.Default.Timer,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.quiz_back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
}

@Composable
fun StatChip(
    label: String,
    value: String,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = label,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    letterSpacing = 0.06.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SmoothProgressBar(
    currentQuestionIndex: Int,
    totalQuestions: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = stringResource(
                R.string.quiz_question_progress,
                currentQuestionIndex + 1,
                totalQuestions
            ),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        val progress = ((currentQuestionIndex + 1).toFloat() / totalQuestions.coerceAtLeast(1).toFloat())
            .coerceIn(0f, 1f)
        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(durationMillis = 300),
            label = "progress"
        )
        
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
fun AdaptiveQuestionCard(
    questionNumber: Int,
    questionText: String,
    media: List<com.hkm.pozix.data.model.QuestionMedia> = emptyList(),
    readOnly: Boolean = false
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(questionNumber, questionText) {
        scrollState.scrollTo(0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .animateContentSize(spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow))
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)
        ) {
            // Header Row: Question badge Qx strictly on top - NEVER overlaps question text!
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Q$questionNumber",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                if (readOnly) Text(
                    stringResource(R.string.quiz_review_read_only),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable Question Content (Math, Code, STEM Text) strictly below the header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.38f).dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(vertical = 2.dp)
                ) {
                    RichContentText(
                        text = questionText,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 25.sp
                    )
                    if (media.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        QuestionMediaContent(media = media)
                    }
                }
            }
        }
    }
}

@Composable
fun SmartAnswerLayout(
    options: List<String>,
    selectedIndex: Int?,
    isAnswered: Boolean,
    correctIndex: Int,
    onSelectAnswer: (Int) -> Unit,
    explanation: String? = null,
    showExplanation: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp)
) {
    val count = options.size
    when {
        count == 2 -> GiantButtonLayout(
            options = options,
            selectedIndex = selectedIndex,
            isAnswered = isAnswered,
            correctIndex = correctIndex,
            onSelectAnswer = onSelectAnswer,
            explanation = explanation,
            showExplanation = showExplanation,
            contentPadding = contentPadding
        )
        count in 3..5 -> LargeCardLayout(
            options = options,
            selectedIndex = selectedIndex,
            isAnswered = isAnswered,
            correctIndex = correctIndex,
            onSelectAnswer = onSelectAnswer,
            explanation = explanation,
            showExplanation = showExplanation,
            contentPadding = contentPadding
        )
        else -> CompactListLayout(
            options = options,
            selectedIndex = selectedIndex,
            isAnswered = isAnswered,
            correctIndex = correctIndex,
            onSelectAnswer = onSelectAnswer,
            explanation = explanation,
            showExplanation = showExplanation,
            contentPadding = contentPadding
        )
    }
}

@Composable
fun GiantButtonLayout(
    options: List<String>,
    selectedIndex: Int?,
    isAnswered: Boolean,
    correctIndex: Int,
    onSelectAnswer: (Int) -> Unit,
    explanation: String? = null,
    showExplanation: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp)
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.Top
        ),
        contentPadding = contentPadding
    ) {
        items(options.size) { index ->
            AnswerCard(
                text = options[index],
                letter = ('A' + index).toString(),
                isSelected = selectedIndex == index,
                isCorrect = if (isAnswered) index == correctIndex else null,
                showResult = isAnswered,
                onClick = { if (!isAnswered) onSelectAnswer(index) },
                cardSize = CardSize.LARGE,
                explanation = explanation,
                showExplanation = showExplanation
            )
        }
    }
}

@Composable
fun LargeCardLayout(
    options: List<String>,
    selectedIndex: Int?,
    isAnswered: Boolean,
    correctIndex: Int,
    onSelectAnswer: (Int) -> Unit,
    explanation: String? = null,
    showExplanation: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp)
) {
    val count = options.size
    val size = if (count <= 3) CardSize.LARGE else CardSize.NORMAL
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = contentPadding
    ) {
        items(options.size) { index ->
            AnswerCard(
                text = options[index],
                letter = ('A' + index).toString(),
                isSelected = selectedIndex == index,
                isCorrect = if (isAnswered) index == correctIndex else null,
                showResult = isAnswered,
                onClick = { if (!isAnswered) onSelectAnswer(index) },
                cardSize = size,
                explanation = explanation,
                showExplanation = showExplanation
            )
        }
    }
}

@Composable
fun CompactListLayout(
    options: List<String>,
    selectedIndex: Int?,
    isAnswered: Boolean,
    correctIndex: Int,
    onSelectAnswer: (Int) -> Unit,
    explanation: String? = null,
    showExplanation: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp)
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = contentPadding
    ) {
        items(options.size) { index ->
            AnswerCard(
                text = options[index],
                letter = ('A' + index).toString(),
                isSelected = selectedIndex == index,
                isCorrect = if (isAnswered) index == correctIndex else null,
                showResult = isAnswered,
                onClick = { if (!isAnswered) onSelectAnswer(index) },
                cardSize = CardSize.COMPACT,
                explanation = explanation,
                showExplanation = showExplanation
            )
        }
    }
}

enum class CardSize { COMPACT, NORMAL, LARGE }

@Composable
fun AnswerCard(
    text: String,
    letter: String,
    isSelected: Boolean,
    isCorrect: Boolean?,
    showResult: Boolean,
    onClick: () -> Unit,
    cardSize: CardSize = CardSize.NORMAL,
    explanation: String? = null,
    showExplanation: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(showResult, isCorrect) {
        if (showResult && isSelected && isCorrect == true) {
            scale.animateTo(1.03f, animationSpec = spring(dampingRatio = 0.5f))
            scale.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f))
        }
    }

    val bgColor: Color
    val txtColor: Color
    val borderColor: Color
    val badgeBg: Color
    val badgeTxt: Color
    val iconTint: Color
    val alpha: Float

    when {
        showResult && isSelected && isCorrect == true -> {
            bgColor = MaterialTheme.colorScheme.primaryContainer
            txtColor = MaterialTheme.colorScheme.onPrimaryContainer
            borderColor = MaterialTheme.colorScheme.primary
            badgeBg = MaterialTheme.colorScheme.primary
            badgeTxt = MaterialTheme.colorScheme.onPrimary
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer
            alpha = 1f
        }
        showResult && isSelected && isCorrect == false -> {
            bgColor = MaterialTheme.colorScheme.errorContainer
            txtColor = MaterialTheme.colorScheme.onErrorContainer
            borderColor = MaterialTheme.colorScheme.error
            badgeBg = MaterialTheme.colorScheme.error
            badgeTxt = MaterialTheme.colorScheme.onError
            iconTint = MaterialTheme.colorScheme.error
            alpha = 1f
        }
        showResult && !isSelected && isCorrect == true -> {
            bgColor = MaterialTheme.colorScheme.tertiaryContainer
            txtColor = MaterialTheme.colorScheme.onTertiaryContainer
            borderColor = MaterialTheme.colorScheme.tertiary
            badgeBg = MaterialTheme.colorScheme.tertiary
            badgeTxt = MaterialTheme.colorScheme.onTertiary
            iconTint = MaterialTheme.colorScheme.onTertiaryContainer
            alpha = 1f
        }
        showResult && !isSelected && isCorrect != true -> {
            bgColor = MaterialTheme.colorScheme.surfaceVariant
            txtColor = MaterialTheme.colorScheme.onSurfaceVariant
            borderColor = MaterialTheme.colorScheme.outline
            badgeBg = MaterialTheme.colorScheme.surfaceVariant
            badgeTxt = MaterialTheme.colorScheme.onSurfaceVariant
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant
            alpha = 0.5f
        }
        isSelected && !showResult -> {
            bgColor = MaterialTheme.colorScheme.surfaceVariant
            txtColor = MaterialTheme.colorScheme.onSurface
            borderColor = MaterialTheme.colorScheme.primary
            badgeBg = MaterialTheme.colorScheme.primary
            badgeTxt = MaterialTheme.colorScheme.onPrimary
            iconTint = MaterialTheme.colorScheme.onSurface
            alpha = 1f
        }
        else -> {
            bgColor = MaterialTheme.colorScheme.surface
            txtColor = MaterialTheme.colorScheme.onSurface
            borderColor = MaterialTheme.colorScheme.primary
            badgeBg = MaterialTheme.colorScheme.primaryContainer
            badgeTxt = MaterialTheme.colorScheme.onPrimaryContainer
            iconTint = MaterialTheme.colorScheme.onSurface
            alpha = 1f
        }
    }

    val vertPad = when (cardSize) {
        CardSize.COMPACT -> 10.dp
        CardSize.NORMAL -> 14.dp
        CardSize.LARGE -> 20.dp
    }
    val fontSize = when (cardSize) {
        CardSize.COMPACT -> 14.sp
        CardSize.NORMAL -> 15.sp
        CardSize.LARGE -> 17.sp
    }
    val lineHeight = when (cardSize) {
        CardSize.COMPACT -> 20.sp
        CardSize.NORMAL -> 22.sp
        CardSize.LARGE -> 25.sp
    }
    val badgeSize = when (cardSize) {
        CardSize.COMPACT -> 26.dp
        CardSize.NORMAL -> 30.dp
        CardSize.LARGE -> 36.dp
    }
    val badgeFontSize = when (cardSize) {
        CardSize.COMPACT -> 11.sp
        CardSize.NORMAL -> 13.sp
        CardSize.LARGE -> 15.sp
    }
    val cornerRadius = when (cardSize) {
        CardSize.COMPACT -> 12.dp
        CardSize.NORMAL -> 16.dp
        CardSize.LARGE -> 20.dp
    }

    val shouldShowExplanation = showResult && showExplanation && explanation?.isNotBlank() == true &&
            isCorrect == true

    Surface(
        onClick = onClick,
        enabled = !showResult,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .scale(scale.value),
        color = bgColor.copy(alpha = alpha),
        shape = RoundedCornerShape(cornerRadius),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor.copy(alpha = alpha))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = vertPad),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(badgeSize),
                    color = badgeBg,
                    shape = RoundedCornerShape(cornerRadius - 4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = letter,
                            color = badgeTxt,
                            fontSize = badgeFontSize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                RichContentText(
                    text = text,
                    modifier = Modifier.weight(1f),
                    textColor = txtColor,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium,
                    lineHeight = lineHeight,
                    inlineOnly = true
                )

                if (showResult) {
                    Spacer(modifier = Modifier.width(8.dp))
                    when {
                        isSelected && isCorrect == true -> Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        isSelected && isCorrect == false -> Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        !isSelected && isCorrect == true -> Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = shouldShowExplanation,
                enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = borderColor.copy(alpha = 0.3f)
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = txtColor.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        RichContentText(
                            text = explanation ?: "",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            textColor = txtColor.copy(alpha = 0.9f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomActionStack(
    isLastQuestion: Boolean,
    onNext: () -> Unit,
    canNext: Boolean = true,
    canPrevious: Boolean = false,
    onPrevious: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (canPrevious) {
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.quiz_previous_review),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
        Button(
            onClick = onNext,
            enabled = canNext,
            modifier = Modifier
                .weight(1f)
                .height(42.dp),
            shape = RoundedCornerShape(21.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
        ) {
            Text(
                text = if (isLastQuestion) stringResource(R.string.quiz_see_results)
                else stringResource(R.string.quiz_next_question),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (isLastQuestion) Icons.Default.EmojiEvents else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
