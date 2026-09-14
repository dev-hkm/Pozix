package com.hkm.pozix.ui.screens

import androidx.activity.compose.BackHandler
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
    onPlayQuiz: () -> Unit,
    onOpenLecture: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var pendingPrompt by remember { mutableStateOf<String?>(null) }

    // Intercept back button when in Prompt Templates (tab 1) to return to AI Chat (tab 0)
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = {
            val enterSpec = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntOffset>(
                dampingRatio = 0.84f,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
            val fadeSpec = androidx.compose.animation.core.tween<Float>(280)
            if (targetState > initialState) {
                (slideInHorizontally(enterSpec) { it / 3 } + fadeIn(fadeSpec))
                    .togetherWith(slideOutHorizontally(enterSpec) { -it / 3 } + fadeOut(fadeSpec))
            } else {
                (slideInHorizontally(enterSpec) { -it / 3 } + fadeIn(fadeSpec))
                    .togetherWith(slideOutHorizontally(enterSpec) { it / 3 } + fadeOut(fadeSpec))
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
                onOpenLecture = onOpenLecture,
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
