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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.hkm.pozix.util.HapticUtil
import com.hkm.pozix.util.QuizJsonParser
import com.hkm.pozix.viewmodel.AIChatUiState
import com.hkm.pozix.viewmodel.AIChatViewModel
import com.hkm.pozix.viewmodel.ImportStatus
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onPlayQuiz: () -> Unit,
    viewModel: AIChatViewModel = viewModel()
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

    // Launchers for media and files
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            HapticUtil.lightTap(context)
            viewModel.attachUri(it, isExplicitImage = true)
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            HapticUtil.lightTap(context)
            viewModel.attachUri(it, isExplicitImage = false)
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

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Root layout using imePadding to guarantee input bar is NEVER hidden by the keyboard
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = {
                    // ChatGPT Style Centered/Left Pill Model Selector
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                HapticUtil.lightTap(context)
                                showProviderPicker = true
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xFF262628) else Color(0xFFEBECEF),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.activeProvider?.let { "${it.name} • ${it.modelId.takeLast(16)}" }
                                    ?: "Chọn AI Model",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticUtil.lightTap(context)
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Chat History
                    IconButton(onClick = {
                        HapticUtil.lightTap(context)
                        showHistorySheet = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Lịch sử trò chuyện"
                        )
                    }

                    // New Chat
                    IconButton(onClick = {
                        HapticUtil.lightTap(context)
                        viewModel.startNewChat()
                    }) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "Đoạn chat mới"
                        )
                    }

                    // More Options Dropdown Menu
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Quản lý AI Providers") },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onNavigateToSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Xóa đoạn chat này") },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    HapticUtil.lightTap(context)
                                    viewModel.clearChat()
                                }
                            )
                        }
                    }
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
                // Chat Message Stream
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (uiState.messages.isEmpty()) {
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
                            items(uiState.messages) { message ->
                                ChatBubbleItem(
                                    message = message,
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

                            if (uiState.isLoading) {
                                item {
                                    AssistantTypingIndicator()
                                }
                            }
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
            // Attached Items Preview Row (Images & Document Chips)
            if (pendingAttachments.isNotEmpty() || isAttaching) {
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
                                    .background(if (isDark) Color(0xFF262628) else Color(0xFFEBECEF)),
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
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .clickable { onRemoveAttachment(item.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Xóa",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        } else {
                            // File / Document Chip
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0xFF262628) else Color(0xFFEBECEF),
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

            // Capsule Pill Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = if (isDark) Color(0xFF242426) else Color(0xFFF0F1F4),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF38383A) else Color(0xFFE2E3E8)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Attachment '+' Button
                    IconButton(
                        onClick = onAttachClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Đính kèm",
                            tint = if (isDark) Color(0xFFD4D4D8) else Color(0xFF52525B),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Text Input
                    TextField(
                        value = textInput,
                        onValueChange = onTextChanged,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        placeholder = {
                            Text(
                                text = if (pendingAttachments.isNotEmpty()) "Nhập yêu cầu cho tệp/ảnh..."
                                else "Nhắn tin cho Pozix AI...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93),
                                maxLines = 1
                            )
                        },
                        minLines = 1,
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Default,
                            keyboardType = KeyboardType.Text
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp)
                    )

                    // Send or Stop Button
                    if (isLoading) {
                        // Stop Generation Button
                        Box(
                            modifier = Modifier
                                .padding(bottom = 2.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                                .clickable { onCancelGeneration() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Dừng",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        // Upward Arrow Send Button
                        Box(
                            modifier = Modifier
                                .padding(bottom = 2.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) {
                                        if (isDark) Color.White else Color.Black
                                    } else {
                                        if (isDark) Color(0xFF38383A) else Color(0xFFD1D1D6)
                                    }
                                )
                                .clickable(enabled = canSend) { onSend() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Gửi",
                                tint = if (canSend) {
                                    if (isDark) Color.Black else Color.White
                                } else {
                                    if (isDark) Color(0xFF71717A) else Color(0xFF8E8E93)
                                },
                                modifier = Modifier.size(18.dp)
                            )
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
        containerColor = if (isDark) Color(0xFF1C1C1E) else MaterialTheme.colorScheme.surface
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
                subtitle = "Gửi đề thi, công thức toán hoặc bài tập từ ảnh chụp",
                iconBg = Color(0xFF3B82F6),
                onClick = onPickGallery
            )

            // Option 2: Camera
            AttachmentOptionItem(
                icon = Icons.Default.CameraAlt,
                title = "Chụp ảnh ngay",
                subtitle = "Chụp trực tiếp đề bài từ sách hoặc bài làm",
                iconBg = Color(0xFF10B981),
                onClick = onTakePhoto
            )

            // Option 3: Document / File
            AttachmentOptionItem(
                icon = Icons.Default.FolderOpen,
                title = "Tệp tin & Tài liệu",
                subtitle = "Tải lên tệp JSON câu hỏi, tệp văn bản TXT, Markdown...",
                iconBg = Color(0xFFF59E0B),
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
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconBg, modifier = Modifier.size(22.dp))
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
        // Assistant Sparkle Avatar
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
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
                            color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
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
            val userBubbleColor = if (isDark) Color(0xFF2E2F30) else Color(0xFFE9E9EB)
            val userTextColor = if (isDark) Color(0xFFF2F2F2) else Color(0xFF1C1C1E)

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
                val displayText = remember(message.text) {
                    if (jsonBlock != null) {
                        val withoutFence = message.text.substringBefore("```json").trim()
                        if (withoutFence.isNotEmpty()) withoutFence
                        else message.text.replace(jsonBlock, "").replace("```json", "").replace("```", "").trim()
                    } else {
                        message.text
                    }
                }

                if (displayText.isNotBlank()) {
                    RichContentText(
                        text = displayText,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 23.sp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Assistant Action Row (Copy button)
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            HapticUtil.lightTap(context)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Message", displayText))
                            Toast.makeText(context, "Đã sao chép nội dung", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Sao chép",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Interactive Quiz Card if AI generated a quiz JSON
            if (jsonBlock != null) {
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
                containerColor = if (isDark) Color(0xFF1E1E22) else Color(0xFFF7F7FA)
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
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            tint = Color.White,
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
                        BadgeChip(text = "${validation.trueFalseCount} đúng/sai", color = Color(0xFF10B981))
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

@Composable
fun AssistantTypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "AI đang suy nghĩ & trả lời...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    color = if (isDark) Color(0xFF222224) else Color(0xFFF2F2F5),
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
        containerColor = if (isDark) Color(0xFF1C1C1E) else MaterialTheme.colorScheme.surface
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
                            else if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
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
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(provider.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                text = provider.name,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = provider.modelId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
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
