package com.hkm.pozix.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
 * Ultra-compact, minimalist Material 3 floating pill bottom navigation bar.
 * Designed with 100% dynamic colors, spring physics, and zero screen obstruction.
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
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(27.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }

                val itemBgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    animationSpec = tween(200),
                    label = "item_bg_color"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "item_content_color"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(itemBgColor)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab
                        ) {
                            if (!isSelected) {
                                HapticUtil.navigationChange(context)
                                onItemSelected(index)
                            }
                        }
                        .semantics {
                            this.selected = isSelected
                            this.contentDescription = item.label
                        }
                        .padding(
                            horizontal = if (isSelected) 12.dp else 10.dp,
                            vertical = 7.dp
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
                            modifier = Modifier.size(20.dp)
                        )

                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(tween(160)) + expandHorizontally(
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
                                expandFrom = Alignment.Start
                            ),
                            exit = fadeOut(tween(100)) + shrinkHorizontally(
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
                                shrinkTowards = Alignment.Start
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(5.dp))
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
