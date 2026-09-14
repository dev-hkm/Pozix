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
            initialOffsetX = { -(it * 0.35f).toInt() },
            animationSpec = tween(280, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(240))
    }

private fun tabExitTransition(scope: AnimatedContentTransitionScope<NavBackStackEntry>) =
    if (getTabIndex(scope.initialState.destination.route) < getTabIndex(scope.targetState.destination.route)) {
        slideOutHorizontally(
            targetOffsetX = { -(it * 0.35f).toInt() },
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
                    LectureManager.openLecture(title, lecture)
                    navController.navigate(Screen.Lecture.route)
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
