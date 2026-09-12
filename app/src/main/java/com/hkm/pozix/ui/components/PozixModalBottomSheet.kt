package com.hkm.pozix.ui.components

import android.app.Activity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * BouncyContainer adds smooth, tactile rubber-band spring overscroll physics
 * to any scrollable child (LazyColumn, verticalScroll, etc.).
 * When pulled past boundary, it resists elastically and snaps back smoothly ("nảy lại").
 */
@Composable
fun BouncyContainer(
    modifier: Modifier = Modifier,
    maxOverscroll: Float = 160f,
    content: @Composable BoxScope.() -> Unit
) {
    var overscrollOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = overscrollOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "overscrollBounce"
    )

    val nestedScrollConnection = remember(maxOverscroll) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If container is stretched and user drags back toward center, collapse stretch first
                if (overscrollOffset > 0f && available.y < 0f) {
                    val consumedY = available.y.coerceAtLeast(-overscrollOffset)
                    overscrollOffset += consumedY
                    return Offset(0f, consumedY)
                }
                if (overscrollOffset < 0f && available.y > 0f) {
                    val consumedY = available.y.coerceAtMost(-overscrollOffset)
                    overscrollOffset += consumedY
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y != 0f && source == NestedScrollSource.UserInput) {
                    // Rubber-band resistance formula
                    val currentAbs = kotlin.math.abs(overscrollOffset)
                    val resistance = 0.35f / (1f + currentAbs / 300f)
                    val delta = available.y * resistance
                    overscrollOffset = (overscrollOffset + delta).coerceIn(-maxOverscroll, maxOverscroll)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // On fast fling hitting boundary, absorb momentum into an elastic overshoot
                if (kotlin.math.abs(available.y) > 80f) {
                    val flingImpulse = (available.y * 0.035f).coerceIn(-maxOverscroll * 0.75f, maxOverscroll * 0.75f)
                    overscrollOffset = flingImpulse
                }
                // Always release back to neutral
                overscrollOffset = 0f
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .nestedScroll(nestedScrollConnection)
            .graphicsLayer {
                translationY = animatedOffset
            },
        content = content
    )
}

/**
 * PozixModalBottomSheet wraps Material 3 ModalBottomSheet with:
 * 1. Automatic status bar & navigation bar theme synchronization (prevents white status bar bugs).
 * 2. Elegant 28.dp rounded top corners and Material 3 Expressive drag handle.
 * 3. Restores host Activity status bar cleanly upon dismissal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PozixModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 2.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = {
        PozixSheetDragHandle()
    },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets,
        properties = properties
    ) {
        val sheetView = LocalView.current
        DisposableEffect(sheetView, isDark) {
            // Find window of the Dialog holding the BottomSheet
            val dialogWindow = generateSequence(sheetView.parent) { it.parent }
                .filterIsInstance<DialogWindowProvider>()
                .firstOrNull()?.window
                ?: (sheetView.context as? Activity)?.window

            if (dialogWindow != null) {
                WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
                val insetsController = WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }

            // Ensure host Activity window also maintains correct status bar contrast
            (sheetView.context as? Activity)?.window?.let { actWindow ->
                val actController = WindowCompat.getInsetsController(actWindow, actWindow.decorView)
                actController.isAppearanceLightStatusBars = !isDark
                actController.isAppearanceLightNavigationBars = !isDark
            }

            onDispose {
                // Restore host Activity status bar upon sheet dismiss
                (sheetView.context as? Activity)?.window?.let { actWindow ->
                    val actController = WindowCompat.getInsetsController(actWindow, actWindow.decorView)
                    actController.isAppearanceLightStatusBars = !isDark
                    actController.isAppearanceLightNavigationBars = !isDark
                }
            }
        }

        content()
    }
}

/**
 * Standard tactile drag handle for Pozix Bottom Sheets.
 */
@Composable
fun PozixSheetDragHandle(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
) {
    Box(
        modifier = modifier
            .padding(top = 10.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(4.5.dp)
                .background(
                    color = color,
                    shape = RoundedCornerShape(percent = 50)
                )
        )
    }
}
