package com.hkm.pozix.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Preview
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import com.hkm.pozix.ui.components.richcontent.RichContentText
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.hkm.pozix.data.model.SavedQuizSet
import com.hkm.pozix.util.HapticUtil
import com.hkm.pozix.viewmodel.SavedQuizSetsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedQuizSetsScreen(
    onPlayQuiz: () -> Unit,
    onStartTest: () -> Unit = {},
    viewModel: SavedQuizSetsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var gridMode by rememberSaveable { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    LaunchedEffect(uiState.quizSets.firstOrNull()?.id) {
        if (uiState.quizSets.isNotEmpty()) gridState.scrollToItem(0)
    }
    
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, end = 24.dp, top = topPadding, bottom = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.saved_quiz_sets_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.saved_quiz_sets_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            uiState.quizSets.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, top = topPadding, bottom = 120.dp)
                ) {
                    Text(
                        text = stringResource(R.string.saved_quiz_sets_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.saved_quiz_sets_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.saved_quiz_sets_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.saved_quiz_sets_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            else -> {
                LazyVerticalGrid(
                    columns = if (gridMode) GridCells.Adaptive(160.dp) else GridCells.Fixed(1),
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = topPadding,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f).padding(bottom = 6.dp)) {
                            Text(
                                text = stringResource(R.string.saved_quiz_sets_title),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.saved_quiz_sets_subtitle),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                            IconButton(onClick = { gridMode = !gridMode; HapticUtil.selectionTick(context) }) {
                                Icon(if (gridMode) Icons.Default.ViewList else Icons.Default.GridView,
                                    stringResource(if (gridMode) R.string.quiz_list_view else R.string.quiz_grid_view),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    items(uiState.quizSets, key = { it.id }, span = {
                        GridItemSpan(if (!gridMode || uiState.expandedCardId == it.id) maxLineSpan else 1)
                    }) { quizSet ->
                        if (gridMode && uiState.expandedCardId != quizSet.id) {
                            CompactQuizTile(quizSet) {
                                HapticUtil.selectionTick(context)
                                viewModel.toggleExpand(quizSet.id)
                            }
                        } else PremiumQuizCard(
                            quizSet = quizSet,
                            isExpanded = uiState.expandedCardId == quizSet.id,
                            onToggleExpand = {
                                HapticUtil.ultraLightTap(context)
                                viewModel.toggleExpand(quizSet.id)
                            },
                            onPlay = {
                                HapticUtil.lightTap(context)
                                viewModel.loadQuizSet(quizSet, onPlayQuiz)
                            },
                            onStartTest = {
                                HapticUtil.lightTap(context)
                                viewModel.loadQuizSet(quizSet, onStartTest)
                            },
                            onPreview = {
                                HapticUtil.lightTap(context)
                                viewModel.showPreviewWarning(quizSet)
                            },
                            onRename = {
                                HapticUtil.ultraLightTap(context)
                                viewModel.showRenameDialog(quizSet)
                            },
                            onDelete = {
                                HapticUtil.ultraLightTap(context)
                                viewModel.showDeleteDialog(quizSet)
                            },
                            onShare = { HapticUtil.lightTap(context); viewModel.shareQuizSet(quizSet) },
                            isSharing = uiState.sharingSetId == quizSet.id,
                            shareEnabled = uiState.sharingSetId == null
                        )
                    }
                }
            }
        }
        
        if (uiState.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.hideDeleteDialog() },
                title = {
                    Text(
                        text = stringResource(R.string.saved_quiz_sets_delete_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Text(
                        text = stringResource(
                            R.string.saved_quiz_sets_delete_message,
                            uiState.quizSetToDelete?.name ?: ""
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            HapticUtil.ultraLightTap(context)
                            viewModel.deleteQuizSet()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(
                            stringResource(R.string.saved_quiz_sets_delete_confirm),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            HapticUtil.ultraLightTap(context)
                            viewModel.hideDeleteDialog()
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (uiState.shareUrl.isNotBlank() || uiState.shareError.isNotBlank()) {
            AlertDialog(
                onDismissRequest = viewModel::dismissShare,
                title = {
                    Text(
                        if (uiState.shareUrl.isNotBlank()) {
                            stringResource(R.string.saved_quiz_sets_share_title)
                        } else {
                            stringResource(R.string.saved_quiz_sets_share_error_title)
                        }
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(uiState.shareUrl.ifBlank { uiState.shareError })
                        }
                        if (uiState.shareUrl.isNotBlank()) {
                            Text(
                                stringResource(R.string.saved_quiz_sets_share_expiry),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = { Button(onClick = {
                    if (uiState.shareUrl.isNotBlank()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                context.getString(R.string.saved_quiz_sets_share_title),
                                uiState.shareUrl
                            )
                        )
                    }
                    viewModel.dismissShare()
                }) {
                    Text(
                        if (uiState.shareUrl.isNotBlank()) {
                            stringResource(R.string.saved_quiz_sets_copy_link)
                        } else {
                            stringResource(R.string.ok)
                        }
                    )
                } }
            )
        }
        
        if (uiState.showRenameDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.hideRenameDialog() },
                title = {
                    Text(
                        text = stringResource(R.string.saved_quiz_sets_rename_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    OutlinedTextField(
                        value = uiState.renameText,
                        onValueChange = { viewModel.updateRenameText(it) },
                        label = { Text(stringResource(R.string.saved_quiz_sets_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            HapticUtil.ultraLightTap(context)
                            viewModel.renameQuizSet()
                        },
                        enabled = uiState.renameText.trim().isNotEmpty()
                    ) {
                        Text(
                            stringResource(R.string.saved_quiz_sets_rename_confirm),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            HapticUtil.ultraLightTap(context)
                            viewModel.hideRenameDialog()
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (uiState.showPreviewWarning) {
            AlertDialog(
                onDismissRequest = { viewModel.hidePreviewWarning() },
                title = {
                    Text(
                        text = stringResource(R.string.preview_warning_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.preview_warning_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            HapticUtil.lightTap(context)
                            viewModel.confirmPreview()
                        }
                    ) {
                        Text(
                            stringResource(R.string.preview_warning_confirm),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            HapticUtil.ultraLightTap(context)
                            viewModel.hidePreviewWarning()
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (uiState.showPreview) {
            QuizPreviewDialog(
                title = uiState.previewTitle,
                description = uiState.previewDescription,
                questions = uiState.previewQuestions,
                onDismiss = { viewModel.hidePreview() }
            )
        }
    }
}

@Composable
fun PremiumQuizCard(
    quizSet: SavedQuizSet,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPlay: () -> Unit,
    onStartTest: () -> Unit,
    onPreview: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    isSharing: Boolean,
    shareEnabled: Boolean
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "rotation"
    )
    
    val progressState = when {
        quizSet.isCompleted -> stringResource(R.string.saved_quiz_sets_completed)
        quizSet.progressPercentage > 0f -> stringResource(R.string.saved_quiz_sets_in_progress)
        else -> stringResource(R.string.saved_quiz_sets_not_started)
    }
    
    val progressFillColor = MaterialTheme.colorScheme.primary
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        tonalElevation = 2.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onToggleExpand,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = quizSet.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (quizSet.isCompleted) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = stringResource(R.string.saved_quiz_sets_completed),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.saved_quiz_sets_question_count,
                                    quizSet.questionCount
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(
                                    R.string.saved_quiz_sets_type_count,
                                    quizSet.singleChoiceCount,
                                    quizSet.trueFalseCount
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.rotate(rotation)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = progressState,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${quizSet.progressPercentage.coerceAtMost(100f).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((quizSet.progressPercentage / 100f).coerceIn(0f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(progressFillColor)
                        )
                    }
                }
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                ) {
                    if (quizSet.description.isNotEmpty()) {
                        Text(
                            text = quizSet.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    if (quizSet.lastUsedTimestamp > 0) {
                        Text(
                            text = stringResource(
                                R.string.saved_quiz_sets_last_played,
                                formatTimestamp(quizSet.lastUsedTimestamp)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onPlay,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(21.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    when {
                                        quizSet.isCompleted -> stringResource(R.string.saved_quiz_sets_play_again)
                                        quizSet.hasInProgressSession -> stringResource(R.string.saved_quiz_sets_resume)
                                        else -> stringResource(R.string.saved_quiz_sets_quiz)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = onStartTest,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    modifier = Modifier.size(21.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.exam_test_mode),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuizCardAction(
                                icon = Icons.Default.Preview,
                                label = stringResource(R.string.preview_button),
                                onClick = onPreview,
                                modifier = Modifier.weight(1f)
                            )
                            QuizCardAction(
                                icon = Icons.Default.Share,
                                label = if (isSharing) {
                                    stringResource(R.string.saved_quiz_sets_sharing)
                                } else {
                                    stringResource(R.string.saved_quiz_sets_share)
                                },
                                onClick = onShare,
                                enabled = shareEnabled,
                                loading = isSharing,
                                modifier = Modifier.weight(1f)
                            )
                            QuizCardAction(
                                icon = Icons.Default.Edit,
                                label = stringResource(R.string.saved_quiz_sets_rename),
                                onClick = onRename,
                                modifier = Modifier.weight(1f)
                            )
                            QuizCardAction(
                                icon = Icons.Default.Delete,
                                label = stringResource(R.string.saved_quiz_sets_delete),
                                onClick = onDelete,
                                destructive = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizCardAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    destructive: Boolean = false
) {
    val contentColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (destructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(19.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun QuizPreviewDialog(
    title: String,
    description: String,
    questions: List<Question>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.close),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.preview_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    questions.forEachIndexed { index, question ->
                        PreviewQuestionItem(
                            index = index + 1,
                            question = question
                        )
                        if (index < questions.lastIndex) {
                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun PreviewQuestionItem(index: Int, question: Question) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.wrapContentHeight(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
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
                Spacer(modifier = Modifier.width(12.dp))
                RichContentText(
                    text = question.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (question) {
            is Question.SingleChoice -> {
                question.options.forEachIndexed { optIndex, option ->
                    val isCorrect = optIndex == question.correctIndex
                    PreviewOptionItem(
                        letter = ('A' + optIndex).toString(),
                        text = option,
                        isCorrect = isCorrect
                    )
                    if (optIndex < question.options.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
            is Question.TrueFalse -> {
                PreviewOptionItem(
                    letter = "A",
                    text = stringResource(R.string.quiz_true),
                    isCorrect = question.correctAnswer == true
                )
                Spacer(modifier = Modifier.height(6.dp))
                PreviewOptionItem(
                    letter = "B",
                    text = stringResource(R.string.quiz_false),
                    isCorrect = question.correctAnswer == false
                )
            }
        }

        if (question.explanation?.isNotBlank() == true) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.quiz_explanation),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        RichContentText(
                            text = question.explanation ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewOptionItem(letter: String, text: String, isCorrect: Boolean) {
    val backgroundColor = if (isCorrect) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val textColor = if (isCorrect) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val badgeColor = if (isCorrect) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val badgeTextColor = if (isCorrect) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = if (isCorrect) {
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(24.dp),
                color = badgeColor,
                shape = RoundedCornerShape(6.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        color = badgeTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            RichContentText(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                textColor = textColor,
                fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                inlineOnly = true
            )
            if (isCorrect) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.quiz_correct),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
