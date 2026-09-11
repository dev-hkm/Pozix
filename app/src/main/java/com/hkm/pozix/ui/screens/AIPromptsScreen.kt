package com.hkm.pozix.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Unified AI & Prompts hub screen seamlessly hosting ChatGPT-style AI Assistant
 * and Prompt Templates with 1-tap switching and zero header clutter.
 */
@Composable
fun AIPromptsScreen(
    initialTab: Int = 0,
    onNavigateBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onPlayQuiz: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var pendingPrompt by remember { mutableStateOf<String?>(null) }

    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = {
            if (targetState > initialState) {
                slideInHorizontally { it / 3 } + fadeIn() togetherWith slideOutHorizontally { -it / 3 } + fadeOut()
            } else {
                slideInHorizontally { -it / 3 } + fadeIn() togetherWith slideOutHorizontally { it / 3 } + fadeOut()
            }
        },
        label = "AIPromptsTransition",
        modifier = Modifier.fillMaxSize()
    ) { tab ->
        when (tab) {
            0 -> AIChatScreen(
                onNavigateBack = onNavigateBack,
                onNavigateToSettings = onNavigateToSettings,
                onPlayQuiz = onPlayQuiz,
                initialPrompt = pendingPrompt,
                showBackButton = true,
                onOpenTemplates = { selectedTab = 1 }
            )
            1 -> PromptTemplatesScreen(
                onBack = { selectedTab = 0 },
                onUseInChat = { prompt ->
                    pendingPrompt = prompt
                    selectedTab = 0
                }
            )
        }
    }
}
