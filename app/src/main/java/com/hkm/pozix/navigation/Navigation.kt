package com.hkm.pozix.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hkm.pozix.ui.screens.AIPromptsScreen
import com.hkm.pozix.ui.screens.ExamPlayerScreen
import com.hkm.pozix.ui.screens.ImportScreen
import com.hkm.pozix.ui.screens.QuizPlayerScreen
import com.hkm.pozix.ui.screens.SavedQuizSetsScreen
import com.hkm.pozix.ui.screens.SettingsScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hkm.pozix.R
import com.hkm.pozix.ui.screens.LectureDetailScreen
import com.hkm.pozix.util.LectureManager


sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Templates : Screen("templates")
    object Import : Screen("import")
    object Settings : Screen("settings")
    object QuizPlayer : Screen("quiz_player")
    object ExamPlayer : Screen("exam_player")
    object AIChat : Screen("ai_chat")
    object Home : Screen("home")
    object Lecture : Screen("lecture")
}

private fun getTabIndex(route: String?): Int = when (route) {
    Screen.Library.route -> 0
    Screen.Templates.route, Screen.AIChat.route -> 1
    Screen.Import.route -> 2
    Screen.Settings.route -> 3
    else -> -1
}

private fun tabEnterTransition(scope: AnimatedContentTransitionScope<NavBackStackEntry>) =
    if (getTabIndex(scope.initialState.destination.route) < getTabIndex(scope.targetState.destination.route)) {
        slideInHorizontally(
            initialOffsetX = { (it * 0.35f).toInt() },
            animationSpec = tween(280, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(240))
    } else {
        slideInHorizontally(
            initialOffsetX = { (-it * 0.35f).toInt() },
            animationSpec = tween(280, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(240))
    }

private fun tabExitTransition(scope: AnimatedContentTransitionScope<NavBackStackEntry>) =
    if (getTabIndex(scope.initialState.destination.route) < getTabIndex(scope.targetState.destination.route)) {
        slideOutHorizontally(
            targetOffsetX = { (-it * 0.35f).toInt() },
            animationSpec = tween(280, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(240))
    } else {
        slideOutHorizontally(
            targetOffsetX = { (it * 0.35f).toInt() },
            animationSpec = tween(280, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(240))
    }

@Composable
fun PozixNavigation(
    navController: NavHostController,
    onLanguageChanged: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isPreparingLecture by LectureManager.isPreparing.collectAsState()

    // Smooth modal loading dialog while background coroutine pre-parses heavy theory content
    // Ensures zero transition stutter ("load xong mới vào")
    if (isPreparingLecture) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.lecture_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Library.route
    ) {
        composable(
            route = Screen.Library.route,
            enterTransition = { tabEnterTransition(this) },
            exitTransition = { tabExitTransition(this) }
        ) {
            SavedQuizSetsScreen(
                onPlayQuiz = {
                    navController.navigate(Screen.QuizPlayer.route)
                },
                onStartTest = {
                    navController.navigate(Screen.ExamPlayer.route)
                },
                onOpenLecture = { title, lecture ->
                    LectureManager.openLectureWithPreload(
                        title = title,
                        content = lecture,
                        scope = coroutineScope,
                        onReady = {
                            navController.navigate(Screen.Lecture.route)
                        }
                    )
                }
            )
        }

        composable(
            route = Screen.Templates.route,
            enterTransition = { tabEnterTransition(this) },
            exitTransition = { tabExitTransition(this) }
        ) {
            AIPromptsScreen(
                initialTab = 0,
                onNavigateBack = {
                    navController.navigate(Screen.Library.route) {
                        popUpTo(Screen.Library.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Library.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onPlayQuiz = {
                    navController.navigate(Screen.QuizPlayer.route)
                },
                onOpenLecture = {
                    navController.navigate(Screen.Lecture.route)
                }
            )
        }

        composable(
            route = Screen.Import.route,
            enterTransition = { tabEnterTransition(this) },
            exitTransition = { tabExitTransition(this) }
        ) {
            ImportScreen(
                onQuizLoaded = {
                    navController.navigate(Screen.Library.route) {
                        popUpTo(Screen.Library.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onPlayQuiz = {
                    navController.navigate(Screen.QuizPlayer.route)
                },
                onOpenLecture = {
                    navController.navigate(Screen.Lecture.route)
                }
            )
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = { tabEnterTransition(this) },
            exitTransition = { tabExitTransition(this) }
        ) {
            SettingsScreen(
                onLanguageChanged = onLanguageChanged
            )
        }

        composable(
            route = Screen.QuizPlayer.route,
            enterTransition = {
                fadeIn(animationSpec = tween(280))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(240))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(280))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(240))
            }
        ) {
            QuizPlayerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ExamPlayer.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(340, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(240))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = tween(340, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(240))
            }
        ) {
            ExamPlayerScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.AIChat.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(340, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(240))
            }
        ) {
            AIPromptsScreen(
                initialTab = 0,
                onNavigateBack = {
                    navController.navigate(Screen.Library.route) {
                        popUpTo(Screen.Library.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Library.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onPlayQuiz = {
                    navController.navigate(Screen.QuizPlayer.route)
                },
                onOpenLecture = {
                    navController.navigate(Screen.Lecture.route)
                }
            )
        }
 
        composable(
            route = Screen.Lecture.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(340, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(240))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(280))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(240))
            }
        ) {
            LectureDetailScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
