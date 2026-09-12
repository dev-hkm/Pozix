package com.hkm.pozix.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.pozix.R
import com.hkm.pozix.util.HapticUtil

data class NavItem(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val route: String
)

/**
 * Ultra-compact, Apple-inspired floating pill bottom navigation bar.
 * Shortened length (hugging content instead of edge-to-edge),
 * tactile scale bounce on tap, icon micro-bounce, and fluid pill animations.
 */
@Composable
fun FloatingPillBottomNav(
    selectedIndex: Int,
    items: List<NavItem>,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .wrapContentWidth()
            .height(50.dp),
        shape = RoundedCornerShape(25.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
        tonalElevation = 4.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(
            width = 0.75.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                // Apple-style tactile spring scale on tap
                val pressScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.88f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "press_scale"
                )

                // Icon micro-bounce when selected
                val iconBounce by animateFloatAsState(
                    targetValue = if (isSelected) 1.12f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = 0.45f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "icon_bounce"
                )

                val itemBgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    animationSpec = if (isSelected) {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    } else {
                        tween(durationMillis = 60)
                    },
                    label = "item_bg_color"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(150),
                    label = "item_content_color"
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(itemBgColor)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab
                        ) {
                            if (!isSelected) {
                                HapticUtil.selectionTick(context)
                                onItemSelected(index)
                            }
                        }
                        .semantics {
                            this.selected = isSelected
                            this.contentDescription = item.label
                        }
                        .padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier
                                .size(19.dp)
                                .graphicsLayer {
                                    scaleX = iconBounce
                                    scaleY = iconBounce
                                }
                        )

                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(tween(140)) + expandHorizontally(
                                animationSpec = spring(
                                    dampingRatio = 0.65f,
                                    stiffness = 380f
                                ),
                                expandFrom = Alignment.Start
                            ),
                            exit = fadeOut(tween(80)) + shrinkHorizontally(
                                animationSpec = spring(
                                    dampingRatio = 0.82f,
                                    stiffness = 460f
                                ),
                                shrinkTowards = Alignment.Start
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.label,
                                    color = contentColor,
                                    fontSize = 11.5.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem(
            selectedIcon = Icons.AutoMirrored.Rounded.LibraryBooks,
            unselectedIcon = Icons.AutoMirrored.Outlined.LibraryBooks,
            label = stringResource(R.string.nav_quizzes),
            route = "library"
        ),
        NavItem(
            selectedIcon = Icons.Rounded.AutoAwesome,
            unselectedIcon = Icons.Outlined.AutoAwesome,
            label = stringResource(R.string.nav_ai_prompts),
            route = "templates"
        ),
        NavItem(
            selectedIcon = Icons.Rounded.UploadFile,
            unselectedIcon = Icons.Outlined.UploadFile,
            label = stringResource(R.string.nav_import),
            route = "import"
        ),
        NavItem(
            selectedIcon = Icons.Rounded.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            label = stringResource(R.string.nav_settings),
            route = "settings"
        )
    )

    val selectedIndex = items.indexOfFirst {
        it.route == currentRoute || (it.route == "templates" && currentRoute in setOf("templates", "ai_chat", "ai_prompts"))
    }.takeIf { it >= 0 } ?: 0

    FloatingPillBottomNav(
        selectedIndex = selectedIndex,
        items = items,
        onItemSelected = { index -> onNavigate(items[index].route) },
        modifier = modifier
    )
}
