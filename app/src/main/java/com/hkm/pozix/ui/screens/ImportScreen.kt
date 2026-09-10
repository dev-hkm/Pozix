package com.hkm.pozix.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkm.pozix.R
import com.hkm.pozix.util.HapticUtil
import com.hkm.pozix.viewmodel.ImportViewModel
import com.hkm.pozix.viewmodel.ValidationState

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ImportScreen(
    onQuizLoaded: () -> Unit,
    viewModel: ImportViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var showLinkImport by remember { mutableStateOf(false) }

    // JSON file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importJsonFromFile(it, context.contentResolver)
            HapticUtil.actionConfirm(context)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column {
                Text(
                    text = stringResource(R.string.import_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.import_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Import Source Row (File Picker + Clipboard Paste)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        HapticUtil.actionConfirm(context)
                        filePickerLauncher.launch(arrayOf("application/json", "text/json", "text/plain", "*/*"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    enabled = !uiState.isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.import_choose_file),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = clipboard.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val text = clipData.getItemAt(0).text?.toString() ?: ""
                            if (text.isNotBlank()) {
                                viewModel.updateJsonText(text)
                                HapticUtil.actionConfirm(context)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.import_paste),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Imported File Banner
            if (uiState.importedFileName != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.import_file_loaded, uiState.importedFileName.orEmpty()),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.clearJson()
                                HapticUtil.toggle(context)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // File Error Banner
            if (uiState.fileError != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.fileError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearFileError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Share Link Import Card (Expandable)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.import_link_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.import_link_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                            )
                        }
                        TextButton(onClick = {
                            HapticUtil.selectionTick(context)
                            showLinkImport = !showLinkImport
                        }) {
                            Text(
                                if (showLinkImport) {
                                    stringResource(R.string.import_link_close)
                                } else {
                                    stringResource(R.string.import_link_open)
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    AnimatedVisibility(visible = showLinkImport) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                            OutlinedTextField(
                                value = uiState.shareLink,
                                onValueChange = viewModel::updateShareLink,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.import_share_link)) },
                                placeholder = { Text("https://pozix-cloud-backup.../v1/shares/...") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = {
                                    viewModel.importFromShare()
                                    HapticUtil.actionConfirm(context)
                                },
                                enabled = uiState.shareLink.isNotBlank() && !uiState.isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.import_loading_shared))
                                } else {
                                    Text(stringResource(R.string.import_load_shared), fontWeight = FontWeight.Bold)
                                }
                            }
                            if (uiState.shareStatus.isNotBlank()) {
                                Text(
                                    uiState.shareStatus,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.shareStatusIsError) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Syntax-Highlighted JSON Editor Box
            val editorScroll = rememberScrollState()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(14.dp)
                ) {
                    val keyColor = MaterialTheme.colorScheme.primary
                    val stringColor = MaterialTheme.colorScheme.secondary
                    val numberColor = MaterialTheme.colorScheme.tertiary
                    val booleanColor = MaterialTheme.colorScheme.tertiary
                    val nullColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    val punctuationColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

                    val visualTransformation = remember(keyColor, stringColor, numberColor, booleanColor, nullColor, punctuationColor) {
                        JsonSyntaxHighlightTransformation(
                            keyColor = keyColor,
                            stringColor = stringColor,
                            numberColor = numberColor,
                            booleanColor = booleanColor,
                            nullColor = nullColor,
                            punctuationColor = punctuationColor
                        )
                    }

                    BasicTextField(
                        value = uiState.jsonText,
                        onValueChange = { viewModel.updateJsonText(it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(editorScroll),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.5f,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        visualTransformation = visualTransformation,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (uiState.jsonText.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.import_paste_hint),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // Validation Result
            AnimatedVisibility(
                visible = uiState.validationState !is ValidationState.Idle,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                when (val state = uiState.validationState) {
                    is ValidationState.Success -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.import_success),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = stringResource(R.string.home_quiz_title, state.result.quiz.title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.home_quiz_questions, state.result.parsedQuestions.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.home_quiz_single_choice, state.result.singleChoiceCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.home_quiz_true_false, state.result.trueFalseCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    is ValidationState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.import_error),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    else -> {}
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // =========================================================================
        // PERSISTENT BOTTOM ACTION BAR (IME & Navigation Inset Aware)
        // Stays right above the soft keyboard when keyboard opens.
        // =========================================================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.ime.union(WindowInsets.navigationBars)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clear button (compact, icon + short label)
                    OutlinedButton(
                        onClick = {
                            viewModel.clearJson()
                            HapticUtil.toggle(context)
                        },
                        modifier = Modifier.height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.import_clear),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Validate button
                    FilledTonalButton(
                        onClick = {
                            if (uiState.jsonText.isNotBlank()) {
                                viewModel.validateJson()
                                HapticUtil.primaryAction(context)
                            }
                        },
                        enabled = uiState.jsonText.isNotBlank() && !uiState.isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            stringResource(R.string.import_validate),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Load / Save button (active only when valid)
                    Button(
                        onClick = {
                            viewModel.showSaveDialog()
                            HapticUtil.primaryAction(context)
                        },
                        enabled = uiState.validationState is ValidationState.Success && !uiState.isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.import_load),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    // Save Dialog
    if (uiState.showSaveDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideSaveDialog() },
            title = {
                Text(text = stringResource(R.string.import_save_dialog_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.import_save_dialog_message),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                    OutlinedTextField(
                        value = uiState.quizSetName,
                        onValueChange = { viewModel.updateQuizSetName(it) },
                        label = { Text(stringResource(R.string.import_save_name_label)) },
                        placeholder = { Text(stringResource(R.string.import_save_name_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Button(
                        onClick = {
                            HapticUtil.success(context)
                            viewModel.saveAndLoadQuiz(onQuizLoaded)
                        },
                        enabled = uiState.quizSetName.trim().isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.import_save_and_load), fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = {
                            HapticUtil.actionConfirm(context)
                            viewModel.saveOnlyQuiz {
                                // Stay on import screen
                            }
                        },
                        enabled = uiState.quizSetName.trim().isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.import_save_only), fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        HapticUtil.actionConfirm(context)
                        viewModel.loadQuizWithoutSaving(onQuizLoaded)
                    }
                ) {
                    Text(stringResource(R.string.import_skip))
                }
            }
        )
    }
}

class JsonSyntaxHighlightTransformation(
    private val keyColor: androidx.compose.ui.graphics.Color,
    private val stringColor: androidx.compose.ui.graphics.Color,
    private val numberColor: androidx.compose.ui.graphics.Color,
    private val booleanColor: androidx.compose.ui.graphics.Color,
    private val nullColor: androidx.compose.ui.graphics.Color,
    private val punctuationColor: androidx.compose.ui.graphics.Color
) : VisualTransformation {
    
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val json = text.text
        val annotatedString = buildAnnotatedString {
            append(json)
            
            var i = 0
            while (i < json.length) {
                val char = json[i]
                
                when {
                    // String detection (keys and values)
                    char == '"' -> {
                        val start = i
                        i++
                        var escaped = false
                        while (i < json.length) {
                            if (json[i] == '\\' && !escaped) {
                                escaped = true
                            } else if (json[i] == '"' && !escaped) {
                                i++
                                break
                            } else {
                                escaped = false
                            }
                            i++
                        }
                        
                        // Check if this is a key (followed by :) or a value
                        var j = i
                        while (j < json.length && (json[j] == ' ' || json[j] == '\t' || json[j] == '\n' || json[j] == '\r')) {
                            j++
                        }
                        val isKey = j < json.length && json[j] == ':'
                        
                        addStyle(
                            SpanStyle(
                                color = if (isKey) keyColor else stringColor,
                                fontWeight = if (isKey) FontWeight.Bold else FontWeight.Normal
                            ),
                            start,
                            i
                        )
                        continue
                    }
                    
                    // Numbers
                    char.isDigit() || (char == '-' && i + 1 < json.length && json[i + 1].isDigit()) -> {
                        val start = i
                        if (char == '-') i++
                        while (i < json.length && (json[i].isDigit() || json[i] == '.')) {
                            i++
                        }
                        addStyle(
                            SpanStyle(color = numberColor, fontWeight = FontWeight.Medium),
                            start,
                            i
                        )
                        continue
                    }
                    
                    // Booleans
                    json.startsWith("true", i) -> {
                        addStyle(
                            SpanStyle(color = booleanColor, fontWeight = FontWeight.Bold),
                            i,
                            i + 4
                        )
                        i += 4
                        continue
                    }
                    json.startsWith("false", i) -> {
                        addStyle(
                            SpanStyle(color = booleanColor, fontWeight = FontWeight.Bold),
                            i,
                            i + 5
                        )
                        i += 5
                        continue
                    }
                    
                    // Null
                    json.startsWith("null", i) -> {
                        addStyle(
                            SpanStyle(color = nullColor, fontWeight = FontWeight.Bold),
                            i,
                            i + 4
                        )
                        i += 4
                        continue
                    }
                    
                    // Structural characters
                    char in "{}[],:".toCharArray() -> {
                        addStyle(
                            SpanStyle(color = punctuationColor, fontWeight = FontWeight.Normal),
                            i,
                            i + 1
                        )
                        i++
                        continue
                    }
                    
                    // Default (whitespace, etc.)
                    else -> {
                        i++
                    }
                }
            }
        }

        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}
