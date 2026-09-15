package com.hkm.pozix.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.OutlinedButton
import com.hkm.pozix.util.LectureManager
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
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
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkm.pozix.R
import com.hkm.pozix.util.HapticUtil
import com.hkm.pozix.util.SharedImportManager
import com.hkm.pozix.viewmodel.ImportViewModel
import com.hkm.pozix.viewmodel.ValidationState
import com.hkm.pozix.ui.theme.readableContentColorFor
import kotlinx.coroutines.launch

private const val SAMPLE_STEM_JSON = """{
  "title": "Toán & KHTN Mẫu",
  "description": "Đề trắc nghiệm mẫu có công thức LaTeX và Code",
  "language": "vi",
  "questions": [
    {
      "type": "single_choice",
      "question": "Cho hàm số ${'$'}f(x) = x \\cdot \\ln x${'$'} với ${'$'}x > 0${'$'}. Đạo hàm ${'$'}f'(x)${'$'} bằng:",
      "options": [
        "${'$'}f'(x) = \\ln x + 1${'$'}",
        "${'$'}f'(x) = \\frac{1}{x}${'$'}",
        "${'$'}f'(x) = 1${'$'}",
        "${'$'}f'(x) = \\ln x${'$'}"
      ],
      "correctIndex": 0,
      "explanation": "Áp dụng công thức đạo hàm tích ${'$'}(uv)' = u'v + uv'${'$'}: ${'$'}f'(x) = 1 \\cdot \\ln x + x \\cdot \\frac{1}{x} = \\ln x + 1${'$'}."
    },
    {
      "type": "true_false",
      "question": "Phương trình bậc hai ${'$'}ax^2 + bx + c = 0${'$'} (${'$'}a \\neq 0${'$'}) có nghiệm khi biệt thức ${'$'}\\Delta = b^2 - 4ac \\ge 0${'$'}.",
      "correctAnswer": true,
      "explanation": "Khi ${'$'}\\Delta \\ge 0${'$'}, phương trình luôn có ít nhất một nghiệm thực."
    }
  ]
}"""

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
fun ImportScreen(
    onQuizLoaded: () -> Unit,
    onPlayQuiz: (() -> Unit)? = null,
    onOpenLecture: (() -> Unit)? = null,
    viewModel: ImportViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    var showLinkImport by remember { mutableStateOf(false) }

    val pendingExternalJson by SharedImportManager.pendingJson.collectAsState()
    LaunchedEffect(pendingExternalJson) {
        val json = pendingExternalJson
        if (!json.isNullOrBlank()) {
            viewModel.smartPasteAndValidate(json)
            SharedImportManager.pendingJson.value = null
            coroutineScope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    // JSON file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importJsonFromFile(it, context.contentResolver)
            HapticUtil.actionConfirm(context)
        }
    }

    fun doSmartPasteAndValidate() {
        val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = sysClipboard.primaryClip
        val text = if (clipData != null && clipData.itemCount > 0) {
            clipData.getItemAt(0).text?.toString().orEmpty()
        } else ""
        if (text.isNotBlank()) {
            viewModel.smartPasteAndValidate(text)
            HapticUtil.primaryAction(context)
            coroutineScope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        } else {
            Toast.makeText(context, context.getString(R.string.import_empty_json), Toast.LENGTH_SHORT).show()
        }
    }

    val isKeyboardOpen = WindowInsets.isImeVisible
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp

    val floatingBarBottomPadding by animateDpAsState(
        targetValue = if (isKeyboardOpen) 14.dp else 78.dp + navBarBottom,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "floating_bar_bottom_padding"
    )
    val contentBottomPadding by animateDpAsState(
        targetValue = if (isKeyboardOpen) 80.dp else 140.dp + navBarBottom,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "content_bottom_padding"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, top = topPadding, bottom = contentBottomPadding),
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

            // Quick Chips Flow Row
            val chipScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File picker chip
                ElevatedFilterChip(
                    selected = false,
                    onClick = {
                        HapticUtil.actionConfirm(context)
                        filePickerLauncher.launch(arrayOf("application/json", "text/json", "text/plain", "*/*"))
                    },
                    label = { Text(stringResource(R.string.import_choose_file), fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                // Standard paste chip
                ElevatedFilterChip(
                    selected = false,
                    onClick = {
                        val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = sysClipboard.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val text = clipData.getItemAt(0).text?.toString() ?: ""
                            if (text.isNotBlank()) {
                                viewModel.updateJsonText(text)
                                HapticUtil.actionConfirm(context)
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.import_paste), fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                // Sample STEM chip
                ElevatedFilterChip(
                    selected = false,
                    onClick = {
                        HapticUtil.selectionTick(context)
                        viewModel.updateJsonText(SAMPLE_STEM_JSON)
                    },
                    label = { Text(stringResource(R.string.import_sample_stem_json), fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DataObject,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                // Link import chip
                ElevatedFilterChip(
                    selected = showLinkImport,
                    onClick = {
                        HapticUtil.selectionTick(context)
                        showLinkImport = !showLinkImport
                    },
                    label = { Text(stringResource(R.string.import_link_title), fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = readableContentColorFor(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Imported File Banner
            if (uiState.importedFileName != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
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
                                fontWeight = FontWeight.SemiBold,
                                color = readableContentColorFor(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                        .compositeOver(MaterialTheme.colorScheme.background),
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.clearJson()
                                HapticUtil.toggle(context)
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = readableContentColorFor(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                        .compositeOver(MaterialTheme.colorScheme.background),
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                ),
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
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
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
                                color = readableContentColorFor(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearFileError() },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = readableContentColorFor(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Share Link Import Card (Expandable)
            AnimatedVisibility(
                visible = showLinkImport,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            IconButton(
                                onClick = {
                                    HapticUtil.selectionTick(context)
                                    showLinkImport = false
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.shareLink,
                            onValueChange = viewModel::updateShareLink,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.import_share_link)) },
                            placeholder = { Text("https://...") },
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

            // Syntax-Highlighted JSON Editor Card (With Expressive Toolbar)
            val editorScroll = rememberScrollState()
            val linesCount = if (uiState.jsonText.isBlank()) 0 else uiState.jsonText.lines().size
            val charsCount = uiState.jsonText.length

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Editor Top Toolbar (Header)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Badge & stats
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "{ } JSON",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (charsCount > 0) {
                                Text(
                                    text = stringResource(R.string.import_editor_stats, linesCount, charsCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Toolbar quick actions
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Format / Beautify JSON button
                            TextButton(
                                onClick = {
                                    val formatted = viewModel.formatJsonText()
                                    if (formatted) {
                                        HapticUtil.actionConfirm(context)
                                        Toast.makeText(context, context.getString(R.string.import_format_success), Toast.LENGTH_SHORT).show()
                                    } else {
                                        HapticUtil.toggle(context)
                                        Toast.makeText(context, context.getString(R.string.import_format_failed), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = uiState.jsonText.isNotBlank(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatAlignLeft,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.import_format_json),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Copy JSON button
                            IconButton(
                                onClick = {
                                    if (uiState.jsonText.isNotBlank()) {
                                        clipboardManager.setText(AnnotatedString(uiState.jsonText))
                                        HapticUtil.actionConfirm(context)
                                        Toast.makeText(context, context.getString(R.string.import_copied_toast), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = uiState.jsonText.isNotBlank(),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.import_copy_json),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Clear editor button
                            IconButton(
                                onClick = {
                                    viewModel.clearJson()
                                    HapticUtil.toggle(context)
                                },
                                enabled = uiState.jsonText.isNotBlank(),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.import_clear),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Divider line
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ) {}

                    // Text editor container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
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
            }

            // Validation Result (Expressive M3 Cards with Quick-Play)
            AnimatedVisibility(
                visible = uiState.validationState !is ValidationState.Idle,
                enter = fadeIn() + slideInVertically() + expandVertically(),
                exit = fadeOut() + slideOutVertically() + shrinkVertically()
            ) {
                when (val state = uiState.validationState) {
                    is ValidationState.Success -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                            ),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            val successTextColor = readableContentColorFor(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                                    .compositeOver(MaterialTheme.colorScheme.background),
                                MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.import_success),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = successTextColor
                                        )
                                        Text(
                                            text = state.result.quiz.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = successTextColor
                                        )
                                    }
                                }

                                if (!state.result.quiz.description.isNullOrBlank()) {
                                    Text(
                                        text = state.result.quiz.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = successTextColor.copy(alpha = 0.85f)
                                    )
                                }

                                // Badges Row (Questions count, SC, TF)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${state.result.parsedQuestions.size}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = stringResource(R.string.saved_quiz_sets_questions),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${state.result.singleChoiceCount}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = stringResource(R.string.saved_quiz_sets_single_choice),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${state.result.trueFalseCount}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = stringResource(R.string.saved_quiz_sets_true_false),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (state.result.shortAnswerCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "${state.result.shortAnswerCount}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = stringResource(R.string.saved_quiz_sets_short_answer),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!state.result.quiz.lecture.isNullOrBlank()) {
                                    val isPreparingLecture by LectureManager.isPreparing.collectAsState()
                                    OutlinedButton(
                                        onClick = {
                                            HapticUtil.lightTap(context)
                                            LectureManager.openLectureWithPreload(
                                                title = state.result.quiz.title,
                                                content = state.result.quiz.lecture,
                                                scope = coroutineScope,
                                                onReady = { onOpenLecture?.invoke() }
                                            )
                                        },
                                        enabled = !isPreparingLecture,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp)
                                            .height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    ) {
                                        if (isPreparingLecture) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.lecture_loading),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        } else {
                                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.lecture_open_button),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }

                                // Instant Action Buttons: "Luyện tập ngay" & "Lưu vào máy"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Play Now (Fastest 1-tap operation)
                                    Button(
                                        onClick = {
                                            HapticUtil.success(context)
                                            viewModel.loadQuizWithoutSaving {
                                                if (onPlayQuiz != null) {
                                                    onPlayQuiz()
                                                } else {
                                                    onQuizLoaded()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.import_play_now),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }

                                    // Save to Library Button
                                    FilledTonalButton(
                                        onClick = {
                                            HapticUtil.primaryAction(context)
                                            viewModel.showSaveDialog()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SaveAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.import_save_library),
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is ValidationState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            val errorTextColor = readableContentColorFor(
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.onErrorContainer
                            )
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
                                        color = errorTextColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = errorTextColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    else -> {}
                }
            }

        }

        // FLOATING ACTION BAR: Floating pills above keyboard / bottom nav with transparent background!
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = floatingBarBottomPadding)
        ) {
            AnimatedContent(
                targetState = uiState.validationState is ValidationState.Success,
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically { it / 2 })
                        .togetherWith(fadeOut(tween(150)) + slideOutVertically { -it / 2 })
                },
                label = "floating_import_actions"
            ) { isSuccess ->
                if (isSuccess) {
                    // Validated Quiz: Instant Play Now & Save Buttons
                    Row(
                        modifier = Modifier.wrapContentWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                HapticUtil.success(context)
                                viewModel.loadQuizWithoutSaving {
                                    if (onPlayQuiz != null) onPlayQuiz() else onQuizLoaded()
                                }
                            },
                            modifier = Modifier.height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.import_play_now),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                HapticUtil.primaryAction(context)
                                viewModel.showSaveDialog()
                            },
                            modifier = Modifier.height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp, pressedElevation = 1.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SaveAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.import_save_library),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // Editing / Blank / Error State: Validate & Quick Edit Actions
                    Row(
                        modifier = Modifier.wrapContentWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.jsonText.isNotBlank()) {
                            // Primary Validate Button
                            Button(
                                onClick = {
                                    viewModel.validateJson()
                                    HapticUtil.primaryAction(context)
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(scrollState.maxValue)
                                    }
                                },
                                enabled = !uiState.isLoading,
                                modifier = Modifier.height(46.dp),
                                shape = RoundedCornerShape(23.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.import_loading_shared),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.import_validate),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Format Button (Circular Floating Pill)
                            Surface(
                                modifier = Modifier.size(46.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                tonalElevation = 4.dp,
                                shadowElevation = 5.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                IconButton(
                                    onClick = {
                                        val formatted = viewModel.formatJsonText()
                                        if (formatted) {
                                            HapticUtil.actionConfirm(context)
                                            Toast.makeText(context, context.getString(R.string.import_format_success), Toast.LENGTH_SHORT).show()
                                        } else {
                                            HapticUtil.toggle(context)
                                            Toast.makeText(context, context.getString(R.string.import_format_failed), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatAlignLeft,
                                        contentDescription = stringResource(R.string.import_format_json),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Clear Button (Circular Floating Pill)
                            Surface(
                                modifier = Modifier.size(46.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                tonalElevation = 4.dp,
                                shadowElevation = 5.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.clearJson()
                                        HapticUtil.toggle(context)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.import_clear),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            // When editor is blank: Floating Smart Paste
                            Button(
                                onClick = { doSmartPasteAndValidate() },
                                modifier = Modifier.height(46.dp),
                                shape = RoundedCornerShape(23.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.import_smart_paste),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Fully Responsive, Edge-to-Edge Save Dialog (No layout breaks or navigation bar cutoffs)
    if (uiState.showSaveDialog) {
        Dialog(
            onDismissRequest = { viewModel.hideSaveDialog() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val view = LocalView.current
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            DisposableEffect(view, isDark) {
                val window = (view.parent as? DialogWindowProvider)?.window
                if (window != null) {
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    @Suppress("DEPRECATION")
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isStatusBarContrastEnforced = false
                        window.isNavigationBarContrastEnforced = false
                    }
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = !isDark
                    insetsController.isAppearanceLightNavigationBars = !isDark
                }
                onDispose {}
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(22.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Dialog Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(42.dp)
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

                        // Dialog Actions Vertical Stack
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    HapticUtil.success(context)
                                    viewModel.saveAndLoadQuiz {
                                        if (onPlayQuiz != null) {
                                            onPlayQuiz()
                                        } else {
                                            onQuizLoaded()
                                        }
                                    }
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
                                    viewModel.saveOnlyQuiz {
                                        onQuizLoaded()
                                    }
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
                                        viewModel.loadQuizWithoutSaving {
                                            if (onPlayQuiz != null) {
                                                onPlayQuiz()
                                            } else {
                                                onQuizLoaded()
                                            }
                                        }
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
