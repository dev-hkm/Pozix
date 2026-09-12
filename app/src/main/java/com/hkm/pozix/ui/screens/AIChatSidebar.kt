package com.hkm.pozix.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkm.pozix.R
import com.hkm.pozix.data.model.ChatSession
import com.hkm.pozix.data.model.AiProvider
import com.hkm.pozix.ui.components.PozixModalBottomSheet
import com.hkm.pozix.viewmodel.SettingsViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path

internal val NewChatIcon: ImageVector by lazy {
    ImageVector.Builder("NewChat", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(11f, 4f); lineTo(6f, 4f)
            curveTo(4.3f, 4f, 3f, 5.3f, 3f, 7f)
            lineTo(3f, 18f); curveTo(3f, 19.7f, 4.3f, 21f, 6f, 21f)
            lineTo(17f, 21f); curveTo(18.7f, 21f, 20f, 19.7f, 20f, 18f)
            lineTo(20f, 13f)
            moveTo(9f, 15f); lineTo(10f, 11f); lineTo(18f, 3f)
            curveTo(19f, 2f, 22f, 5f, 21f, 6f)
            lineTo(13f, 14f); close()
            moveTo(16.5f, 4.5f); lineTo(19.5f, 7.5f)
        }
    }.build()
}

@Composable
internal fun ChatSidebar(
    sessions: List<ChatSession>,
    currentId: String?,
    onSelect: (String) -> Unit,
    onTemplates: () -> Unit,
    onProviders: () -> Unit,
    onPin: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var optionsId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    val filtered = remember(sessions, query) {
        sessions.filter { it.title.contains(query.trim(), ignoreCase = true) }
            .sortedByDescending { it.updatedAt }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Zix Bot", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.ai_chat_templates)) },
            selected = false, onClick = onTemplates,
            icon = { Icon(Icons.Default.AutoAwesomeMotion, null) }
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.ai_chat_manage_providers)) },
            selected = false, onClick = onProviders,
            icon = { Icon(Icons.Default.Tune, null) }
        )
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text(stringResource(R.string.ai_sidebar_search)) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        )
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (filtered.isEmpty()) {
                item { Text(stringResource(R.string.ai_sidebar_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)) }
            }
            listOf(true, false).forEach { pinned ->
                val group = filtered.filter { it.isPinned == pinned }
                if (group.isNotEmpty()) {
                    item(key = "section_$pinned") {
                        Text(stringResource(if (pinned) R.string.ai_sidebar_pinned else R.string.ai_sidebar_recent),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp))
                    }
                    items(group, key = { it.id }) { session ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (session.id == currentId) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { onSelect(session.id) },
                                onLongClick = { optionsId = session.id })
                        ) {
                            Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (pinned) Icons.Default.PushPin else Icons.AutoMirrored.Filled.Chat,
                                    null, modifier = Modifier.size(18.dp))
                                Text(session.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                                IconButton(onClick = { optionsId = session.id }) {
                                    Icon(Icons.Default.MoreHoriz, stringResource(R.string.ai_chat_options))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    sessions.firstOrNull { it.id == optionsId }?.let { session ->
        AlertDialog(
            onDismissRequest = { optionsId = null },
            title = { Text(session.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    TextButton(onClick = { onPin(session.id); optionsId = null }) {
                        Icon(Icons.Default.PushPin, null); Spacer(Modifier.width(12.dp))
                        Text(stringResource(if (session.isPinned) R.string.ai_sidebar_unpin else R.string.ai_sidebar_pin))
                    }
                    TextButton(onClick = { title = session.title; renameId = session.id; optionsId = null }) {
                        Icon(Icons.Default.Edit, null); Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.saved_quiz_sets_rename))
                    }
                    TextButton(onClick = { deleteId = session.id; optionsId = null }) {
                        Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.saved_quiz_sets_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {}
        )
    }
    renameId?.let { id ->
        AlertDialog(onDismissRequest = { renameId = null },
            title = { Text(stringResource(R.string.saved_quiz_sets_rename)) },
            text = { OutlinedTextField(value = title, onValueChange = { title = it.take(120) },
                singleLine = true, label = { Text(stringResource(R.string.saved_quiz_sets_name_label)) }) },
            confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = {
                onRename(id, title); renameId = null
            }) { Text(stringResource(R.string.saved_quiz_sets_rename)) } },
            dismissButton = { TextButton(onClick = { renameId = null }) { Text(stringResource(R.string.cancel)) } })
    }
    deleteId?.let { id ->
        AlertDialog(onDismissRequest = { deleteId = null },
            title = { Text(stringResource(R.string.ai_chat_delete_this)) },
            text = { Text(stringResource(R.string.ai_sidebar_delete_warning)) },
            confirmButton = { TextButton(onClick = { onDelete(id); deleteId = null }) {
                Text(stringResource(R.string.saved_quiz_sets_delete), color = MaterialTheme.colorScheme.error)
            } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatProviderManager(onDismiss: () -> Unit) {
    // Same repository, editor, model discovery and custom settings as Settings.
    val settings: SettingsViewModel = viewModel()
    val state by settings.uiState.collectAsState()
    var editing by remember { mutableStateOf<AiProvider?>(null) }
    var deleting by remember { mutableStateOf<AiProvider?>(null) }
    if (editing == null) {
        PozixModalBottomSheet(onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding()) {
                Text(stringResource(R.string.ai_chat_manage_providers), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.settings_ai_providers_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    if (state.aiProviders.isEmpty()) item {
                        Text(stringResource(R.string.ai_provider_no_providers), modifier = Modifier.padding(vertical = 20.dp))
                    }
                    items(state.aiProviders, key = { it.id }) { provider ->
                        val active = state.activeProviderId == provider.id
                        Surface(onClick = { settings.setActiveAiProvider(provider.id) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (active) Icons.Default.CheckCircle else Icons.Default.CloudQueue, null)
                                Column(Modifier.weight(1f).padding(10.dp)) {
                                    Text(provider.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(provider.modelId, style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { editing = provider }) {
                                    Icon(Icons.Default.Edit, stringResource(R.string.ai_provider_edit))
                                }
                                IconButton(onClick = { deleting = provider }) {
                                    Icon(Icons.Default.DeleteOutline, stringResource(R.string.ai_provider_delete))
                                }
                            }
                        }
                    }
                }
                FilledTonalButton(onClick = {
                    editing = AiProvider(name = "", baseUrl = "", apiKey = "", modelId = "")
                }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ai_provider_add))
                }
            }
        }
    }
    editing?.let { provider ->
        AiProviderEditorSheet(initial = provider, onDismiss = { editing = null },
            onSave = { settings.saveAiProvider(it, setActive = true); editing = null },
            onFetchModels = { url, key -> settings.fetchProviderModels(url, key) })
    }
    deleting?.let { provider ->
        AlertDialog(onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.ai_provider_delete_title)) },
            text = { Text(stringResource(R.string.ai_provider_delete_message, provider.name)) },
            confirmButton = { TextButton(onClick = { settings.deleteAiProvider(provider.id); deleting = null }) {
                Text(stringResource(R.string.ai_provider_delete))
            } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } })
    }
}
