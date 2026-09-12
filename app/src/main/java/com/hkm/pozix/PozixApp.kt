package com.hkm.pozix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hkm.pozix.navigation.PozixNavigation
import com.hkm.pozix.navigation.Screen
import com.hkm.pozix.util.SharedImportManager
import androidx.compose.animation.core.FastOutSlowInEasing
import com.hkm.pozix.ui.components.BottomNavBar

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun PozixApp(
    onLanguageChanged: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Library.route
    val isKeyboardOpen = WindowInsets.isImeVisible
    val context = androidx.compose.ui.platform.LocalContext.current
    val reviewDraft by com.hkm.pozix.util.QuizAiFollowUp.pending.collectAsState()
    LaunchedEffect(Unit) { com.hkm.pozix.util.QuizAiFollowUp.restore(context) }
    LaunchedEffect(reviewDraft) {
        if (reviewDraft != null && currentRoute != Screen.AIChat.route) {
            navController.navigate(Screen.AIChat.route) { launchSingleTop = true }
        }
    }

    val pendingExternalJson by SharedImportManager.pendingJson.collectAsState()
    LaunchedEffect(pendingExternalJson) {
        if (!pendingExternalJson.isNullOrBlank()) {
            navController.navigate(Screen.Import.route) {
                popUpTo(Screen.Library.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    // Hide bottom navigation completely when in AI Chat, Quiz/Exam player, or when typing
    val showBottomBar = currentRoute !in setOf(
        Screen.QuizPlayer.route,
        Screen.ExamPlayer.route,
        Screen.Templates.route,
        Screen.AIChat.route
    ) && !isKeyboardOpen

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PozixNavigation(
            navController = navController,
            onLanguageChanged = onLanguageChanged
        )

        AnimatedVisibility(
            visible = showBottomBar,
            enter = fadeIn(tween(180)) + slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                initialOffsetY = { it }
            ),
            exit = fadeOut(tween(140)) + slideOutVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                targetOffsetY = { it }
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(Screen.Library.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    }
}
