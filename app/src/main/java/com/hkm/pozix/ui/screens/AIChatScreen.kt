package com.hkm.pozix.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkm.pozix.R
import com.hkm.pozix.data.model.ChatMessage
import com.hkm.pozix.data.model.ChatSession
import com.hkm.pozix.data.model.QuizValidationResult
import com.hkm.pozix.ui.components.richcontent.RichContentText
import com.hkm.pozix.util.HapticUtil
import com.hkm.pozix.util.QuizJsonParser
import com.hkm.pozix.viewmodel.AIChatUiState
import com.hkm.pozix.viewmodel.AIChatViewModel
import com.hkm.pozix.viewmodel.ImportStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    var showProviderPicker by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var previewImageFilePath by remember { mutableStateOf<String?>(null) }
    var textInput by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            HapticUtil.lightTap(context)
            viewModel.attachImage(it)
        }
    }

    // Refresh active provider when returning from Settings.
    LaunchedEffect(Unit) { viewModel.refreshProviders() }

    // Handle toast messages on import status changes
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
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column {
                        Text(
                            text = uiState.currentSessionTitle.ifBlank { stringResource(R.string.ai_chat_title) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = uiState.activeProvider?.let { "${it.name} • ${it.modelId}" }
                                ?: stringResource(R.string.ai_chat_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
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
                    // Chat History Button
                    IconButton(onClick = {
                        HapticUtil.lightTap(context)
                        showHistorySheet = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Lịch sử trò chuyện"
                        )
                    }

                    // New Chat Button
                    IconButton(onClick = {
                        HapticUtil.lightTap(context)
                        viewModel.startNewChat()
                    }) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "Đoạn chat mới"
                        )
                    }

                    // Switch Provider
                    if (uiState.providers.size > 1) {
                        IconButton(onClick = {
                            HapticUtil.lightTap(context)
                            showProviderPicker = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = stringResource(R.string.ai_chat_switch_provider)
                            )
                        }
                    }

                    // Clear Current Chat
                    IconButton(onClick = {
                        HapticUtil.lightTap(context)
                        viewModel.clearChat()
                    }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.activeProvider == null) {
                ProviderSetupCard(
                    onNavigateToSettings = onNavigateToSettings,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat Messages List
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
                                }
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
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
                                        LoadingBubbleItem()
                                    }
                                }
                            }
                        }
                    }

                    // Input Section
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            // Pending Images Preview Row
                            if (uiState.pendingImages.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(uiState.pendingImages) { path ->
                                        Box(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                        ) {
                                            val bitmap = remember(path) {
                                                try {
                                                    BitmapFactory.decodeFile(path)?.asImageBitmap()
                                                } catch (_: Exception) {
                                                    null
                                                }
                                            }
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap,
                                                    contentDescription = "Preview",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clickable { previewImageFilePath = path },
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    HapticUtil.lightTap(context)
                                                    viewModel.removePendingImage(path)
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(24.dp)
                                                    .padding(2.dp)
                                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Input Controls Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Image Attachment Button
                                IconButton(
                                    onClick = {
                                        HapticUtil.lightTap(context)
                                        imagePickerLauncher.launch("image/*")
                                    },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    if (uiState.isAttachingImage) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Đính kèm ảnh",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Text Input Field
                                TextField(
                                    value = textInput,
                                    onValueChange = { textInput = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp),
                                    placeholder = {
                                        Text(
                                            text = if (uiState.pendingImages.isNotEmpty()) "Nhập yêu cầu giải/tạo đề từ ảnh..."
                                            else stringResource(R.string.ai_chat_input_placeholder),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    },
                                    minLines = 1,
                                    maxLines = 4,
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Send,
                                        keyboardType = KeyboardType.Text
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if ((textInput.isNotBlank() || uiState.pendingImages.isNotEmpty()) && !uiState.isLoading) {
                                                HapticUtil.lightTap(context)
                                                viewModel.sendMessage(textInput.trim())
                                                textInput = ""
                                                keyboardController?.hide()
                                            }
                                        }
                                    ),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )

                                val canSend = (textInput.isNotBlank() || uiState.pendingImages.isNotEmpty()) && !uiState.isLoading

                                IconButton(
                                    onClick = {
                                        if (canSend) {
                                            HapticUtil.lightTap(context)
                                            viewModel.sendMessage(textInput.trim())
                                            textInput = ""
                                            keyboardController?.hide()
                                        }
                                    },
                                    enabled = canSend,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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

    // Full Image Preview Dialog
    if (previewImageFilePath != null) {
        FullImagePreviewDialog(
            filePath = previewImageFilePath!!,
            onDismiss = { previewImageFilePath = null }
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
            shape = RoundedCornerShape(20.dp),
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
                        contentDescription = "Full Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp)
                            .clip(RoundedCornerShape(14.dp)),
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
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
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectSession(session.id)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
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

@Composable
fun ChatWelcomeSection(
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Pozix AI Assistant",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.ai_chat_welcome_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.ai_chat_suggestions_header),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        val suggestions = listOf(
            stringResource(R.string.ai_chat_suggest_1),
            stringResource(R.string.ai_chat_suggest_2),
            stringResource(R.string.ai_chat_suggest_3),
            "Giải chi tiết bài toán đạo hàm và tích phân kèm công thức LaTeX",
            "Tạo bài thi trắc nghiệm 10 câu Hóa học hữu cơ"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { text ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(text) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    onImageClick: (String) -> Unit,
    onImportPlay: (String) -> Unit,
    onSaveLibrary: (String) -> Unit
) {
    val isUser = message.role == "user"
    val jsonBlock = remember(message.text) { extractJsonBlock(message.text) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
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
                                contentDescription = "Image",
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                    .clickable { onImageClick(imgPath) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // Text Bubble with LaTeX & Markdown support
            val bubbleBg = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
            val textColor = if (isUser) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            val shape = if (isUser) {
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
            } else {
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
            }

            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(bubbleBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
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
                        textColor = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Interactive Quiz Card if JSON is available
            if (jsonBlock != null) {
                Spacer(modifier = Modifier.height(8.dp))
                InteractiveQuizCard(
                    jsonText = jsonBlock,
                    onImportPlay = { onImportPlay(jsonBlock) },
                    onSaveLibrary = { onSaveLibrary(jsonBlock) }
                )
            }
        }
    }
}

@Composable
fun InteractiveQuizCard(
    jsonText: String,
    onImportPlay: () -> Unit,
    onSaveLibrary: () -> Unit
) {
    val validation = remember(jsonText) { QuizJsonParser.parseAndValidate(jsonText) }
    var isSaved by remember { mutableStateOf(false) }

    if (validation is QuizValidationResult.Success) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.ai_chat_import_card_title, validation.quiz.title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (!validation.quiz.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validation.quiz.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(
                        R.string.ai_chat_import_card_desc,
                        validation.parsedQuestions.size,
                        validation.singleChoiceCount,
                        validation.trueFalseCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onImportPlay,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.ai_chat_btn_import_play), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = {
                            onSaveLibrary()
                            isSaved = true
                        },
                        enabled = !isSaved,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Check else Icons.Default.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSaved) stringResource(R.string.ai_chat_btn_saved) else stringResource(R.string.ai_chat_btn_save),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingBubbleItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.ai_chat_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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
