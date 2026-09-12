package com.hkm.pozix.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkm.pozix.R
import com.hkm.pozix.data.model.AttachmentType
import com.hkm.pozix.data.model.ChatAttachment
import com.hkm.pozix.data.model.ChatMessage
import com.hkm.pozix.data.model.ChatSession
import com.hkm.pozix.data.model.QuizValidationResult
import com.hkm.pozix.ui.components.BouncyContainer
import com.hkm.pozix.ui.components.PozixModalBottomSheet
import com.hkm.pozix.ui.components.richcontent.RichContentText
import androidx.compose.foundation.text.selection.SelectionContainer
import com.hkm.pozix.util.HapticUtil
import com.hkm.pozix.util.QuizJsonParser
import com.hkm.pozix.viewmodel.AIChatUiState
import com.hkm.pozix.viewmodel.AIChatViewModel
import com.hkm.pozix.viewmodel.ImportStatus
import com.hkm.pozix.viewmodel.AiPhase
import com.hkm.pozix.util.AiQuizOutput
import com.hkm.pozix.ui.components.richcontent.StreamingRichContentText
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onPlayQuiz: () -> Unit = {},
    viewModel: AIChatViewModel = viewModel(),
    initialPrompt: String? = null,
    showBackButton: Boolean = true,
    onOpenTemplates: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val renderEntries = com.hkm.pozix.ui.components.richcontent.rememberChatEntries(uiState.messages, uiState.isLoading)
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var showProviderPicker by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    LaunchedEffect(drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Open) keyboardController?.hide()
    }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showAttachmentTray by remember { mutableStateOf(false) }
    var reasoningEffort by remember { mutableStateOf("off") }
    var showReasoningSheet by remember { mutableStateOf(false) }
    var previewImageFilePath by remember { mutableStateOf<String?>(null) }
    var textInput by remember { mutableStateOf("") }
    val pendingReviewText by com.hkm.pozix.util.QuizAiFollowUp.pending.collectAsState()
    LaunchedEffect(pendingReviewText) {
        pendingReviewText?.let { viewModel.prepareQuizReview(it) }
    }
    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            textInput = initialPrompt
        }
    }

    // Launchers for media and files (supporting multiple photos and documents)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            HapticUtil.lightTap(context)
            viewModel.attachMultipleUris(uris, isExplicitImage = true)
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            HapticUtil.lightTap(context)
            viewModel.attachMultipleUris(uris, isExplicitImage = false)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            HapticUtil.lightTap(context)
            scope.launch {
                try {
                    val cacheDir = File(context.filesDir, "ai_chat_attachments").apply { if (!exists()) mkdirs() }
                    val photoFile = File(cacheDir, "cam_${UUID.randomUUID()}.jpg")
                    FileOutputStream(photoFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    viewModel.attachUri(Uri.fromFile(photoFile), isExplicitImage = true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Refresh active provider when screen loads
    LaunchedEffect(Unit) { viewModel.refreshProviders() }

    // Handle toast messages on import status
    LaunchedEffect(uiState.importStatus) {
        when (val status = uiState.importStatus) {
            is ImportStatus.Success -> {
                Toast.makeText(context, context.getString(R.string.ai_chat_import_success), Toast.LENGTH_SHORT).show()
                viewModel.clearImportStatus()
            }
            is ImportStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                viewModel.clearImportStatus()
            }
            ImportStatus.Idle -> {}
        }
    }

    // Smart auto-scroll: respects user gesture, never jerks screen or fights manual scroll
    val lastMessageTextLength = uiState.messages.lastOrNull()?.text?.length ?: 0
    val lastMessageReasoningLength = uiState.messages.lastOrNull()?.reasoning?.length ?: 0
    var userScrolledUp by remember { mutableStateOf(false) }

    // Accurately detect whether the user is genuinely at the bottom edge of the last item
    val isScrolledToBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visible = layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) return@derivedStateOf true
            val lastVisible = visible.last()
            val total = layoutInfo.totalItemsCount
            if (lastVisible.index < total - 1) {
                false
            } else {
                val itemBottom = lastVisible.offset + lastVisible.size
                val viewportBottom = layoutInfo.viewportEndOffset
                itemBottom + layoutInfo.afterContentPadding <= viewportBottom + 40
            }
        }
    }

    // Reset userScrolledUp whenever a new message is posted
    var prevMessageCount by remember { mutableIntStateOf(uiState.messages.size) }
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.size > prevMessageCount) {
            userScrolledUp = false
        }
        prevMessageCount = uiState.messages.size
    }

    // While user is dragging or has scrolled away from bottom, suspend auto-scroll completely
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isUserDragging, isScrolledToBottom) {
        if (isUserDragging) {
            userScrolledUp = true
        } else if (isScrolledToBottom) {
            userScrolledUp = false
        }
    }

    val followTail by rememberUpdatedState(!userScrolledUp)
    LaunchedEffect(uiState.currentSessionId) {
        snapshotFlow {
            listState.layoutInfo.let { layout ->
                val last = layout.visibleItemsInfo.lastOrNull()
                if (last != null && last.index == layout.totalItemsCount - 1)
                    (last.offset + last.size + layout.afterContentPadding - layout.viewportEndOffset).coerceAtLeast(0)
                else 0
            }
        }.distinctUntilChanged().debounce(48L).collect { overflow ->
            if (overflow > 0 && followTail && !listState.isScrollInProgress) {
                listState.scroll { scrollBy(overflow.toFloat()) }
            }
        }
    }

    LaunchedEffect(renderEntries.size, renderEntries.lastOrNull()?.message?.text?.length) {
        withFrameNanos { }
        if (followTail && !listState.isScrollInProgress && renderEntries.isNotEmpty()) {
            listState.scrollToItem((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
        }
    }

    // A quiz card is inserted asynchronously after validation, and may grow after streaming stops.
    // Re-check actual layout every frame briefly, including the clearance beneath the floating composer.
    LaunchedEffect(uiState.messages.lastOrNull()?.quizJson) {
        if (uiState.messages.lastOrNull()?.quizJson != null) {
            repeat(90) {
                withFrameNanos { }
                if (!followTail || isUserDragging) return@LaunchedEffect
                if (!listState.isScrollInProgress && listState.canScrollForward) {
                    val layout = listState.layoutInfo
                    val last = layout.visibleItemsInfo.lastOrNull()
                    if (last != null && last.index == layout.totalItemsCount - 1) {
                        val distance = last.offset + last.size + layout.afterContentPadding - layout.viewportEndOffset
                        if (distance > 0) listState.scroll { scrollBy(distance.toFloat()) }
                    } else if (layout.totalItemsCount > 0) listState.scrollToItem(layout.totalItemsCount - 1)
                }
            }
        }
    }

    androidx.activity.compose.BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !showProviderPicker && !showReasoningSheet,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.86f).widthIn(max = 360.dp),
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
            ) {
                ChatSidebar(
                    sessions = uiState.sessions, currentId = uiState.currentSessionId,
                    onSelect = { id ->
                        HapticUtil.lightTap(context)
                        viewModel.loadSession(id)
                        textInput = ""
                        showAttachmentTray = false
                        scope.launch { drawerState.close() }
                    },
                    onTemplates = {
                        scope.launch {
                            drawerState.close()
                            onOpenTemplates?.invoke()
                        }
                    },
                    onProviders = {
                        scope.launch { drawerState.close(); showProviderPicker = true }
                    },
                    onPin = { viewModel.toggleSessionPinned(it) },
                    onRename = { id, title -> viewModel.renameSession(id, title) },
                    onDelete = { viewModel.deleteSession(it) }
                )
            }
        }
    ) {
    // Root layout using imePadding to guarantee input bar is NEVER hidden by the keyboard
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
        ) {
            // LAYER 1: Chat Message Stream or Setup Card (Spans full height under floating header)
            if (uiState.activeProvider == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 90.dp, bottom = 130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ProviderSetupCard(
                        onNavigateToSettings = { showProviderPicker = true },
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                AnimatedContent(
                    targetState = Pair(uiState.currentSessionId, uiState.messages.isEmpty()),
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(320)) + scaleIn(initialScale = 0.97f, animationSpec = tween(320)))
                            .togetherWith(fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.98f, animationSpec = tween(180)))
                    },
                    label = "chatContentTransition",
                    modifier = Modifier.fillMaxSize()
                ) { (_, isEmpty) ->
                    if (isEmpty) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(
                                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 62.dp,
                                bottom = 140.dp
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                ChatWelcomeSection(
                                    onSuggestionClick = { suggestion ->
                                        HapticUtil.lightTap(context)
                                        textInput = suggestion
                                    },
                                    onAttachClick = { showAttachmentTray = true }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(
                                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 62.dp,
                                bottom = 150.dp
                            )
                        ) {
                            items(renderEntries, key = { it.key },
                                contentType = { it.block?.javaClass?.simpleName ?: it.section }) { entry ->
                                if (entry.block != null) {
                                    RichContentText(
                                        text = "block",
                                        blocksOverride = remember(entry.block) { listOf(entry.block) },
                                        fontSize = 15.sp, lineHeight = 23.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    ChatBubbleItem(
                                        message = entry.message, isStreaming = entry.streaming,
                                        section = entry.section, phase = uiState.phase,
                                        onImageClick = { previewImageFilePath = it },
                                        onImportPlay = { viewModel.importQuizSet(it, onPlayQuiz) },
                                        onSaveLibrary = { viewModel.saveQuizSetOnly(it) }
                                    )
                                }
                            }

                            // Show thinking indicator with entrance animation while waiting for first token
                            if (uiState.isLoading && uiState.messages.lastOrNull()?.role != "model") {
                                item(key = "typing_indicator") {
                                    Box(
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = tween(250),
                                            fadeOutSpec = tween(200),
                                            placementSpec = spring(dampingRatio = 0.8f)
                                        )
                                    ) {
                                        AiPhaseLine(uiState.phase)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // LAYER 2: TOP TRANSLUCENT GRADIENT & FLOATING CHATGPT-STYLE HEADER (media_1789121761075.png)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                // Soft Translucent Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 74.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.80f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Top Controls Floating Row (Elevated glass pills & circle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Left: Floating Circular Button (Back or History)
                    Surface(
                        onClick = {
                            HapticUtil.lightTap(context)
                            keyboardController?.hide()
                            scope.launch { drawerState.open() }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.ai_chat_history),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = {
                            HapticUtil.lightTap(context)
                            viewModel.startNewChat()
                            textInput = ""
                            showAttachmentTray = false
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
                        )
                    ) {
                        Icon(NewChatIcon, stringResource(R.string.ai_chat_new_chat))
                    }
                }
            }

            // LAYER 3: FLOATING SCROLL-TO-BOTTOM FAB (Centered above input island, exactly matching ChatGPT!)
            androidx.compose.animation.AnimatedVisibility(
                visible = userScrolledUp,
                enter = fadeIn(tween(200)) + scaleIn(spring(dampingRatio = 0.7f)),
                exit = fadeOut(tween(150)) + scaleOut(tween(150)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 145.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        HapticUtil.lightTap(context)
                        userScrolledUp = false
                        scope.launch {
                            if (uiState.messages.isNotEmpty()) {
                                listState.animateScrollToItem((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0), scrollOffset = 100000)
                            }
                        }
                    },
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = stringResource(R.string.ai_chat_scroll_bottom),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // LAYER 4: DeepSeek SIGNATURE FLOATING ISLAND INPUT CARD
            DeepSeekStyleFloatingInputCard(
                modifier = Modifier.align(Alignment.BottomCenter),
                showIdleGlow = uiState.messages.isEmpty(),
                textInput = textInput,
                onTextChanged = { textInput = it },
                isLoading = uiState.isLoading,
                isAttaching = uiState.isAttaching,
                pendingAttachments = uiState.pendingAttachments,
                reasoningEffort = reasoningEffort,
                pendingReview = pendingReviewText,
                canSendReview = uiState.activeProvider != null && !uiState.isLoading && !uiState.isAttaching,
                onSendReview = {
                    pendingReviewText?.let { review ->
                        viewModel.sendQuizReview(review, reasoningEffort, textInput)
                        textInput = ""
                    }
                },
                onDismissReview = { com.hkm.pozix.util.QuizAiFollowUp.clear(context) },
                onOpenReasoningSelector = {
                    HapticUtil.lightTap(context)
                    showReasoningSheet = true
                },
                quizToolEnabled = uiState.quizToolEnabled,
                onToggleQuizTool = {
                    HapticUtil.lightTap(context)
                    viewModel.toggleQuizTool()
                },
                showAttachmentTray = showAttachmentTray,
                onToggleAttachmentTray = {
                    HapticUtil.lightTap(context)
                    showAttachmentTray = !showAttachmentTray
                },
                onPickGallery = {
                    showAttachmentTray = false
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onTakePhoto = {
                    showAttachmentTray = false
                    cameraLauncher.launch(null)
                },
                onPickDocument = {
                    showAttachmentTray = false
                    documentPickerLauncher.launch("*/*")
                },
                onRemoveAttachment = { viewModel.removePendingAttachment(it) },
                onPreviewImage = { previewImageFilePath = it },
                onSend = {
                    if (textInput.isNotBlank() || uiState.pendingAttachments.isNotEmpty()) {
                        HapticUtil.lightTap(context)
                        userScrolledUp = false
                        val isReasoningActive = reasoningEffort != "off"
                        val promptText = if (isReasoningActive && textInput.isNotBlank() && !textInput.lowercase().contains("suy nghĩ") && !textInput.lowercase().contains("think")) {
                            "${context.getString(R.string.ai_chat_think_desc)}:\n${textInput.trim()}"
                        } else {
                            textInput.trim()
                        }
                        val review = pendingReviewText
                        if (review != null) viewModel.sendQuizReview(review, reasoningEffort, promptText)
                        else viewModel.sendMessage(promptText, reasoningEffort)
                        textInput = ""
                        keyboardController?.hide()
                        showAttachmentTray = false
                    }
                },
                onCancelGeneration = {
                    HapticUtil.lightTap(context)
                    viewModel.cancelGeneration()
                }
            )
        }
    }

    }

    // Full Screen Image Preview Dialog
    if (previewImageFilePath != null) {
        FullImagePreviewDialog(
            filePath = previewImageFilePath!!,
            onDismiss = { previewImageFilePath = null }
        )
    }

    // Attachment Modal Bottom Sheet
    if (showAttachmentSheet) {
        AttachmentPickerBottomSheet(
            onPickGallery = {
                showAttachmentSheet = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onTakePhoto = {
                showAttachmentSheet = false
                cameraLauncher.launch(null)
            },
            onPickDocument = {
                showAttachmentSheet = false
                documentPickerLauncher.launch("*/*")
            },
            onDismiss = { showAttachmentSheet = false }
        )
    }

    if (showProviderPicker) {
        ChatProviderManager(onDismiss = {
            showProviderPicker = false
            viewModel.refreshProviders()
        })
    }

    // Reasoning Effort Bottom Sheet
    if (showReasoningSheet) {
        ReasoningEffortBottomSheet(
            currentEffort = reasoningEffort,
            onSelectEffort = { reasoningEffort = it },
            onDismiss = { showReasoningSheet = false }
        )
    }
}

/**
 * DeepSeek Signature Floating Island Input Card.
 * Floats gracefully over the chat background with zero docked bars.
 * Multi-layer container with auto-expanding input, Think toggle pill, Prompts pill,
 * plus button with rotating morph animation, and smooth expandable media tray.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeepSeekStyleFloatingInputCard(
    modifier: Modifier = Modifier,
    textInput: String,
    onTextChanged: (String) -> Unit,
    isLoading: Boolean,
    isAttaching: Boolean,
    pendingAttachments: List<ChatAttachment>,
    reasoningEffort: String,
    pendingReview: String? = null,
    canSendReview: Boolean = false,
    onSendReview: () -> Unit = {},
    onDismissReview: () -> Unit = {},
    onOpenReasoningSelector: () -> Unit,
    quizToolEnabled: Boolean,
    onToggleQuizTool: () -> Unit,
    showAttachmentTray: Boolean,
    onToggleAttachmentTray: () -> Unit,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickDocument: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onPreviewImage: (String) -> Unit,
    onSend: () -> Unit,
    onCancelGeneration: () -> Unit,
    showIdleGlow: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val canSend = (textInput.isNotBlank() || pendingAttachments.isNotEmpty()) && !isLoading
    var inputFocused by remember { mutableStateOf(false) }
    // The target inset changes at IME animation start, unlike the current inset.
    val imeTargetVisible = WindowInsets.imeAnimationTarget.getBottom(LocalDensity.current) > 0
    val interactiveFocus = inputFocused && imeTargetVisible
    val expanded = interactiveFocus || showAttachmentTray || isAttaching ||
        pendingReview != null || pendingAttachments.isNotEmpty()
    val inset by animateDpAsState(
        if (expanded) 8.dp else 44.dp,
        spring(dampingRatio = 0.78f, stiffness = 190f), label = "composerInset"
    )
    val corner by animateDpAsState(
        if (expanded) 28.dp else 32.dp,
        spring(dampingRatio = 0.78f, stiffness = 190f), label = "composerCorner"
    )
    val verticalPadding by animateDpAsState(
        if (expanded) 8.dp else 4.dp,
        spring(dampingRatio = 0.78f, stiffness = 190f), label = "composerPadding"
    )
    val elevation by animateDpAsState(
        if (interactiveFocus) 9.dp else 2.dp,
        spring(dampingRatio = 0.78f, stiffness = 190f), label = "composerElevation"
    )
    val composerBorder by animateColorAsState(
        if (interactiveFocus) MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.35f else 0.5f),
        tween(300), label = "composerBorder"
    )

    val plusRotation by animateFloatAsState(
        targetValue = if (showAttachmentTray) 45f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 220f),
        label = "plusRotation"
    )

    val plusContainer by animateColorAsState(
        if (showAttachmentTray) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
        tween(180), label = "attachmentButtonColor"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = inset, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating Island Container Card
        Surface(
            modifier = Modifier.fillMaxWidth().composerIdleGlow(
                enabled = showIdleGlow && !imeTargetVisible && !expanded && !isLoading,
                corner = corner
            ),
            shape = RoundedCornerShape(corner),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(
                width = 1.dp,
                color = composerBorder
            ),
            shadowElevation = elevation,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = verticalPadding)
            ) {
                val reviewPayload = remember(pendingReview) {
                    pendingReview?.let(com.hkm.pozix.util.QuizAiFollowUp::decode)
                }

                AnimatedVisibility(
                    visible = reviewPayload != null,
                    enter = expandVertically(spring(dampingRatio = 0.82f)) + fadeIn(tween(180)),
                    exit = shrinkVertically(spring(dampingRatio = 0.82f)) + fadeOut(tween(140))
                ) {
                    reviewPayload?.let { payload ->
                        QuizReviewDraftCard(
                            payload = payload,
                            enabled = canSendReview,
                            onSend = onSendReview,
                            onDismiss = onDismissReview
                        )
                    }
                }

                // 1. Attached Items Preview Row (Images & Document Chips)
                AnimatedVisibility(
                    visible = pendingAttachments.isNotEmpty() || isAttaching,
                    enter = expandVertically(spring(dampingRatio = 0.75f)) + fadeIn(tween(200)),
                    exit = shrinkVertically(spring(dampingRatio = 0.75f)) + fadeOut(tween(200))
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isAttaching) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                }
                            }
                        }

                        items(pendingAttachments, key = { it.id }) { item ->
                            if (item.type == AttachmentType.IMAGE) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                ) {
                                    val bitmap = remember(item.localPath) {
                                        try {
                                            BitmapFactory.decodeFile(item.localPath)?.asImageBitmap()
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = item.name,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable { onPreviewImage(item.localPath) },
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Surface(
                                        onClick = { onRemoveAttachment(item.id) },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.ai_chat_delete),
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.ai_chat_delete),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { onRemoveAttachment(item.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Keep the same field and actions mounted through every morph.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .animateContentSize(
                            animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
                            alignment = Alignment.BottomStart
                        )
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textInput.isEmpty()) {
                        Text(
                            text = if (pendingAttachments.isNotEmpty()) stringResource(R.string.ai_chat_input_with_attachments)
                            else stringResource(R.string.ai_chat_input_placeholder),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                lineHeight = 21.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    BasicTextField(
                        value = textInput,
                        onValueChange = onTextChanged,
                        modifier = Modifier.fillMaxWidth().onFocusChanged { inputFocused = it.isFocused },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        maxLines = if (expanded) 6 else 1,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = true,
                            imeAction = ImeAction.Default,
                            keyboardType = KeyboardType.Text
                        )
                    )
                }

                    // Right Side: (+) Button and Send/Stop Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // DeepSeek Circular (+) Expand / Rotate Button
                        Surface(
                            onClick = onToggleAttachmentTray,
                            shape = CircleShape,
                            color = plusContainer,
                            border = BorderStroke(
                                width = 0.8.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.ai_chat_attach_title),
                                    tint = if (showAttachmentTray) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer { rotationZ = plusRotation }
                                )
                            }
                        }

                        // Send / Stop Button with smooth morphing
                        AnimatedContent(
                            targetState = isLoading,
                            transitionSpec = {
                                (scaleIn(spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(150))) togetherWith
                                (scaleOut(tween(120)) + fadeOut(tween(120)))
                            },
                            label = "sendStopMorph"
                        ) { loading ->
                            if (loading) {
                                Surface(
                                    onClick = onCancelGeneration,
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = stringResource(R.string.ai_chat_stop),
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    onClick = {
                                        if (canSend) {
                                            onSend()
                                        }
                                    },
                                    enabled = canSend,
                                    shape = CircleShape,
                                    color = if (canSend) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = stringResource(R.string.ai_chat_send),
                                            tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(spring(dampingRatio = 0.82f, stiffness = 190f)) + fadeIn(tween(260)),
                    exit = shrinkVertically(spring(dampingRatio = 0.82f, stiffness = 190f)) + fadeOut(tween(220))
                ) {
                // 3. DeepSeek Signature Bottom Control Bar:
                // [ 🧠 Think ]  [ 📋 Prompts ]   ...   [ (+) ]  [ (↑) / (■) ]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Side: Feature Toggle Pills
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isReasoningActive = reasoningEffort.lowercase() != "off"
                        val thinkLabel = when (reasoningEffort.lowercase()) {
                            "off" -> stringResource(R.string.ai_reasoning_off)
                            "low" -> stringResource(R.string.ai_reasoning_low)
                            "medium" -> stringResource(R.string.ai_reasoning_medium)
                            "high" -> stringResource(R.string.ai_reasoning_high)
                            else -> reasoningEffort
                        }

                        // DeepSeek "Think" / Reasoning Effort Pill
                        Surface(
                            onClick = onOpenReasoningSelector,
                            shape = RoundedCornerShape(20.dp),
                            color = if (isReasoningActive) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                            border = BorderStroke(
                                width = if (isReasoningActive) 1.2.dp else 0.8.dp,
                                color = if (isReasoningActive) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = stringResource(R.string.ai_chat_think),
                                    tint = if (isReasoningActive) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = stringResource(R.string.ai_reasoning_pill_format, thinkLabel),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (isReasoningActive) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isReasoningActive) MaterialTheme.colorScheme.onPrimaryContainer
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Quiz availability: permission, not forced generation.
                        val quizContainer by animateColorAsState(
                            targetValue = if (quizToolEnabled) {
                                MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.24f else 0.12f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
                            },
                            animationSpec = tween(220),
                            label = "quizToolContainer"
                        )
                        val quizContent = if (quizToolEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            onClick = onToggleQuizTool,
                            shape = RoundedCornerShape(20.dp),
                            color = quizContainer,
                            border = BorderStroke(
                                width = if (quizToolEnabled) 1.1.dp else 0.8.dp,
                                color = if (quizToolEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesomeMotion,
                                    contentDescription = if (quizToolEnabled) "Quiz: On" else "Quiz: Off",
                                    tint = quizContent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (quizToolEnabled) "Quiz: On" else "Quiz: Off",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (quizToolEnabled) FontWeight.SemiBold else FontWeight.Medium
                                    ),
                                    color = quizContent
                                )
                            }
                        }
                    }


                }

                }
                // 4. DeepSeek Expandable Attachment Tray (Inline, expands with spring)
                AnimatedVisibility(
                    visible = showAttachmentTray,
                    enter = expandVertically(spring(dampingRatio = 0.82f, stiffness = 190f)) + fadeIn(tween(260)),
                    exit = shrinkVertically(spring(dampingRatio = 0.82f, stiffness = 190f)) + fadeOut(tween(220))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Camera tile
                            DeepSeekActionTile(
                                icon = Icons.Default.CameraAlt,
                                label = stringResource(R.string.ai_chat_camera),
                                onClick = onTakePhoto,
                                modifier = Modifier.weight(1f)
                            )

                            // Photos tile
                            DeepSeekActionTile(
                                icon = Icons.Default.PhotoLibrary,
                                label = stringResource(R.string.ai_chat_photos),
                                onClick = onPickGallery,
                                modifier = Modifier.weight(1f)
                            )

                            // Documents tile
                            DeepSeekActionTile(
                                icon = Icons.Default.FolderOpen,
                                label = stringResource(R.string.ai_chat_documents),
                                onClick = onPickDocument,
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
private fun DeepSeekActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier.height(76.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * ChatGPT Style Attachment Sheet: Photos, Camera, Files
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentPickerBottomSheet(
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickDocument: () -> Unit,
    onDismiss: () -> Unit
) {
    PozixModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        BouncyContainer(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.ai_chat_attach_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Option 1: Gallery
                AttachmentOptionItem(
                    icon = Icons.Default.PhotoLibrary,
                    title = stringResource(R.string.ai_chat_photo_gallery),
                    subtitle = stringResource(R.string.ai_chat_photo_gallery_desc),
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onPickGallery
                )

                // Option 2: Camera
                AttachmentOptionItem(
                    icon = Icons.Default.CameraAlt,
                    title = stringResource(R.string.ai_chat_take_photo),
                    subtitle = stringResource(R.string.ai_chat_take_photo_desc),
                    iconBg = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onTakePhoto
                )

                // Option 3: Document / File
                AttachmentOptionItem(
                    icon = Icons.Default.FolderOpen,
                    title = stringResource(R.string.ai_chat_docs),
                    subtitle = stringResource(R.string.ai_chat_docs_desc),
                    iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onPickDocument
                )
            }
        }
    }
}

@Composable
private fun AttachmentOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconBg: Color,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * ChatGPT-style Chat Bubble
 */
@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    isStreaming: Boolean = false,
    phase: AiPhase = AiPhase.IDLE,
    onImageClick: (String) -> Unit,
    onImportPlay: (String) -> Unit,
    onSaveLibrary: (String) -> Unit,
    section: String = "all"
) {
    val isUser = message.role == "user"
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val context = LocalContext.current
    val artifact by produceState<AiQuizOutput.Artifact?>(null, message.quizGeneration, message.quizJson, message.text, isStreaming) {
        if (!isStreaming && !isUser && section != "header") value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            when {
                message.quizJson != null -> AiQuizOutput.Artifact(message.quizJson, message.text)
                message.quizGeneration -> AiQuizOutput.extract(message.text)
                else -> null
            }
        }
    }

    val jsonBlock = artifact?.json

    if (isUser) {
        // User Message: Aligned to end, compact pill layout
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(
                modifier = Modifier.widthIn(max = 320.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Attached Images Thumbnails
                if (message.imagePaths.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(message.imagePaths) { imgPath ->
                            val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, imgPath) {
                                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                    BitmapFactory.decodeFile(imgPath, options)
                                    options.inSampleSize = 1
                                    while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 640)
                                        options.inSampleSize *= 2
                                    options.inJustDecodeBounds = false
                                    BitmapFactory.decodeFile(imgPath, options)?.asImageBitmap()
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap!!,
                                    contentDescription = stringResource(R.string.ai_chat_view_image),
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        .clickable { onImageClick(imgPath) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                // Attached Documents Chips
                val docAttachments = remember(message.attachments) {
                    message.attachments.filter { it.type == AttachmentType.DOCUMENT }
                }
                if (docAttachments.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        docAttachments.forEach { doc ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${doc.name} (${doc.formattedSize})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // User Message Pill
                val userBubbleColor = MaterialTheme.colorScheme.primaryContainer
                val userTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = userBubbleColor
                ) {
                    Text(
                        text = message.quizReviewJson?.let { com.hkm.pozix.util.QuizAiFollowUp.decode(it) }
                            ?.let { "${it.title}\n${it.score} / ${it.totalQuestions}\n${stringResource(R.string.ai_review_results)}" }
                            ?: message.text,
                        color = userTextColor,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    } else {
        // Assistant Message: Full width open layout with compact header, rich markdown, math & code blocks
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            val fullTargetText = artifact?.displayText ?: message.text
            if (section != "footer") {
            // Assistant Brand Header (Avatar + Zix Bot Name)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                ZixBotAvatar(isStreaming = isStreaming, size = 26.dp)
                Text(
                    text = "Zix Bot",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Reasoning / Thinking Accordion Card (if model outputs thought process)
            if (!message.reasoning.isNullOrBlank()) {
                ReasoningAccordionCard(
                    reasoning = message.reasoning,
                    thinkingDurationMs = message.thinkingDurationMs,
                    isStreaming = isStreaming && fullTargetText.isBlank(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )
            }

            }
            // The SSE publisher is already throttled to a frame-friendly cadence.
            // Do not replay the whole answer through a second character animation:
            // that doubled recompositions and forced Markdown/KaTeX work on every
            // frame of a long response.
            val flowingText = fullTargetText

            if (section == "all" && flowingText.isNotBlank()) {
                StreamingRichContentText(
                    text = flowingText,
                    streaming = isStreaming || flowingText != fullTargetText,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 23.sp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }

            if (section != "header") {
            message.errorNotice?.takeIf { it.isNotBlank() }?.let { notice ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.32f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = notice.trim(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Zix Bot Liquid Streaming Indicator
            if (isStreaming) {
                AiPhaseLine(phase)
            }

            // Haptic feedback tick when streaming settles
            var wasStreaming by remember { mutableStateOf(false) }
            LaunchedEffect(isStreaming) {
                if (wasStreaming && !isStreaming && fullTargetText.isNotBlank()) {
                    HapticUtil.selectionTick(context)
                }
                wasStreaming = isStreaming
            }

            // Assistant Action Row (Copy button)
            AnimatedVisibility(
                visible = !isStreaming && fullTargetText.isNotBlank(),
                enter = fadeIn(tween(250)) + expandVertically(tween(250))
            ) {
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    HapticUtil.selectionTick(context)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Zix Bot Message", fullTargetText))
                                    Toast.makeText(context, context.getString(R.string.ai_chat_copied_reply), Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.ai_chat_copy),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.ai_chat_copy),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Interactive Quiz Card if AI generated a quiz JSON
            AnimatedVisibility(
                visible = jsonBlock != null && !isStreaming,
                enter = fadeIn(tween(350)) + expandVertically(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)) + slideInVertically { it / 3 }
            ) {
                if (jsonBlock != null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        GeneratedQuizCard(
                            jsonText = jsonBlock,
                            onImportPlay = { onImportPlay(jsonBlock) },
                            onSaveLibrary = { onSaveLibrary(jsonBlock) }
                        )
                    }
                }
            }
            }
        }
    }
}

/**
 * Modern, polished Quiz Card generated by AI.
 */
@Composable
fun GeneratedQuizCard(
    jsonText: String,
    onImportPlay: () -> Unit,
    onSaveLibrary: () -> Unit
) {
    val parsed by produceState<QuizValidationResult?>(null, jsonText) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            QuizJsonParser.parseAndValidate(jsonText)
        }
    }
    val validation = parsed
    var isSaved by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    if (validation is QuizValidationResult.Success) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header Banner
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = validation.quiz.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.ai_chat_ai_quiz_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (!validation.quiz.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = validation.quiz.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Breakdown Badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BadgeChip(text = stringResource(R.string.ai_chat_questions_count, validation.parsedQuestions.size), color = MaterialTheme.colorScheme.primary)
                    if (validation.singleChoiceCount > 0) {
                        BadgeChip(text = stringResource(R.string.ai_chat_single_choice_count, validation.singleChoiceCount), color = MaterialTheme.colorScheme.tertiary)
                    }
                    if (validation.trueFalseCount > 0) {
                        BadgeChip(text = stringResource(R.string.ai_chat_true_false_count, validation.trueFalseCount), color = MaterialTheme.colorScheme.secondary)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onImportPlay,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.ai_chat_start_exam), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            onSaveLibrary()
                            isSaved = true
                        },
                        enabled = !isSaved,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Check else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSaved) stringResource(R.string.ai_chat_saved_quiz) else stringResource(R.string.ai_chat_save_quiz), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Liquid Text Streaming Engine ("hiệu ứng tuôn text ra").
 * Pours characters out continuously and smoothly at 60fps (~16ms/tick).
 * Uses an adaptive dynamic pacer:
 * - If backlog is small (1-10 chars): trickles 1-2 chars per tick for silky natural typewriter flow.
 * - If network dumps a large SSE chunk (30-100+ chars): dynamically accelerates so it never falls behind.
 * - Instantly returns targetText if not actively streaming (old messages, chat history).
 */
/**
 * Collapsible Thinking Process Card for Reasoning Models.
 */
/**
 * Collapsible Thinking Process Card for Reasoning Models (e.g. DeepSeek R1, OpenAI o1/o3, Gemini Thinking).
 * Designed after modern AI assistant reasoning cards (Image 2):
 * - Default collapsed state as requested by user ("với mặc định cho nó tắt đi").
 * - Outline lightbulb icon with gentle breathing pulse while actively thinking.
 * - Displays duration: "Đã suy nghĩ trong 9.5s" (or "Thought for 9.5 seconds") when completed.
 * - Subtle vertical guide line underneath lightbulb leading into indented thinking content.
 */
@Composable
fun ReasoningAccordionCard(
    reasoning: String,
    thinkingDurationMs: Long?,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    // Always default to collapsed ("với mặc định cho nó tắt đi")
    var isExpanded by remember { mutableStateOf(false) }

    // Breathing pulse for lightbulb while thinking
    val infiniteTransition = rememberInfiniteTransition(label = "thinkingPulse")
    val bulbAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bulbAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = if (isStreaming) {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = bulbAlpha)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    },
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))

                val headerTitle = when {
                    isStreaming -> stringResource(R.string.ai_thinking_in_progress)
                    thinkingDurationMs != null && thinkingDurationMs > 0 -> {
                        val sec = (thinkingDurationMs / 1000L).coerceAtLeast(1L)
                        stringResource(R.string.ai_thought_duration, sec)
                    }
                    else -> stringResource(R.string.ai_thought_process)
                }

                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) stringResource(R.string.ai_thought_collapse) else stringResource(R.string.ai_thought_expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(180))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 4.dp)
                ) {
                    // Subtle vertical guide line underneath lightbulb
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(1.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reasoning,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 18.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pure Character-by-Character Liquid Streaming Engine.
 * Pours characters out continuously letter-by-letter at high frequency (~11ms).
 * Never jumps large chunks at once.
 */
@Composable
fun rememberLiquidStreamText(
    targetText: String,
    isStreaming: Boolean
): String {
    var revealedCount by remember { mutableIntStateOf(if (isStreaming) 0 else targetText.length) }
    val currentTarget by rememberUpdatedState(targetText)
    // Continue draining the small presentation buffer after EOF; never dump it at completion.
    LaunchedEffect(targetText) {
        if (revealedCount > currentTarget.length) revealedCount = 0
        while (revealedCount < currentTarget.length) {
            androidx.compose.runtime.withFrameNanos { }
            revealedCount = com.hkm.pozix.util.StreamPresentation.nextRevealIndex(currentTarget, revealedCount)
        }
    }
    return targetText.take(revealedCount.coerceIn(0, targetText.length))
}

/**
 * Zix Bot Avatar with rotating iridescent halo and breathing scale.
 */
@Composable
fun ZixBotAvatar(
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    if (!isStreaming) {
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Box(Modifier.size(size * 0.94f).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(size * 0.5f))
            }
        }
        return
    }
    val infiniteTransition = rememberInfiniteTransition(label = "zixBotAvatar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "avatarRotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isStreaming) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarPulse"
    )

    val haloColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primary
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale },
        contentAlignment = Alignment.Center
    ) {
        if (isStreaming) {
            // Rotating iridescent outer ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = rotation }
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(haloColors))
            )
            // Cutout circle for distinct glowing border
            Box(
                modifier = Modifier
                    .size(size * 0.86f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        // Core avatar disc
        Box(
            modifier = Modifier
                .size(if (isStreaming) size * 0.78f else size * 0.94f)
                .clip(CircleShape)
                .background(
                    if (isStreaming) {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(if (isStreaming) size * 0.45f else size * 0.5f)
            )
        }
    }
}

/**
 * Zix Bot signature streaming indicator:
 * 1. Radiant pulsing spark orb with bloom halo.
 * 2. Animated label.
 * 3. Sweeping iridescent ribbon line.
 */
@Composable
fun ZixBotStreamingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "zixBotStreamFx")

    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Glowing spark particle & status label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Luminous glowing orb with bloom
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(glowScale),
                contentAlignment = Alignment.Center
            ) {
                // Bloom aura
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f * glowAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Inner core
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                )
            }

            Text(
                text = stringResource(R.string.ai_chat_composing),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
        }

        // Sweeping iridescent ribbon
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(2.5.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ),
                        start = Offset(shimmerTranslate, 0f),
                        end = Offset(shimmerTranslate + 350f, 0f)
                    )
                )
        )
    }
}

