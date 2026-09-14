package com.hkm.pozix.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkm.pozix.R
import com.hkm.pozix.ui.theme.readableContentColorFor
import com.hkm.pozix.data.model.AiProvider
import com.hkm.pozix.ui.theme.getAvailableFonts
import com.hkm.pozix.ui.theme.getFontDisplayName
import com.hkm.pozix.ui.theme.getFontFamily
import com.hkm.pozix.ui.components.BouncyContainer
import com.hkm.pozix.ui.components.PozixModalBottomSheet
import com.hkm.pozix.util.HapticUtil
import com.hkm.pozix.viewmodel.CloudNoticeType
import com.hkm.pozix.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    onLanguageChanged: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showFontSheet by remember { mutableStateOf(false) }
    var showProviderList by remember { mutableStateOf(false) }
    var editingProvider by remember { mutableStateOf<AiProvider?>(null) }
    var deleteProviderTarget by remember { mutableStateOf<AiProvider?>(null) }
    var cloudAction by remember { mutableStateOf<String?>(null) } // "backup" or "restore"
    var showForgetTokenConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val backupJson = viewModel.exportBackup()
                    withContext(Dispatchers.IO) {
                        val output = context.contentResolver.openOutputStream(it)
                            ?: error("Unable to open backup destination")
                        output.use { stream ->
                            stream.write(backupJson.toByteArray(Charsets.UTF_8))
                        }
                    }
                    viewModel.showBackupNotice(
                        CloudNoticeType.SUCCESS,
                        context.getString(R.string.backup_export_success)
                    )
                    HapticUtil.success(context)
                } catch (_: Exception) {
                    viewModel.showBackupNotice(
                        CloudNoticeType.ERROR,
                        context.getString(R.string.backup_export_failed)
                    )
                    HapticUtil.error(context)
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val jsonContent = withContext(Dispatchers.IO) {
                        val input = context.contentResolver.openInputStream(it)
                            ?: error("Unable to open selected file")
                        input.bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() }
                    }
                    viewModel.prepareRestore(jsonContent)
                    HapticUtil.selectionTick(context)
                } catch (_: Exception) {
                    viewModel.showBackupNotice(
                        CloudNoticeType.ERROR,
                        context.getString(R.string.backup_import_failed)
                    )
                    HapticUtil.error(context)
                }
            }
        }
    }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            Column {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_version_info, com.hkm.pozix.BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "appearance") {
            SettingsCard(title = stringResource(R.string.settings_appearance_section)) {
                // Material 3 SingleChoiceSegmentedButtonRow Theme Selector
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val themeOptions = listOf(
                        "system" to stringResource(R.string.settings_theme_system),
                        "light" to stringResource(R.string.settings_theme_light),
                        "dark" to stringResource(R.string.settings_theme_dark)
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        themeOptions.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = uiState.themeMode == mode,
                                onClick = {
                                    if (uiState.themeMode != mode) {
                                        HapticUtil.selectionTick(context)
                                        viewModel.setThemeMode(mode)
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = themeOptions.size
                                ),
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = uiState.themeMode == mode)
                                },
                                label = {
                                    Text(
                                        text = label,
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_language),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(
                            text = if (uiState.language == "vi") stringResource(R.string.settings_language_vi) else stringResource(R.string.settings_language_en),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        HapticUtil.selectionTick(context)
                        showLanguageSheet = true
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_font),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(
                            text = getFontDisplayName(uiState.font),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.FontDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        HapticUtil.selectionTick(context)
                        showFontSheet = true
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_code_highlight_title),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.settings_code_highlight_desc),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.codeHighlight,
                            onCheckedChange = {
                                HapticUtil.selectionTick(context)
                                viewModel.setCodeHighlight(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        item(key = "quiz_prefs") {
            SettingsCard(title = stringResource(R.string.settings_quiz_preferences)) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_shuffle_questions),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.settings_shuffle_questions_desc),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.shuffleQuestions,
                            onCheckedChange = {
                                viewModel.setShuffleQuestions(it)
                                HapticUtil.toggle(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        viewModel.setShuffleQuestions(!uiState.shuffleQuestions)
                        HapticUtil.toggle(context)
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_shuffle_answers),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.settings_shuffle_answers_desc),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.shuffleAnswers,
                            onCheckedChange = {
                                viewModel.setShuffleAnswers(it)
                                HapticUtil.toggle(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        viewModel.setShuffleAnswers(!uiState.shuffleAnswers)
                        HapticUtil.toggle(context)
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_show_explanation),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.settings_show_explanation_desc),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.showExplanation,
                            onCheckedChange = {
                                viewModel.setShowExplanation(it)
                                HapticUtil.toggle(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        viewModel.setShowExplanation(!uiState.showExplanation)
                        HapticUtil.toggle(context)
                    }
                )
            }
        }

        item(key = "ai_assistant") {
            SettingsCard(title = stringResource(R.string.settings_ai_section)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_ai_providers_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val activeProvider = uiState.aiProviders.firstOrNull { it.id == uiState.activeProviderId }
                    if (activeProvider != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
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
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeProvider.name.ifBlank { activeProvider.baseUrl },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = activeProvider.modelId.ifBlank { activeProvider.normalizedBaseUrl() },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.ai_provider_active),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Text(
                            text = stringResource(R.string.ai_provider_no_providers),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (uiState.aiProviders.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = {
                                    HapticUtil.selectionTick(context)
                                    showProviderList = true
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.ai_provider_manage),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                        Button(
                            onClick = {
                                HapticUtil.primaryAction(context)
                                editingProvider = AiProvider(
                                    name = "",
                                    baseUrl = "https://api.openai.com/v1",
                                    apiKey = "",
                                    modelId = ""
                                )
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.ai_provider_add),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        item(key = "local_backup") {
            SettingsCard(title = stringResource(R.string.backup_on_device_title)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = stringResource(R.string.backup_on_device_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                HapticUtil.primaryAction(context)
                                exportLauncher.launch("pozix_backup.json")
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.backup_export), fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                HapticUtil.primaryAction(context)
                                importLauncher.launch(arrayOf("application/json"))
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.backup_restore), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item(key = "cloud_backup") {
            SettingsCard(title = stringResource(R.string.cloud_backup_title)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.cloud_backup_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.cloudBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    if (uiState.cloudToken.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.cloud_recovery_token),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${uiState.cloudToken.take(4)}••••${uiState.cloudToken.takeLast(4)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Recovery Token", uiState.cloudToken))
                                        HapticUtil.selectionTick(context)
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Token", modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = {
                                        HapticUtil.warning(context)
                                        showForgetTokenConfirm = true
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Forget Token",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                HapticUtil.primaryAction(context)
                                cloudAction = "backup"
                            },
                            enabled = !uiState.cloudBusy,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.cloud_backup_action), fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                HapticUtil.primaryAction(context)
                                cloudAction = "restore"
                            },
                            enabled = !uiState.cloudBusy,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.cloud_restore_action), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item(key = "about") {
            SettingsCard(title = stringResource(R.string.settings_about)) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Pozix",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.settings_developed_by),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }

    if (showLanguageSheet) {
        PozixModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_select_language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                BouncyContainer(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val languages = listOf(
                            "en" to stringResource(R.string.settings_language_en),
                            "vi" to stringResource(R.string.settings_language_vi)
                        )

                        languages.forEach { (code, name) ->
                            val selected = uiState.language == code
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        if (!selected) {
                                            HapticUtil.selectionTick(context)
                                            viewModel.setLanguage(code) {
                                                onLanguageChanged()
                                            }
                                        }
                                        showLanguageSheet = false
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) readableContentColorFor(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        ) else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showFontSheet) {
        PozixModalBottomSheet(
            onDismissRequest = { showFontSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_select_font),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                BouncyContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(getAvailableFonts()) { fontName ->
                            val isSelected = uiState.font == fontName
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        if (!isSelected) {
                                            HapticUtil.selectionTick(context)
                                            viewModel.setFont(fontName)
                                        }
                                        showFontSheet = false
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = getFontDisplayName(fontName),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontFamily = getFontFamily(fontName),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) readableContentColorFor(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            ) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Sphinx of black quartz, judge my vow",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = getFontFamily(fontName),
                                            color = if (isSelected) readableContentColorFor(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            ).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showProviderList) {
        PozixModalBottomSheet(
            onDismissRequest = { showProviderList = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_ai_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_ai_providers_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                BouncyContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.aiProviders) { provider ->
                            val isActive = provider.id == uiState.activeProviderId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        HapticUtil.selectionTick(context)
                                        viewModel.setActiveAiProvider(provider.id)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isActive) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = provider.name.ifBlank { provider.baseUrl },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isActive) readableContentColorFor(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = provider.modelId.ifBlank { provider.normalizedBaseUrl() },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isActive) readableContentColorFor(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            ).copy(alpha = 0.8f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        if (!provider.reasoningEffort.isNullOrBlank() && provider.reasoningEffort != "default") {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = stringResource(R.string.ai_provider_reasoning_label, provider.reasoningEffort.replaceFirstChar { it.uppercase() }),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    if (isActive) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(R.string.ai_provider_active),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = {
                                        HapticUtil.selectionTick(context)
                                        editingProvider = provider
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.ai_provider_edit)
                                        )
                                    }
                                    IconButton(onClick = {
                                        HapticUtil.warning(context)
                                        deleteProviderTarget = provider
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = stringResource(R.string.ai_provider_delete),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        HapticUtil.primaryAction(context)
                        editingProvider = AiProvider(
                            name = "",
                            baseUrl = "https://api.openai.com/v1",
                            apiKey = "",
                            modelId = ""
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.ai_provider_add), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    editingProvider?.let { provider ->
        AiProviderEditorSheet(
            initial = provider,
            onDismiss = { editingProvider = null },
            onSave = { updated ->
                viewModel.saveAiProvider(updated, setActive = true)
                HapticUtil.primaryAction(context)
                editingProvider = null
                showProviderList = false
            },
            onFetchModels = { baseUrl, apiKey -> viewModel.fetchProviderModels(baseUrl, apiKey) }
        )
    }

    deleteProviderTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteProviderTarget = null },
            title = {
                Text(
                    text = stringResource(R.string.ai_provider_delete_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(stringResource(R.string.ai_provider_delete_message, target.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        HapticUtil.warning(context)
                        viewModel.deleteAiProvider(target.id)
                        deleteProviderTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.ai_provider_delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteProviderTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (cloudAction != null) {
        val isBackup = cloudAction == "backup"
        var password by remember(cloudAction) { mutableStateOf("") }
        var passwordConfirm by remember(cloudAction) { mutableStateOf("") }
        var tokenInput by remember(cloudAction) { mutableStateOf("") }
        var passwordVisible by remember(cloudAction) { mutableStateOf(false) }

        PozixModalBottomSheet(
            onDismissRequest = { cloudAction = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            BouncyContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isBackup) Icons.Default.CloudUpload else Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(if (isBackup) R.string.cloud_backup_sheet_title else R.string.cloud_restore_sheet_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        if (isBackup) {
                            if (uiState.cloudToken.isBlank()) R.string.cloud_backup_first_description else R.string.cloud_backup_update_description
                        } else {
                            R.string.cloud_restore_description
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (!isBackup) {
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.cloud_recovery_token)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.cloud_password)) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide" else "Show"
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                if (isBackup && uiState.cloudToken.isBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = passwordConfirm,
                        onValueChange = { passwordConfirm = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.cloud_confirm_password)) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (isBackup) {
                            if (uiState.cloudToken.isBlank() && password != passwordConfirm) {
                                viewModel.showBackupNotice(CloudNoticeType.ERROR, "Passwords do not match")
                                HapticUtil.error(context)
                                return@Button
                            }
                            if (password.length < 8) {
                                viewModel.showBackupNotice(CloudNoticeType.ERROR, context.getString(R.string.cloud_password_too_short))
                                HapticUtil.error(context)
                                return@Button
                            }
                            HapticUtil.primaryAction(context)
                            viewModel.backupToCloud(password)
                            cloudAction = null
                        } else {
                            if (tokenInput.isBlank() || password.isBlank()) {
                                viewModel.showBackupNotice(CloudNoticeType.ERROR, context.getString(R.string.cloud_invalid_credentials))
                                HapticUtil.error(context)
                                return@Button
                            }
                            HapticUtil.primaryAction(context)
                            viewModel.restoreFromCloud(tokenInput, password)
                            cloudAction = null
                        }
                    },
                    enabled = !uiState.cloudBusy && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(if (isBackup) R.string.cloud_start_backup else R.string.cloud_decrypt_backup),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showForgetTokenConfirm) {
        AlertDialog(
            onDismissRequest = { showForgetTokenConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.cloud_new_token_confirm_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.cloud_new_token_confirm_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        HapticUtil.warning(context)
                        viewModel.forgetCloudToken()
                        showForgetTokenConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.cloud_new_token_confirm_action), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgetTokenConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProviderEditorSheet(
    initial: AiProvider,
    onDismiss: () -> Unit,
    onSave: (AiProvider) -> Unit,
    onFetchModels: suspend (baseUrl: String, apiKey: String) -> Result<List<String>>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isNew = initial.name.isBlank() && initial.modelId.isBlank()

    var name by remember { mutableStateOf(initial.name) }
    var baseUrl by remember { mutableStateOf(initial.baseUrl) }
    var apiKey by remember { mutableStateOf(initial.apiKey) }
    var modelId by remember { mutableStateOf(initial.modelId) }
    var keyVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var modelsLoading by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var showModelPicker by remember { mutableStateOf(false) }
    var reasoningEffort by remember { mutableStateOf(initial.reasoningEffort ?: "default") }
    val standardReasoning = listOf("default", "low", "medium", "high")
    var isCustomEffort by remember { mutableStateOf(reasoningEffort.lowercase() !in standardReasoning) }
    var customEffortText by remember { mutableStateOf(if (reasoningEffort.lowercase() !in standardReasoning) reasoningEffort else "") }
    var modelSearchQuery by remember { mutableStateOf("") }

    PozixModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        BouncyContainer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (isNew) R.string.ai_provider_add else R.string.ai_provider_edit
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_ai_providers_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.ai_provider_name)) },
                placeholder = { Text(stringResource(R.string.ai_provider_name_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it.trim(); error = ""; models = emptyList() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.ai_provider_base_url)) },
                placeholder = { Text(stringResource(R.string.ai_provider_base_url_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it.trim(); error = ""; models = emptyList() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.ai_provider_api_key)) },
                placeholder = { Text(stringResource(R.string.ai_provider_api_key_hint)) },
                singleLine = true,
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    Row {
                        if (apiKey.isNotBlank()) {
                            IconButton(onClick = { apiKey = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (keyVisible) "Hide" else "Show"
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = modelId,
                onValueChange = { modelId = it.trim(); error = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.ai_provider_model)) },
                placeholder = { Text(stringResource(R.string.ai_provider_model_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    scope.launch {
                        modelsLoading = true
                        error = ""
                        val result = onFetchModels(baseUrl, apiKey)
                        modelsLoading = false
                        result.fold(
                            onSuccess = { list ->
                                models = list
                                // Auto-adopt the typed model if the server knows it.
                                if (modelId.isBlank() && list.isNotEmpty()) {
                                    modelId = list.first()
                                }
                                modelSearchQuery = ""
                                showModelPicker = true
                                HapticUtil.selectionTick(context)
                            },
                            onFailure = { e ->
                                error = e.message ?: context.getString(R.string.ai_provider_models_empty)
                                HapticUtil.error(context)
                            }
                        )
                    }
                },
                enabled = !modelsLoading && AiProvider.isPlausibleBaseUrl(baseUrl),
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (modelsLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.ai_provider_models_loading))
                } else {
                    Text(stringResource(R.string.ai_provider_fetch_models), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.ai_provider_reasoning_effort),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.ai_provider_reasoning_effort_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val reasoningOptions = listOf(
                    "default" to stringResource(R.string.ai_reasoning_default),
                    "low" to stringResource(R.string.ai_reasoning_low),
                    "medium" to stringResource(R.string.ai_reasoning_medium),
                    "high" to stringResource(R.string.ai_reasoning_high),
                    "custom" to stringResource(R.string.ai_reasoning_custom)
                )
                for ((key, label) in reasoningOptions) {
                    val selected = if (key == "custom") isCustomEffort else (!isCustomEffort && reasoningEffort == key)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (key == "custom") {
                                isCustomEffort = true
                                if (customEffortText.isNotBlank()) {
                                    reasoningEffort = customEffortText.trim()
                                }
                            } else {
                                isCustomEffort = false
                                reasoningEffort = key
                            }
                            HapticUtil.selectionTick(context)
                        },
                        label = { Text(label, fontSize = 10.sp, maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            AnimatedVisibility(visible = isCustomEffort) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = customEffortText,
                        onValueChange = {
                            customEffortText = it
                            reasoningEffort = it.trim()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.ai_reasoning_custom)) },
                        placeholder = { Text(stringResource(R.string.ai_reasoning_custom_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (error.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val normalized = AiProvider.normalizeBaseUrl(baseUrl)
                        if (name.isBlank() ||
                            !AiProvider.isPlausibleBaseUrl(normalized) ||
                            modelId.isBlank()
                        ) {
                            error = context.getString(R.string.ai_provider_invalid)
                            HapticUtil.error(context)
                            return@Button
                        }
                        val finalReasoningEffort = if (isCustomEffort) {
                            customEffortText.trim().takeIf { it.isNotBlank() }
                        } else {
                            reasoningEffort.takeIf { it != "default" }
                        }
                        onSave(
                            initial.copy(
                                name = name.trim(),
                                baseUrl = normalized,
                                apiKey = apiKey.trim(),
                                modelId = modelId.trim(),
                                reasoningEffort = finalReasoningEffort
                            )
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.ai_provider_save), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showModelPicker && models.isNotEmpty()) {
        PozixModalBottomSheet(
            onDismissRequest = { showModelPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.ai_provider_pick_model),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = modelSearchQuery,
                    onValueChange = { modelSearchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.ai_provider_search_models)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (modelSearchQuery.isNotBlank()) {
                            IconButton(onClick = { modelSearchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                val filteredModels = remember(models, modelSearchQuery) {
                    if (modelSearchQuery.isBlank()) models
                    else models.filter { it.contains(modelSearchQuery.trim(), ignoreCase = true) }
                }

                if (filteredModels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.ai_provider_no_models_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    BouncyContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredModels) { model ->
                                val selected = model == modelId
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            modelId = model
                                            showModelPicker = false
                                            HapticUtil.selectionTick(context)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = model,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) readableContentColorFor(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
