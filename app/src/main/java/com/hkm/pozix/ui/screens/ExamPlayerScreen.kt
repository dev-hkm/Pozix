package com.hkm.pozix.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import com.hkm.pozix.ui.components.richcontent.RichContentText
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.hkm.pozix.util.HapticUtil
import com.hkm.pozix.viewmodel.ExamConfig
import com.hkm.pozix.viewmodel.ExamState
import com.hkm.pozix.viewmodel.ExamViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun ExamPlayerScreen(
    onBack: () -> Unit,
    viewModel: ExamViewModel = viewModel()
) {
    val examState by viewModel.examState.collectAsState()
    val currentState = examState

    BackHandler {
        if (currentState is ExamState.Playing) {
            viewModel.requestExit()
        } else {
            onBack()
        }
    }

    AnimatedContent(
        targetState = currentState,
        transitionSpec = {
            fadeIn(tween(350)) togetherWith fadeOut(tween(220))
        },
        contentKey = { it::class },
        modifier = Modifier.fillMaxSize(),
        label = "examState"
    ) { state ->
        when (state) {
            is ExamState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val infiniteTransition = rememberInfiniteTransition(label = "loading")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "loadingAlpha"
                    )
                    Text(
                        text = stringResource(R.string.loading),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                    )
                }
            }
            is ExamState.Setup -> {
                ExamSetupContent(
                    state = state,
                    onStart = { viewModel.startExam() },
                    onBack = onBack,
                    onConfigChange = { viewModel.updateConfig(it) }
                )
            }
            is ExamState.Playing -> {
                ExamPlayingContent(
                    state = state,
                    onSelectAnswer = { viewModel.selectAnswer(it) },
                    onClearAnswer = viewModel::clearAnswer,
                    onToggleFlag = viewModel::toggleFlag,
                    onNext = { viewModel.nextQuestion() },
                    onPrevious = { viewModel.previousQuestion() },
                    onGoTo = { viewModel.goToQuestion(it) },
                    onTogglePalette = { viewModel.togglePalette() },
                    onSubmit = { viewModel.requestSubmit() },
                    onConfirmSubmit = { viewModel.confirmSubmit() },
                    onCancelSubmit = { viewModel.cancelSubmit() },
                    onCancelExit = { viewModel.cancelExit() },
                    onSaveAndExit = { viewModel.saveAndExit(onBack) },
                    onContinueSaved = { viewModel.continueSavedExam() },
                    onRestartSaved = { viewModel.restartSavedExam() }
                )
            }
            is ExamState.Finished -> {
                ExamResultScreen(
                    state = state,
                    onRetry = { viewModel.restart() },
                    onBack = onBack
                )
            }
            is ExamState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.error_loading_quiz),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ExamSetupContent(
    state: ExamState.Setup,
    onStart: () -> Unit,
    onBack: () -> Unit,
    onConfigChange: (ExamConfig) -> Unit
) {
    val config = state.config
    val context = LocalContext.current
    var sliderValue by remember(config.timeLimitMinutes) { mutableFloatStateOf(config.timeLimitMinutes.toFloat()) }
    var lastHapticValue by remember { mutableStateOf(config.timeLimitMinutes) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.quiz_back)
                )
            }
            Text(
                text = stringResource(R.string.exam_setup_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = state.quizTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.exam_setup_summary,
                        state.questionCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "${stringResource(R.string.exam_time_limit)}: ${sliderValue.roundToInt()} min",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(5, 10, 20, 30, 45, 60)) { minutes ->
                FilterChip(
                    selected = sliderValue.roundToInt() == minutes,
                    onClick = {
                        sliderValue = minutes.toFloat()
                        onConfigChange(config.copy(timeLimitMinutes = minutes))
                        HapticUtil.ultraLightTap(context)
                    },
                    label = { Text(stringResource(R.string.exam_minutes, minutes)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                sliderValue = newValue
                val rounded = newValue.roundToInt()
                if (rounded != lastHapticValue) {
                    lastHapticValue = rounded
                    HapticUtil.ultraLightTap(context)
                }
            },
            onValueChangeFinished = {
                onConfigChange(config.copy(timeLimitMinutes = sliderValue.roundToInt()))
            },
            valueRange = 5f..180f,
            steps = 174,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Text(
                    stringResource(R.string.exam_minutes, 5),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.exam_three_hours),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                ExamToggleRow(
                    label = stringResource(R.string.exam_shuffle_questions),
                    checked = config.shuffleQuestions,
                    onToggle = { onConfigChange(config.copy(shuffleQuestions = !config.shuffleQuestions)) }
                )
                ExamToggleRow(
                    label = stringResource(R.string.exam_shuffle_answers),
                    checked = config.shuffleAnswers,
                    onToggle = { onConfigChange(config.copy(shuffleAnswers = !config.shuffleAnswers)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                HapticUtil.lightTap(context)
                onStart()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(imageVector = Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.exam_start),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ExamToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                HapticUtil.ultraLightTap(context)
                onToggle()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = {
                HapticUtil.ultraLightTap(context)
                onToggle()
            }
        )
    }
}

@Composable
fun ExamPlayingContent(
    state: ExamState.Playing,
    onSelectAnswer: (Int) -> Unit,
    onClearAnswer: () -> Unit,
    onToggleFlag: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onGoTo: (Int) -> Unit,
    onTogglePalette: () -> Unit,
    onSubmit: () -> Unit,
    onConfirmSubmit: () -> Unit,
    onCancelSubmit: () -> Unit,
    onCancelExit: () -> Unit,
    onSaveAndExit: () -> Unit,
    onContinueSaved: () -> Unit,
    onRestartSaved: () -> Unit
) {
    val context = LocalContext.current
    val totalQuestions = state.questions.size
    val answeredCount = state.answers.size
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)
    val questionScroll = rememberScrollState()
    LaunchedEffect(state.currentIndex) {
        questionScroll.scrollTo(0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            ExamTimerBar(
                remainingMillis = state.remainingMillis,
                totalMillis = state.timeLimitMillis,
                isWarning = state.isTimeWarning
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Q${state.currentIndex + 1}/$totalQuestions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$answeredCount/$totalQuestions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                HapticUtil.lightTap(context)
                                onTogglePalette()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = stringResource(R.string.exam_palette),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { answeredCount.toFloat() / totalQuestions.coerceAtLeast(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.currentIndex in state.flaggedQuestions,
                    onClick = {
                        HapticUtil.ultraLightTap(context)
                        onToggleFlag()
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.exam_flag_review),
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                TextButton(
                    onClick = {
                        HapticUtil.ultraLightTap(context)
                        onClearAnswer()
                    },
                    enabled = state.currentIndex in state.answers
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.exam_clear_answer))
                }
            }

            // One persistent composition: no overlapping WebViews/font runtimes during a transition.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val safeIndex = state.currentIndex.coerceIn(state.questions.indices)
                val question = state.questions[safeIndex]
                val options = when (question) {
                    is Question.SingleChoice -> question.options
                    is Question.TrueFalse -> listOf(
                        stringResource(R.string.quiz_true),
                        stringResource(R.string.quiz_false)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var accumulatedDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { accumulatedDrag = 0f },
                                onDragEnd = {
                                    if (accumulatedDrag > 120f) currentOnPrevious()
                                    else if (accumulatedDrag < -120f) currentOnNext()
                                    accumulatedDrag = 0f
                                },
                                onDragCancel = { accumulatedDrag = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    accumulatedDrag += dragAmount
                                }
                            )
                        }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .animateContentSize()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(questionScroll)
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Q${safeIndex + 1} / $totalQuestions",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (safeIndex in state.flaggedQuestions) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Flag,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = stringResource(R.string.exam_flag_review),
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            RichContentText(
                                text = question.question,
                                textColor = MaterialTheme.colorScheme.onSurface,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 25.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        ExamAnswerLayout(
                            options = options,
                            selectedIndex = state.answers[safeIndex],
                            onSelectAnswer = onSelectAnswer
                        )
                    }
                }
            }

            ExamNavigationBar(
                currentIndex = state.currentIndex,
                totalQuestions = totalQuestions,
                onPrevious = {
                    HapticUtil.lightTap(context)
                    onPrevious()
                },
                onNext = {
                    HapticUtil.lightTap(context)
                    onNext()
                },
                onSubmit = {
                    HapticUtil.lightTap(context)
                    onSubmit()
                }
            )
        }

        AnimatedVisibility(
            visible = state.showPalette,
            enter = fadeIn(tween(200)) + slideInVertically(tween(300)) { it / 2 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { it / 2 }
        ) {
            ExamQuestionPalette(
                questions = state.questions,
                answers = state.answers,
                flaggedQuestions = state.flaggedQuestions,
                currentIndex = state.currentIndex,
                onGoTo = {
                    HapticUtil.ultraLightTap(context)
                    onGoTo(it)
                    onTogglePalette()
                },
                onDismiss = onTogglePalette
            )
        }

        if (state.showSubmitConfirm) {
            val total = state.questions.size
            val answered = state.answers.size
            val unanswered = total - answered
            val flagged = state.flaggedQuestions.size

            AlertDialog(
                onDismissRequest = {
                    HapticUtil.selectionTick(context)
                    onCancelSubmit()
                },
                icon = {
                    Icon(
                        imageVector = if (unanswered > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (unanswered > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.exam_submit_confirm_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.exam_submit_confirm_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$answered/$total",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = stringResource(R.string.exam_palette_answered),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                color = if (unanswered > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$unanswered",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (unanswered > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.exam_palette_unanswered),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (unanswered > 0) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$flagged",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = stringResource(R.string.exam_palette_flagged),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (unanswered > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.exam_unanswered_warning, unanswered),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            HapticUtil.confirm(context)
                            onConfirmSubmit()
                        }
                    ) {
                        Text(stringResource(R.string.exam_submit), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            HapticUtil.selectionTick(context)
                            onCancelSubmit()
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (state.showExitConfirm) {
            AlertDialog(
                onDismissRequest = onCancelExit,
                title = { Text(stringResource(R.string.exam_save_exit_title)) },
                text = { Text(stringResource(R.string.exam_save_exit_message)) },
                confirmButton = {
                    Button(onClick = onSaveAndExit) {
                        Text(
                            stringResource(R.string.exam_save_exit_action),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = onCancelExit) {
                        Text(stringResource(R.string.exam_keep_taking))
                    }
                }
            )
        }

        if (state.showResumeConfirm) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.exam_resume_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.exam_resume_message,
                            state.answers.size
                        )
                    )
                },
                confirmButton = {
                    Button(onClick = onContinueSaved) {
                        Text(
                            stringResource(R.string.exam_resume_action),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = onRestartSaved) {
                        Text(stringResource(R.string.exam_restart_action))
                    }
                }
            )
        }
    }
}

@Composable
fun ExamTimerBar(remainingMillis: Long, totalMillis: Long, isWarning: Boolean) {
    val minutes = (remainingMillis / 1000) / 60
    val seconds = (remainingMillis / 1000) % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)
    val progress = (remainingMillis.toFloat() / totalMillis.coerceAtLeast(1)).coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = if (isWarning) MaterialTheme.colorScheme.errorContainer.copy(alpha = pulseAlpha)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                border = if (isWarning) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timeText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )
                    if (isWarning) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
fun ExamAnswerLayout(
    options: List<String>,
    selectedIndex: Int?,
    onSelectAnswer: (Int) -> Unit
) {
    val count = options.size
    val cardSize = when {
        count <= 3 -> CardSize.LARGE
        count <= 5 -> CardSize.NORMAL
        else -> CardSize.COMPACT
    }
    val spacing = if (count <= 3) 12.dp else 8.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(space = spacing),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(options.size) { index ->
            ExamAnswerCard(
                text = options[index],
                letter = ('A' + index).toString(),
                isSelected = selectedIndex == index,
                cardSize = cardSize,
                onClick = { onSelectAnswer(index) }
            )
        }
    }
}

@Composable
fun ExamAnswerCard(
    text: String,
    letter: String,
    isSelected: Boolean,
    cardSize: CardSize,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "cardScale"
    )

    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val badgeBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    val badgeTxt = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer

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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .scale(scale)
            .clickable {
                HapticUtil.answerSelected(context)
                onClick()
            },
        color = bgColor,
        shape = RoundedCornerShape(cornerRadius),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
    ) {
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
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            RichContentText(
                text = text,
                modifier = Modifier.weight(1f),
                textColor = textColor,
                fontSize = fontSize,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                lineHeight = lineHeight,
                inlineOnly = true
            )
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(tween(250, easing = LinearEasing)) + fadeIn(tween(250)),
                exit = scaleOut(tween(150)) + fadeOut(tween(150))
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun ExamNavigationBar(
    currentIndex: Int,
    totalQuestions: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = currentIndex > 0,
            modifier = Modifier
                .size(48.dp)
                .border(
                    1.dp,
                    if (currentIndex > 0) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(14.dp)
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.exam_previous),
                tint = if (currentIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(text = stringResource(R.string.exam_submit), fontWeight = FontWeight.Bold)
        }

        IconButton(
            onClick = onNext,
            enabled = currentIndex < totalQuestions - 1,
            modifier = Modifier
                .size(48.dp)
                .border(
                    1.dp,
                    if (currentIndex < totalQuestions - 1) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(14.dp)
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.exam_next),
                tint = if (currentIndex < totalQuestions - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun ExamQuestionPalette(
    questions: List<Question>,
    answers: Map<Int, Int>,
    flaggedQuestions: Set<Int>,
    currentIndex: Int,
    onGoTo: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Unanswered, 2: Flagged

    val unansweredCount = questions.indices.count { it !in answers }
    val flaggedCount = flaggedQuestions.size

    val displayedIndices = remember(selectedFilter, questions.size, answers, flaggedQuestions) {
        when (selectedFilter) {
            1 -> questions.indices.filter { it !in answers }
            2 -> questions.indices.filter { it in flaggedQuestions }
            else -> questions.indices.toList()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.exam_palette_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            HapticUtil.selectionTick(context)
                            onDismiss()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == 0,
                        onClick = {
                            HapticUtil.selectionTick(context)
                            selectedFilter = 0
                        },
                        label = { Text("${stringResource(R.string.exam_palette_all)} (${questions.size})", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedFilter == 1,
                        onClick = {
                            HapticUtil.selectionTick(context)
                            selectedFilter = 1
                        },
                        label = { Text("${stringResource(R.string.exam_palette_unanswered)} ($unansweredCount)", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedFilter == 2,
                        onClick = {
                            HapticUtil.selectionTick(context)
                            selectedFilter = 2
                        },
                        label = { Text("${stringResource(R.string.exam_palette_flagged)} ($flaggedCount)", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        PaletteLegend(
                            MaterialTheme.colorScheme.primary,
                            stringResource(R.string.exam_palette_current)
                        )
                    }
                    item {
                        PaletteLegend(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            stringResource(R.string.exam_palette_flagged)
                        )
                    }
                    item {
                        PaletteLegend(
                            MaterialTheme.colorScheme.primaryContainer,
                            stringResource(R.string.exam_palette_answered)
                        )
                    }
                    item {
                        PaletteLegend(
                            MaterialTheme.colorScheme.surfaceVariant,
                            stringResource(R.string.exam_palette_unanswered)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (displayedIndices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.exam_palette_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 320.dp)
                    ) {
                        items(displayedIndices.size) { i ->
                            val index = displayedIndices[i]
                            val isAnswered = index in answers
                            val isCurrent = index == currentIndex
                            val isFlagged = index in flaggedQuestions

                            val bgColor = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                isFlagged -> MaterialTheme.colorScheme.tertiaryContainer
                                isAnswered -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val txtColor = when {
                                isCurrent -> MaterialTheme.colorScheme.onPrimary
                                isFlagged -> MaterialTheme.colorScheme.onTertiaryContainer
                                isAnswered -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Surface(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clickable {
                                        HapticUtil.navigationChange(context)
                                        onGoTo(index)
                                    },
                                color = bgColor,
                                shape = RoundedCornerShape(12.dp),
                                border = if (isCurrent) androidx.compose.foundation.BorderStroke(
                                    2.dp, MaterialTheme.colorScheme.primary
                                ) else null,
                                shadowElevation = if (isCurrent) 4.dp else 0.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = txtColor
                                    )
                                    if (isFlagged) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = null,
                                            tint = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(11.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(12.dp), color = color, shape = RoundedCornerShape(3.dp)) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
