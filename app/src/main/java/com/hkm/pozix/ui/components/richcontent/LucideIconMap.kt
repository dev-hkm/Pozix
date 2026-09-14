package com.hkm.pozix.ui.components.richcontent

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Minimalist, high-precision Lucide-style vector icon resolver for Pozix.
 * Uses Material Outlined icons with refined 1.5-2dp vector strokes matching Lucide icons.
 * Replaces tacky toy emojis with clean, professional educational glyphs.
 */
object LucideIconMap {
    private val ICONS: Map<String, ImageVector> = mapOf(
        "star" to Icons.Outlined.StarRate,
        "check" to Icons.Outlined.Check,
        "check-circle" to Icons.Outlined.CheckCircleOutline,
        "check-check" to Icons.Outlined.DoneAll,
        "alert-triangle" to Icons.Outlined.WarningAmber,
        "warning" to Icons.Outlined.WarningAmber,
        "warn" to Icons.Outlined.WarningAmber,
        "alert-circle" to Icons.Outlined.ErrorOutline,
        "error" to Icons.Outlined.ErrorOutline,
        "info" to Icons.Outlined.Info,
        "lightbulb" to Icons.Outlined.Lightbulb,
        "bulb" to Icons.Outlined.Lightbulb,
        "idea" to Icons.Outlined.Lightbulb,
        "book" to Icons.AutoMirrored.Outlined.MenuBook,
        "book-open" to Icons.AutoMirrored.Outlined.MenuBook,
        "bookmark" to Icons.Outlined.BookmarkBorder,
        "pin" to Icons.Outlined.PushPin,
        "code" to Icons.Outlined.Code,
        "terminal" to Icons.Outlined.Terminal,
        "zap" to Icons.Outlined.Bolt,
        "bolt" to Icons.Outlined.Bolt,
        "flash" to Icons.Outlined.Bolt,
        "flame" to Icons.Outlined.LocalFireDepartment,
        "fire" to Icons.Outlined.LocalFireDepartment,
        "heart" to Icons.Outlined.FavoriteBorder,
        "rocket" to Icons.Outlined.RocketLaunch,
        "shield" to Icons.Outlined.Shield,
        "flag" to Icons.Outlined.Flag,
        "sparkles" to Icons.Outlined.AutoAwesome,
        "help-circle" to Icons.AutoMirrored.Outlined.HelpOutline,
        "help" to Icons.AutoMirrored.Outlined.HelpOutline,
        "cpu" to Icons.Outlined.Memory,
        "database" to Icons.Outlined.Storage,
        "tag" to Icons.AutoMirrored.Outlined.Label,
        "clock" to Icons.Outlined.Schedule,
        "time" to Icons.Outlined.Schedule,
        "eye" to Icons.Outlined.Visibility,
        "lock" to Icons.Outlined.Lock,
        "compass" to Icons.Outlined.Explore,
        "layers" to Icons.Outlined.Layers,
        "target" to Icons.Outlined.TrackChanges,
        "search" to Icons.Outlined.Search,
        "file-text" to Icons.AutoMirrored.Outlined.Article,
        "file" to Icons.AutoMirrored.Outlined.Article,
        "calculator" to Icons.Outlined.Calculate,
        "award" to Icons.Outlined.EmojiEvents,
        "trophy" to Icons.Outlined.EmojiEvents,
        "atom" to Icons.Outlined.Science,
        "flask" to Icons.Outlined.Biotech,
        "key" to Icons.Outlined.VpnKey,
        "bell" to Icons.Outlined.Notifications,
        "calendar" to Icons.Outlined.CalendarToday,
        "link" to Icons.Outlined.Link
    )

    fun isValidIcon(name: String): Boolean = ICONS.containsKey(name.trim().lowercase())

    fun getIcon(name: String): ImageVector? = ICONS[name.trim().lowercase()]

    /**
     * Documentation list for AI prompt templates.
     */
    val SUPPORTED_ICON_NAMES: List<String> = ICONS.keys.distinct().sorted()
}
