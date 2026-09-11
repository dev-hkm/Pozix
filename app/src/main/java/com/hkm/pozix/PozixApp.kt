package com.hkm.pozix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hkm.pozix.navigation.PozixNavigation
import com.hkm.pozix.navigation.Screen
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
    val showBottomBar = currentRoute !in setOf(
        Screen.QuizPlayer.route,
        Screen.ExamPlayer.route
    ) && !isKeyboardOpen

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        PozixNavigation(
            navController = navController,
            onLanguageChanged = onLanguageChanged
        )

        AnimatedVisibility(
            visible = showBottomBar,
            enter = fadeIn(tween(180)) + slideInVertically(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
                initialOffsetY = { it }
            ),
            exit = fadeOut(tween(140)) + slideOutVertically(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
                targetOffsetY = { it }
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
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
