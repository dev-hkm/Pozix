package com.hkm.pozix.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkm.pozix.R
import com.hkm.pozix.util.HapticUtil
import com.hkm.pozix.viewmodel.PromptItem
import com.hkm.pozix.viewmodel.PromptTemplatesViewModel
import kotlinx.coroutines.delay

@Composable
fun PromptTemplatesScreen(
    viewModel: PromptTemplatesViewModel = viewModel(),
    onBack: (() -> Unit)? = null,
    onUseInChat: ((String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp)
    ) {
        if (onBack != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        HapticUtil.lightTap(context)
                        onBack()
                    },
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.ai_chat_back),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.prompts_back_to_ai),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.prompts_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.prompts_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // =========================================================================
        // JSON SCHEMA & AI PLANNING SECTION
        // Shows full importable JSON structure and AI prompt template with copy buttons
        // =========================================================================
        JsonSchemaPromptCard(onUseInChat = onUseInChat)

        Spacer(modifier = Modifier.height(24.dp))

        // Preset Prompts Section
        Text(
            text = stringResource(R.string.prompts_preset),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        uiState.presetPrompts.forEach { prompt ->
            PromptCard(
                prompt = prompt,
                onToggleExpand = { viewModel.toggleExpand(prompt.id) },
                onCopy = {
                    copyToClipboard(context, prompt.content)
                    HapticUtil.selectionTick(context)
                },
                onUseInChat = onUseInChat?.let { callback -> { callback(prompt.content) } }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Custom Prompt Builder
        Text(
            text = stringResource(R.string.prompts_custom),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                OutlinedTextField(
                    value = uiState.customTopic,
                    onValueChange = { viewModel.updateCustomTopic(it) },
                    label = { Text(stringResource(R.string.prompts_custom_topic)) },
                    placeholder = { Text(stringResource(R.string.prompts_custom_topic_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { viewModel.generateCustomPrompt() },
                    enabled = uiState.customTopic.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.prompts_custom_generate))
                }
                
                if (uiState.generatedPrompt.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = uiState.generatedPrompt,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    var customCopied by remember { mutableStateOf(false) }
                    
                    if (onUseInChat != null) {
                        Button(
                            onClick = {
                                onUseInChat(uiState.generatedPrompt)
                                HapticUtil.primaryAction(context)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.prompts_use_in_chat))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    FilledTonalButton(
                        onClick = {
                            copyToClipboard(context, uiState.generatedPrompt)
                            HapticUtil.veryLightTap(context)
                            customCopied = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = if (customCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (customCopied) stringResource(R.string.prompts_copied) else stringResource(R.string.prompts_copy))
                    }
                    
                    LaunchedEffect(customCopied) {
                        if (customCopied) {
                            delay(2000)
                            customCopied = false
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PromptCard(
    prompt: PromptItem,
    onToggleExpand: () -> Unit,
    onCopy: () -> Unit,
    onUseInChat: (() -> Unit)? = null
) {
    var copied by remember { mutableStateOf(false) }
    
    val rotation by animateFloatAsState(
        targetValue = if (prompt.isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "rotation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title, copy icon, and expand icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = prompt.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                // Inline copy icon
                IconButton(
                    onClick = {
                        onCopy()
                        copied = true
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.prompts_copy),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Expand/collapse icon
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (prompt.isExpanded) stringResource(R.string.prompts_collapse) else stringResource(R.string.prompts_expand),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }
            
            // Expanded content
            AnimatedVisibility(
                visible = prompt.isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = prompt.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    if (onUseInChat != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = {
                                onUseInChat()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.prompts_use_in_chat),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            
            LaunchedEffect(copied) {
                if (copied) {
                    delay(2000)
                    copied = false
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Pozix Prompt", text)
    clipboard.setPrimaryClip(clip)
}

@Composable
fun JsonSchemaPromptCard(
    onUseInChat: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var copiedSchema by remember { mutableStateOf(false) }
    var copiedPrompt by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    val jsonSchemaExample = """{
  "title": "Quiz Title (e.g. World History Trivia)",
  "description": "Short description of topic or difficulty (Optional)",
  "language": "en",
  "questions": [
    {
      "type": "single_choice",
      "question": "What is the capital of Australia?",
      "options": [
        "Sydney",
        "Melbourne",
        "Canberra",
        "Brisbane"
      ],
      "correctIndex": 2,
      "explanation": "Canberra was chosen as the capital in 1908."
    },
    {
      "type": "true_false",
      "question": "The Great Wall of China is visible from the Moon with naked eyes.",
      "correctAnswer": false,
      "explanation": "NASA confirmed it is not visible without optical aid."
    }
  ]
}"""

    val aiPlanningPrompt = """You are an expert quiz creator for the Pozix quiz app. Help me plan and create an engaging quiz set.

CRITICAL INSTRUCTION: Output ONLY raw, parseable JSON matching the exact schema below. Do not wrap in extra markdown if possible, and do not add explanatory text before or after the JSON.

JSON SCHEMA:
$jsonSchemaExample

SCHEMA RULES:
1. "type": must be "single_choice" or "true_false".
2. "single_choice": must have 2-6 "options", and "correctIndex" (0-indexed integer).
3. "true_false": must have "correctAnswer" (boolean: true or false).
4. "explanation": optional clear, educational rationale for why the answer is correct.

Please ask me what topic, target difficulty, and question count I would like, or generate the quiz directly if I specify the topic!"""

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.prompts_json_schema_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.prompts_json_schema_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Code Preview Box (Syntax-like dark surface)
            var schemaExpanded by remember { mutableStateOf(false) }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                onClick = { schemaExpanded = !schemaExpanded }
            ) {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        text = jsonSchemaExample,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (schemaExpanded) Int.MAX_VALUE else 14,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons - Stacked full-width layout prevents any text wrapping or truncation bugs in both English and Vietnamese
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onUseInChat != null) {
                    Button(
                        onClick = {
                            onUseInChat(aiPlanningPrompt)
                            HapticUtil.primaryAction(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 44.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.prompts_use_in_chat),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                FilledTonalButton(
                    onClick = {
                        copyToClipboard(context, jsonSchemaExample)
                        HapticUtil.primaryAction(context)
                        copiedSchema = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (copiedSchema) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (copiedSchema) stringResource(R.string.prompts_copied) else stringResource(R.string.prompts_copy_schema),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        copyToClipboard(context, aiPlanningPrompt)
                        HapticUtil.primaryAction(context)
                        copiedPrompt = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (copiedPrompt) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (copiedPrompt) stringResource(R.string.prompts_copied) else stringResource(R.string.prompts_copy_ai_prompt),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            LaunchedEffect(copiedSchema) {
                if (copiedSchema) {
                    delay(2000)
                    copiedSchema = false
                }
            }

            LaunchedEffect(copiedPrompt) {
                if (copiedPrompt) {
                    delay(2000)
                    copiedPrompt = false
                }
            }
        }
    }
}
