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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkm.pozix.R
import com.hkm.pozix.util.HapticUtil
import com.hkm.pozix.viewmodel.ImportViewModel
import com.hkm.pozix.viewmodel.ValidationState

private const val SAMPLE_STEM_JSON = """{
  "title": "Toán & KHTN Mẫu",
  "description": "Đề trắc nghiệm mẫu có công thức LaTeX và Code",
  "language": "vi",
  "questions": [
    {
      "type": "single_choice",
      "question": "Cho hàm số ${'$'}f(x) = x \cdot \ln x${'$'} với ${'$'}x > 0${'$'}. Đạo hàm ${'$'}f'(x)${'$'} bằng:",
      "options": [
        "${'$'}f'(x) = \ln x + 1${'$'}",
        "${'$'}f'(x) = \frac{1}{x}${'$'}",
        "${'$'}f'(x) = 1${'$'}",
        "${'$'}f'(x) = \ln x${'$'}"
      ],
      "correctIndex": 0,
      "explanation": "Áp dụng công thức đạo hàm tích ${'$'}(uv)' = u'v + uv'${'$'}: ${'$'}f'(x) = 1 \cdot \ln x + x \cdot \frac{1}{x} = \ln x + 1${'$'}."
    },
    {
      "type": "true_false",
      "question": "Phương trình bậc hai ${'$'}ax^2 + bx + c = 0${'$'} (${'$'}a \neq 0${'$'}) có nghiệm khi biệt thức ${'$'}\Delta = b^2 - 4ac \ge 0${'$'}.",
      "correctAnswer": true,
      "explanation": "Khi ${'$'}\Delta \ge 0${'$'}, phương trình luôn có ít nhất một nghiệm thực."
    }
  ]
}"""

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ImportScreen(
    onQuizLoaded: () -> Unit,
    viewModel: ImportViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
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
        // Main scrollable content with generous bottom clearance for the floating BottomNavBar
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
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

            // Sample JSON Template Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilterChip(
                    selected = false,
                    onClick = {
                        HapticUtil.selectionTick(context)
                        viewModel.updateJsonText(SAMPLE_STEM_JSON)
                    },
                    label = { Text(stringResource(R.string.import_sample_stem_json), fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DataObject,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
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

            // Action Buttons Card (In-flow, never obscured by BottomNavBar!)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clear button
                    OutlinedButton(
                        onClick = {
                            viewModel.clearJson()
                            HapticUtil.toggle(context)
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.import_clear),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.import_clear), fontWeight = FontWeight.Medium)
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
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            stringResource(R.string.import_validate),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Load / Save button
                    Button(
                        onClick = {
                            viewModel.showSaveDialog()
                            HapticUtil.primaryAction(context)
                        },
                        enabled = uiState.validationState is ValidationState.Success && !uiState.isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
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

    // Fully Responsive & Polished Save Dialog (No layout breaks!)
    if (uiState.showSaveDialog) {
        Dialog(
            onDismissRequest = { viewModel.hideSaveDialog() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Dialog Header
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
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SaveAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.import_save_dialog_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = {
                                HapticUtil.selectionTick(context)
                                viewModel.hideSaveDialog()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.import_save_dialog_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Rename Text Field
                    OutlinedTextField(
                        value = uiState.quizSetName,
                        onValueChange = { viewModel.updateQuizSetName(it) },
                        label = { Text(stringResource(R.string.import_save_name_label)) },
                        placeholder = { Text(stringResource(R.string.import_save_name_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (uiState.quizSetName.isNotBlank()) {
                                IconButton(onClick = { viewModel.updateQuizSetName("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Dialog Actions Vertical Stack
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                HapticUtil.success(context)
                                viewModel.saveAndLoadQuiz(onQuizLoaded)
                            },
                            enabled = uiState.quizSetName.trim().isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.import_save_and_load), fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                HapticUtil.actionConfirm(context)
                                viewModel.saveOnlyQuiz {}
                            },
                            enabled = uiState.quizSetName.trim().isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.import_save_only), fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    HapticUtil.actionConfirm(context)
                                    viewModel.loadQuizWithoutSaving(onQuizLoaded)
                                }
                            ) {
                                Text(stringResource(R.string.import_skip))
                            }

                            TextButton(
                                onClick = {
                                    HapticUtil.selectionTick(context)
                                    viewModel.hideSaveDialog()
                                }
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    }
                }
            }
        }
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
                                i++
                                continue
                            }
                            if (json[i] == '"' && !escaped) {
                                break
                            }
                            escaped = false
                            i++
                        }
                        val end = if (i < json.length) i + 1 else json.length
                        
                        // Check if this is a key (followed by colon)
                        var nextNonWs = end
                        while (nextNonWs < json.length && json[nextNonWs].isWhitespace()) {
                            nextNonWs++
                        }
                        val isKey = nextNonWs < json.length && json[nextNonWs] == ':'
                        
                        addStyle(
                            style = SpanStyle(
                                color = if (isKey) keyColor else stringColor,
                                fontWeight = if (isKey) FontWeight.Bold else FontWeight.Normal
                            ),
                            start = start,
                            end = end
                        )
                    }
                    
                    // Number detection
                    char.isDigit() || (char == '-' && i + 1 < json.length && json[i + 1].isDigit()) -> {
                        val start = i
                        i++
                        while (i < json.length && (json[i].isDigit() || json[i] == '.' || json[i] == 'e' || json[i] == 'E' || json[i] == '+' || json[i] == '-')) {
                            i++
                        }
                        addStyle(
                            style = SpanStyle(color = numberColor),
                            start = start,
                            end = i
                        )
                        continue
                    }
                    
                    // Boolean (true/false) and null
                    char.isLetter() -> {
                        val start = i
                        while (i < json.length && json[i].isLetter()) {
                            i++
                        }
                        val word = json.substring(start, i)
                        when (word) {
                            "true", "false" -> {
                                addStyle(
                                    style = SpanStyle(color = booleanColor, fontWeight = FontWeight.Bold),
                                    start = start,
                                    end = i
                                )
                            }
                            "null" -> {
                                addStyle(
                                    style = SpanStyle(color = nullColor, fontWeight = FontWeight.Bold),
                                    start = start,
                                    end = i
                                )
                            }
                        }
                        continue
                    }
                    
                    // Punctuation braces, brackets, colon, comma
                    char in "{}[],:" -> {
                        addStyle(
                            style = SpanStyle(color = punctuationColor, fontWeight = FontWeight.Bold),
                            start = i,
                            end = i + 1
                        )
                    }
                }
                i++
            }
        }
        
        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}