/**
 * Zix Bot 3-dot thinking wave with staggered delays.
 */
@Composable
fun PulsingThinkingDots() {
    val transition = rememberInfiniteTransition(label = "zixBotDots")
    val dot1 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 160, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .scale(dot1)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = dot1))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .scale(dot2)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = dot2))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .scale(dot3)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = dot3))
        )
    }
}

/**
 * Zix Bot Shimmering Thinking Card.
 */
@Composable
fun AssistantTypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinkingCard")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ZixBotAvatar(isStreaming = true)

        Spacer(modifier = Modifier.width(12.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ),
                    start = Offset(shimmerOffset, 0f),
                    end = Offset(shimmerOffset + 280f, 0f)
                )
            ),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PulsingThinkingDots()

                Text(
                    text = stringResource(R.string.ai_thinking_in_progress),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ChatWelcomeSection(
    onSuggestionClick: (String) -> Unit,
    onAttachClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.ai_chat_welcome_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.ai_chat_welcome_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        val prompts = listOf(
            stringResource(R.string.ai_chat_suggestion_1),
            stringResource(R.string.ai_chat_suggestion_2),
            stringResource(R.string.ai_chat_suggestion_3),
            stringResource(R.string.ai_chat_suggestion_4),
            stringResource(R.string.ai_suggestion_summary),
            stringResource(R.string.ai_suggestion_plan),
            stringResource(R.string.ai_suggestion_compare),
            stringResource(R.string.ai_suggestion_review)
        )

        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            prompts.forEach { text ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSuggestionClick(text) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullImagePreviewDialog(
    filePath: String,
    onDismiss: () -> Unit
) {
    val bitmap = remember(filePath) {
        try {
            BitmapFactory.decodeFile(filePath)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(R.string.ai_chat_view_image),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = stringResource(R.string.ai_chat_cannot_load_image),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryBottomSheet(
    sessions: List<ChatSession>,
    currentSessionId: String?,
    onSelectSession: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onDismiss: () -> Unit
) {
    PozixModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ai_chat_history),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(
                    onClick = {
                        onNewChat()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.ai_chat_new_chat), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.ai_chat_no_history),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                BouncyContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    items(sessions, key = { it.id }) { session ->
                        val isCurrent = session.id == currentSessionId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onSelectSession(session.id)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isCurrent) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isCurrent) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${stringResource(R.string.ai_chat_messages_count, session.messages.size)} • ${formatSessionDate(session.updatedAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteSession(session.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = stringResource(R.string.ai_chat_delete),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
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

private fun formatSessionDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun ProviderSetupCard(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.ai_chat_no_provider_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.ai_chat_no_provider_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.ai_chat_no_provider_button),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun ProviderPickerDialog(
    providers: List<com.hkm.pozix.data.model.AiProvider>,
    activeId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ai_chat_switch_provider), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                providers.forEach { provider ->
                    val selected = provider.id == activeId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelect(provider.id) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = provider.name.ifBlank { provider.baseUrl },
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = provider.modelId.ifBlank { provider.normalizedBaseUrl() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

private fun extractJsonBlock(text: String): String? {
    val regex = """```json\s+([\s\S]*?)\s*```""".toRegex()
    val matchResult = regex.find(text)
    if (matchResult != null) {
        return matchResult.groupValues[1].trim()
    }
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    if (start != -1 && end != -1 && end > start) {
        val possibleJson = text.substring(start, end + 1).trim()
        if (possibleJson.contains("\"title\"") && possibleJson.contains("\"questions\"")) {
            return possibleJson
        }
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReasoningEffortBottomSheet(
    currentEffort: String,
    onSelectEffort: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var isCustomSelected by remember {
        mutableStateOf(currentEffort.lowercase() !in listOf("off", "low", "medium", "high"))
    }
    var customText by remember {
        mutableStateOf(if (isCustomSelected) currentEffort else "")
    }
    val context = LocalContext.current

    PozixModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        BouncyContainer(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.ai_reasoning_sheet_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.ai_reasoning_sheet_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Standard Options
            val options = listOf(
                Triple("off", stringResource(R.string.ai_reasoning_off), stringResource(R.string.ai_reasoning_off_desc)),
                Triple("low", stringResource(R.string.ai_reasoning_low), stringResource(R.string.ai_reasoning_low_desc)),
                Triple("medium", stringResource(R.string.ai_reasoning_medium), stringResource(R.string.ai_reasoning_medium_desc)),
                Triple("high", stringResource(R.string.ai_reasoning_high), stringResource(R.string.ai_reasoning_high_desc))
            )

            options.forEach { (key, label, desc) ->
                val isSelected = !isCustomSelected && currentEffort.equals(key, ignoreCase = true)
                Surface(
                    onClick = {
                        HapticUtil.selectionTick(context)
                        onSelectEffort(key)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.8.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Custom Option Card
            Surface(
                onClick = {
                    isCustomSelected = true
                    HapticUtil.selectionTick(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (isCustomSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    width = if (isCustomSelected) 1.5.dp else 0.8.dp,
                    color = if (isCustomSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ai_reasoning_custom),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.ai_reasoning_custom_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isCustomSelected) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customText,
                                onValueChange = { customText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(stringResource(R.string.ai_reasoning_custom_hint)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = {
                                    val trimmed = customText.trim()
                                    if (trimmed.isNotBlank()) {
                                        HapticUtil.actionConfirm(context)
                                        onSelectEffort(trimmed)
                                        onDismiss()
                                    }
                                },
                                enabled = customText.isNotBlank(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.ok), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
}
