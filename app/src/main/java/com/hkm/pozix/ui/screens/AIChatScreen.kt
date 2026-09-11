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

import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.hkm.pozix.ui.components.richcontent.RichContentText
import androidx.compose.foundation.text.selection.SelectionContainer
import com.hkm.pozix.util.HapticUtil
import com.hkm.pozix.util.QuizJsonParser
import com.hkm.pozix.viewmodel.AIChatUiState
import com.hkm.pozix.viewmodel.AIChatViewModel
import com.hkm.pozix.viewmodel.ImportStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var showProviderPicker by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var previewImageFilePath by remember { mutableStateOf<String?>(null) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
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
                itemBottom <= viewportBottom + 40
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
                    (last.offset + last.size - layout.viewportEndOffset).coerceAtLeast(0)
                else 0
            }
        }.collect { overflow ->
            if (overflow > 0 && followTail && !listState.isScrollInProgress) {
                listState.scroll { scrollBy(overflow.toFloat()) }
            }
        }
    }

    // Root layout using imePadding to guarantee input bar is NEVER hidden by the keyboard
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                navigationIcon = {
                    IconButton(onClick = {
                        HapticUtil.lightTap(context)
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                title = {
                    // Modern Centered Pill Model Selector
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .clickable {
                                HapticUtil.lightTap(context)
                                showProviderPicker = true
                            },
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Zix Bot",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = uiState.activeProvider?.let { "${it.name} • ${it.modelId.takeLast(16)}" } ?: "Chọn AI Model",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                actions = {
                    // 1. Sleek New Chat Button
                    Surface(
                        onClick = {
                            HapticUtil.lightTap(context)
                            viewModel.startNewChat()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Đoạn chat mới",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // 2. Options Dropdown Menu Button
                    Box {
                        Surface(
                            onClick = {
                                HapticUtil.lightTap(context)
                                showMoreMenu = true
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Tùy chọn",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Lịch sử trò chuyện") },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    HapticUtil.lightTap(context)
                                    showHistorySheet = true
                                }
                            )
                            if (onOpenTemplates != null) {
                                DropdownMenuItem(
                                    text = { Text("Mẫu câu lệnh & JSON") },
                                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        HapticUtil.lightTap(context)
                                        onOpenTemplates()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Quản lý AI Providers") },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onNavigateToSettings()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Xóa đoạn chat này", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMoreMenu = false
                                    HapticUtil.lightTap(context)
                                    viewModel.clearChat()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        // MAIN COLUMN WITH imePadding() - Pushes everything (messages + input) above soft keyboard
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
        ) {
            if (uiState.activeProvider == null) {
                // Setup Card when no provider is added yet
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ProviderSetupCard(
                        onNavigateToSettings = onNavigateToSettings,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                // Chat Message Stream with Fluid Transitions
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = uiState.messages.isEmpty(),
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(250))
                        },
                        label = "chatContentTransition"
                    ) { isEmpty ->
                        if (isEmpty) {
                            ChatWelcomeSection(
                                onSuggestionClick = { suggestion ->
                                    HapticUtil.lightTap(context)
                                    viewModel.sendMessage(suggestion)
                                },
                                onAttachClick = { showAttachmentSheet = true }
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
                            ) {
                                itemsIndexed(
                                    items = uiState.messages,
                                    key = { index, message -> "${message.timestamp}_${message.role}_$index" }
                                ) { index, message ->
                                    val isLast = index == uiState.messages.lastIndex
                                    val isStreaming = uiState.isLoading && isLast && message.role == "model"
                                    Box(
                                        modifier = if (isStreaming) Modifier else Modifier.animateItem(
                                            fadeInSpec = tween(300),
                                            fadeOutSpec = tween(250),
                                            placementSpec = spring(
                                                dampingRatio = 0.8f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    ) {
                                        ChatBubbleItem(
                                            message = message,
                                            isStreaming = isStreaming,
                                            onImageClick = { previewImageFilePath = it },
                                            onImportPlay = { json ->
                                                HapticUtil.lightTap(context)
                                                viewModel.importQuizSet(json, onPlayQuiz)
                                            },
                                            onSaveLibrary = { json ->
                                                HapticUtil.lightTap(context)
                                                viewModel.saveQuizSetOnly(json)
                                            }
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
                                            AssistantTypingIndicator()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating Scroll-to-Bottom FAB when user scrolls up
                    androidx.compose.animation.AnimatedVisibility(
                        visible = userScrolledUp,
                        enter = fadeIn(tween(200)) + scaleIn(spring(dampingRatio = 0.7f)),
                        exit = fadeOut(tween(150)) + scaleOut(tween(150)),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 12.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                HapticUtil.lightTap(context)
                                userScrolledUp = false
                                scope.launch {
                                    if (uiState.messages.isNotEmpty()) {
                                        listState.animateScrollToItem(uiState.messages.size - 1, scrollOffset = 100000)
                                    }
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.primary,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Cuộn xuống đáy",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // ChatGPT SIGNATURE CAPSULE INPUT BAR
                ChatGPTStyleInputBar(
                    textInput = textInput,
                    onTextChanged = { textInput = it },
                    isLoading = uiState.isLoading,
                    isAttaching = uiState.isAttaching,
                    pendingAttachments = uiState.pendingAttachments,
                    onAttachClick = { showAttachmentSheet = true },
                    onRemoveAttachment = { viewModel.removePendingAttachment(it) },
                    onPreviewImage = { previewImageFilePath = it },
                    onSend = {
                        if (textInput.isNotBlank() || uiState.pendingAttachments.isNotEmpty()) {
                            HapticUtil.lightTap(context)
                            userScrolledUp = false
                            viewModel.sendMessage(textInput.trim())
                            textInput = ""
                            keyboardController?.hide()
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

    // Chat History Bottom Sheet
    if (showHistorySheet) {
        ChatHistoryBottomSheet(
            sessions = uiState.sessions,
            currentSessionId = uiState.currentSessionId,
            onSelectSession = { id ->
                HapticUtil.lightTap(context)
                viewModel.loadSession(id)
            },
            onNewChat = {
                HapticUtil.lightTap(context)
                viewModel.startNewChat()
            },
            onDeleteSession = { id ->
                HapticUtil.lightTap(context)
                viewModel.deleteSession(id)
            },
            onDismiss = { showHistorySheet = false }
        )
    }

    // Provider Picker Dialog
    if (showProviderPicker && uiState.providers.size > 1) {
        ProviderPickerDialog(
            providers = uiState.providers,
            activeId = uiState.activeProvider?.id,
            onSelect = { id ->
                HapticUtil.lightTap(context)
                viewModel.selectProvider(id)
                showProviderPicker = false
            },
            onDismiss = { showProviderPicker = false }
        )
    }
}

/**
 * Signature ChatGPT Pill Input Bar.
 * Elegant, rounded, floating container with attachment menu and circular send/stop button.
 */
@Composable
fun ChatGPTStyleInputBar(
    textInput: String,
    onTextChanged: (String) -> Unit,
    isLoading: Boolean,
    isAttaching: Boolean,
    pendingAttachments: List<ChatAttachment>,
    onAttachClick: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onPreviewImage: (String) -> Unit,
    onSend: () -> Unit,
    onCancelGeneration: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val canSend = (textInput.isNotBlank() || pendingAttachments.isNotEmpty()) && !isLoading

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Attached Items Preview Row (Images & Document Chips) with smooth expand/collapse
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
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    }

                    items(pendingAttachments, key = { it.id }) { item ->
                        if (item.type == AttachmentType.IMAGE) {
                            // Image Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
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
                                        .padding(3.dp)
                                        .size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Xóa",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // File / Document Chip
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = item.formattedSize,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Xóa",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onRemoveAttachment(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Modern ChatGPT-style Separate Control Row:
            // [ + Circle Button ]  [ Pill Capsule with BasicTextField ]  [ ↑ Send / Stop Circle Button ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Separate circular '+' button
                Surface(
                    onClick = onAttachClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Đính kèm",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 2. Central Capsule Pill for text input
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (textInput.isEmpty()) {
                            Text(
                                text = if (pendingAttachments.isNotEmpty()) "Nhập yêu cầu cho tệp/ảnh..."
                                else "Nhắn tin cho Zix Bot...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        BasicTextField(
                            value = textInput,
                            onValueChange = onTextChanged,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Default,
                                keyboardType = KeyboardType.Text
                            )
                        )
                    }
                }

                // 3. Send or Stop Circular Button with animated morphing & 100% Dynamic Colors
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
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Dừng",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
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
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Gửi",
                                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
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
    val isDark = isSystemInDarkTheme()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Đính kèm vào câu hỏi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Option 1: Gallery
            AttachmentOptionItem(
                icon = Icons.Default.PhotoLibrary,
                title = "Thư viện ảnh",
                subtitle = "Gửi nhiều ảnh đề thi, công thức toán hoặc bài tập",
                iconBg = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onPickGallery
            )

            // Option 2: Camera
            AttachmentOptionItem(
                icon = Icons.Default.CameraAlt,
                title = "Chụp ảnh ngay",
                subtitle = "Chụp trực tiếp đề bài từ sách hoặc bài làm",
                iconBg = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onTakePhoto
            )

            // Option 3: Document / File
            AttachmentOptionItem(
                icon = Icons.Default.FolderOpen,
                title = "Tệp tin & Tài liệu",
                subtitle = "Tải lên tệp JSON câu hỏi, tệp văn bản TXT, Markdown...",
                iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = onPickDocument
            )
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
    val isDark = isSystemInDarkTheme()
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
    onImageClick: (String) -> Unit,
    onImportPlay: (String) -> Unit,
    onSaveLibrary: (String) -> Unit
) {
    val isUser = message.role == "user"
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val jsonBlock = remember(message.text) { extractJsonBlock(message.text) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        // Assistant Sparkle Avatar (Zix Bot Iridescent Halo when streaming)
        if (!isUser) {
            ZixBotAvatar(isStreaming = isStreaming)
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Attached Images Thumbnails
            if (message.imagePaths.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(message.imagePaths) { imgPath ->
                        val bitmap = remember(imgPath) {
                            try {
                                BitmapFactory.decodeFile(imgPath)?.asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Image attachment",
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

            // Message Bubble Box
            val userBubbleColor = MaterialTheme.colorScheme.primaryContainer
            val userTextColor = MaterialTheme.colorScheme.onPrimaryContainer

            if (isUser) {
                // User Message Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = userBubbleColor
                ) {
                    Text(
                        text = message.text,
                        color = userTextColor,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            } else {
                // Assistant Message: Natural open layout with rich markdown, math & code blocks
                val fullTargetText = remember(message.text) {
                    if (jsonBlock != null) {
                        val withoutFence = message.text.substringBefore("```json").trim()
                        if (withoutFence.isNotEmpty()) withoutFence
                        else message.text.replace(jsonBlock, "").replace("```json", "").replace("```", "").trim()
                    } else {
                        message.text
                    }
                }

                // Reasoning / Thinking Accordion Card (if model outputs thought process)
                if (!message.reasoning.isNullOrBlank()) {
                    ReasoningAccordionCard(
                        reasoning = message.reasoning,
                        thinkingDurationMs = message.thinkingDurationMs,
                        isStreaming = isStreaming && fullTargetText.isBlank(),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Liquid text pouring engine ("tuôn text ra") in true realtime
                val flowingText = rememberLiquidStreamText(
                    targetText = fullTargetText,
                    isStreaming = isStreaming
                )

                if (flowingText.isNotBlank()) {
                    RichContentText(
                        text = flowingText,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 23.sp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Zix Bot Liquid Streaming Indicator (Luminous spark + sweeping ribbon)
                if (isStreaming) {
                    ZixBotStreamingIndicator()
                }

                // Haptic feedback tick when streaming settles
                var wasStreaming by remember { mutableStateOf(false) }
                LaunchedEffect(isStreaming) {
                    if (wasStreaming && !isStreaming && fullTargetText.isNotBlank()) {
                        HapticUtil.selectionTick(context)
                    }
                    wasStreaming = isStreaming
                }

                // Assistant Action Row (Copy button) - smoothly animated when streaming settles
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
                                        Toast.makeText(context, "Đã sao chép câu trả lời", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Sao chép",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Sao chép",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Quiz Card if AI generated a quiz JSON (with spring reveal animation)
            AnimatedVisibility(
                visible = jsonBlock != null,
                enter = fadeIn(tween(350)) + expandVertically(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)) + slideInVertically { it / 3 }
            ) {
                if (jsonBlock != null) {
                    Column {
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

/**
 * Modern, polished Quiz Card generated by AI.
 */
@Composable
fun GeneratedQuizCard(
    jsonText: String,
    onImportPlay: () -> Unit,
    onSaveLibrary: () -> Unit
) {
    val validation = remember(jsonText) { QuizJsonParser.parseAndValidate(jsonText) }
    var isSaved by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

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
                            text = "Bộ đề thi tạo bởi AI",
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
                    BadgeChip(text = "${validation.parsedQuestions.size} câu hỏi", color = MaterialTheme.colorScheme.primary)
                    if (validation.singleChoiceCount > 0) {
                        BadgeChip(text = "${validation.singleChoiceCount} trắc nghiệm", color = MaterialTheme.colorScheme.tertiary)
                    }
                    if (validation.trueFalseCount > 0) {
                        BadgeChip(text = "${validation.trueFalseCount} đúng/sai", color = MaterialTheme.colorScheme.secondary)
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
                        Text("Vào thi ngay", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                        Text(if (isSaved) "Đã lưu" else "Lưu đề", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                    isStreaming -> "Đang suy nghĩ..."
                    thinkingDurationMs != null && thinkingDurationMs > 0 -> {
                        val sec = thinkingDurationMs / 1000.0
                        "Đã suy nghĩ trong ${"%.1f".format(Locale.US, sec)}s"
                    }
                    else -> "Quá trình suy nghĩ"
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
                    contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
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
                            text = rememberLiquidStreamText(reasoning, isStreaming),
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
    modifier: Modifier = Modifier
) {
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
            .size(36.dp)
            .scale(pulseScale),
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
                    .size(31.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        // Core avatar disc
        Box(
            modifier = Modifier
                .size(if (isStreaming) 28.dp else 34.dp)
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
                modifier = Modifier.size(if (isStreaming) 16.dp else 18.dp)
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
                text = "Zix Bot đang soạn câu trả lời...",
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
                    text = "Zix Bot đang suy nghĩ...",
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
    val isDark = isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
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
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Tôi có thể giúp gì cho bạn?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Tạo đề thi trắc nghiệm, giải bài tập Toán - KHTN từ ảnh chụp hoặc phân tích tệp câu hỏi",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        val prompts = listOf(
            "Tạo bài kiểm tra 10 câu Tiếng Anh Ngữ pháp có giải thích chi tiết",
            "Giải bài toán đạo hàm & tích phân này kèm công thức LaTeX",
            "Tạo đề thi 15 câu Hóa học Este - Lipit",
            "Phân tích đề bài từ hình ảnh hoặc tệp JSON câu hỏi đính kèm"
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            prompts.forEach { text ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSuggestionClick(text) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
                        contentDescription = "Xem ảnh",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "Không thể tải ảnh",
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
    val isDark = isSystemInDarkTheme()
    ModalBottomSheet(
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
                    text = "Lịch sử trò chuyện",
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
                    Text("Chat mới", fontWeight = FontWeight.SemiBold)
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
                        text = "Chưa có cuộc trò chuyện nào",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
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
                                        text = "${session.messages.size} tin nhắn • ${formatSessionDate(session.updatedAt)}",
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
                                        contentDescription = "Xóa",
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
