package com.hkm.pozix.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.pozix.R
import com.hkm.pozix.util.HapticUtil

/**
 * Unified AI & Prompts hub screen combining ChatGPT-style AI Assistant
 * with educational prompt templates and JSON schema generator.
 */
@Composable
fun AIPromptsScreen(
    initialTab: Int = 0,
    onNavigateToSettings: () -> Unit,
    onPlayQuiz: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var pendingPrompt by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Sleek top segmented pill controller
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab 0: AI Assistant
                        val isAi = selectedTab == 0
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isAi) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier.clickable {
                                if (!isAi) {
                                    HapticUtil.selectionTick(context)
                                    selectedTab = 0
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isAi) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.tab_ai_chat),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isAi) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAi) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Tab 1: Prompt Templates
                        val isTemplates = selectedTab == 1
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isTemplates) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier.clickable {
                                if (!isTemplates) {
                                    HapticUtil.selectionTick(context)
                                    selectedTab = 1
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.LibraryBooks,
                                    contentDescription = null,
                                    tint = if (isTemplates) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.tab_prompts),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isTemplates) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isTemplates) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Animated Tab Content Area
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it / 3 } + fadeIn() togetherWith slideOutHorizontally { -it / 3 } + fadeOut()
                } else {
                    slideInHorizontally { -it / 3 } + fadeIn() togetherWith slideOutHorizontally { it / 3 } + fadeOut()
                }
            },
            label = "AIPromptsTabTransition",
            modifier = Modifier.weight(1f)
        ) { tab ->
            when (tab) {
                0 -> AIChatScreen(
                    onNavigateBack = {},
                    onNavigateToSettings = onNavigateToSettings,
                    onPlayQuiz = onPlayQuiz,
                    initialPrompt = pendingPrompt,
                    showBackButton = false,
                    onOpenTemplates = { selectedTab = 1 }
                )
                1 -> PromptTemplatesScreen(
                    onUseInChat = { prompt ->
                        pendingPrompt = prompt
                        selectedTab = 0
                    }
                )
            }
        }
    }
}
