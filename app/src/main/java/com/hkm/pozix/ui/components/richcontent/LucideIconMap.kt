package com.hkm.pozix.ui.components.richcontent

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Minimalist, high-precision Lucide-style vector icon resolver for Pozix.
 * Uses Material Outlined icons with refined 1.5-2dp vector strokes matching Lucide icons.
 * Replaces tacky toy emojis with clean, professional educational glyphs.
 */
object LucideIconMap {

    /**
     * Normalizes an icon name from AI outputs or markdown strings:
     * - Strips prefixes: "lucide:", "lucide-", "icon:", "icon=", "badge:"
     * - Replaces underscores with hyphens
     * - Trims and converts to lowercase
     */
    fun normalizeIconName(name: String): String {
        var s = name.trim().lowercase()
        if (s.startsWith("lucide:")) s = s.removePrefix("lucide:")
        if (s.startsWith("lucide-")) s = s.removePrefix("lucide-")
        if (s.startsWith("icon:")) s = s.removePrefix("icon:")
        if (s.startsWith("icon=")) s = s.removePrefix("icon=")
        if (s.startsWith("badge:")) s = s.removePrefix("badge:")
        s = s.replace('_', '-')
        s = s.replace(Regex("-+"), "-").trim('-')
        return s
    }

    private val ICONS: Map<String, ImageVector> = mapOf(
        // Checkmarks & Success
        "check" to Icons.Outlined.Check,
        "done" to Icons.Outlined.Check,
        "check-circle" to Icons.Outlined.CheckCircleOutline,
        "circle-check" to Icons.Outlined.CheckCircleOutline,
        "check-circle-2" to Icons.Outlined.CheckCircleOutline,
        "check-check" to Icons.Outlined.DoneAll,
        "done-all" to Icons.Outlined.DoneAll,
        "checklist" to Icons.Outlined.Checklist,
        "list-checks" to Icons.Outlined.Checklist,

        // Errors, Bans & Crosses (Matches user issue: x-circle)
        "x-circle" to Icons.Outlined.Cancel,
        "circle-x" to Icons.Outlined.Cancel,
        "x-circle-2" to Icons.Outlined.Cancel,
        "cancel" to Icons.Outlined.Cancel,
        "ban" to Icons.Outlined.Cancel,
        "stop-circle" to Icons.Outlined.Cancel,
        "cross-circle" to Icons.Outlined.Cancel,
        "x" to Icons.Outlined.Close,
        "close" to Icons.Outlined.Close,
        "cross" to Icons.Outlined.Close,
        "times" to Icons.Outlined.Close,

        // Warnings & Alerts (Matches user issue: triangle-alert)
        "triangle-alert" to Icons.Outlined.WarningAmber,
        "alert-triangle" to Icons.Outlined.WarningAmber,
        "warning" to Icons.Outlined.WarningAmber,
        "warn" to Icons.Outlined.WarningAmber,
        "caution" to Icons.Outlined.WarningAmber,
        "alert-circle" to Icons.Outlined.ErrorOutline,
        "circle-alert" to Icons.Outlined.ErrorOutline,
        "error" to Icons.Outlined.ErrorOutline,
        "danger" to Icons.Outlined.ErrorOutline,

        // Information & Help
        "info" to Icons.Outlined.Info,
        "circle-info" to Icons.Outlined.Info,
        "information" to Icons.Outlined.Info,
        "help-circle" to Icons.AutoMirrored.Outlined.HelpOutline,
        "circle-help" to Icons.AutoMirrored.Outlined.HelpOutline,
        "help" to Icons.AutoMirrored.Outlined.HelpOutline,
        "question" to Icons.AutoMirrored.Outlined.HelpOutline,
        "faq" to Icons.AutoMirrored.Outlined.HelpOutline,

        // Science, Laws & Scales (Matches user issue: scale)
        "scale" to Icons.Outlined.Balance,
        "scales" to Icons.Outlined.Balance,
        "balance" to Icons.Outlined.Balance,
        "scale-3d" to Icons.Outlined.Balance,
        "justice" to Icons.Outlined.Balance,
        "law" to Icons.Outlined.Balance,

        // Globe & International (Matches user issue: globe-2)
        "globe" to Icons.Outlined.Public,
        "globe-2" to Icons.Outlined.Public,
        "earth" to Icons.Outlined.Public,
        "world" to Icons.Outlined.Public,
        "planet" to Icons.Outlined.Public,
        "web" to Icons.Outlined.Public,
        "public" to Icons.Outlined.Public,
        "language" to Icons.Outlined.Public,

        // Education & Academia
        "school" to Icons.Outlined.School,
        "graduation-cap" to Icons.Outlined.School,
        "academic-cap" to Icons.Outlined.School,
        "academy" to Icons.Outlined.School,
        "university" to Icons.Outlined.School,
        "college" to Icons.Outlined.School,
        "student" to Icons.Outlined.School,

        // History & Culture
        "landmark" to Icons.Outlined.AccountBalance,
        "history" to Icons.Outlined.AccountBalance,
        "history-edu" to Icons.Outlined.HistoryEdu,
        "museum" to Icons.Outlined.AccountBalance,
        "temple" to Icons.Outlined.AccountBalance,
        "heritage" to Icons.Outlined.AccountBalance,

        // Trends & Growth
        "trending-up" to Icons.AutoMirrored.Outlined.TrendingUp,
        "trend-up" to Icons.AutoMirrored.Outlined.TrendingUp,
        "growth" to Icons.AutoMirrored.Outlined.TrendingUp,
        "trending-down" to Icons.AutoMirrored.Outlined.TrendingDown,
        "trend-down" to Icons.AutoMirrored.Outlined.TrendingDown,

        // STEM & Math
        "calculator" to Icons.Outlined.Calculate,
        "calc" to Icons.Outlined.Calculate,
        "percent" to Icons.Outlined.Percent,
        "percentage" to Icons.Outlined.Percent,
        "atom" to Icons.Outlined.Science,
        "science" to Icons.Outlined.Science,
        "physics" to Icons.Outlined.Science,
        "quantum" to Icons.Outlined.Science,
        "flask" to Icons.Outlined.Biotech,
        "chemistry" to Icons.Outlined.Biotech,
        "lab" to Icons.Outlined.Biotech,
        "biotech" to Icons.Outlined.Biotech,
        "dna" to Icons.Outlined.Biotech,
        "infinity" to Icons.Outlined.AllInclusive,

        // Psychology, Brain & Mind
        "psychology" to Icons.Outlined.Psychology,
        "brain" to Icons.Outlined.Psychology,
        "mind" to Icons.Outlined.Psychology,
        "logic" to Icons.Outlined.Psychology,

        // Ideas & Highlights
        "lightbulb" to Icons.Outlined.Lightbulb,
        "bulb" to Icons.Outlined.Lightbulb,
        "idea" to Icons.Outlined.Lightbulb,
        "lamp" to Icons.Outlined.Lightbulb,
        "tips" to Icons.Outlined.Lightbulb,
        "sparkles" to Icons.Outlined.AutoAwesome,
        "sparkle" to Icons.Outlined.AutoAwesome,
        "magic" to Icons.Outlined.AutoAwesome,
        "ai" to Icons.Outlined.AutoAwesome,
        "star" to Icons.Outlined.StarRate,
        "favorite" to Icons.Outlined.StarRate,
        "zap" to Icons.Outlined.Bolt,
        "bolt" to Icons.Outlined.Bolt,
        "flash" to Icons.Outlined.Bolt,
        "lightning" to Icons.Outlined.Bolt,
        "speed" to Icons.Outlined.Bolt,
        "power" to Icons.Outlined.Bolt,
        "flame" to Icons.Outlined.LocalFireDepartment,
        "fire" to Icons.Outlined.LocalFireDepartment,
        "hot" to Icons.Outlined.LocalFireDepartment,
        "rocket" to Icons.Outlined.RocketLaunch,
        "launch" to Icons.Outlined.RocketLaunch,
        "boost" to Icons.Outlined.RocketLaunch,

        // Goals & Navigation
        "target" to Icons.Outlined.TrackChanges,
        "goal" to Icons.Outlined.TrackChanges,
        "focus" to Icons.Outlined.TrackChanges,
        "aim" to Icons.Outlined.TrackChanges,
        "crosshair" to Icons.Outlined.TrackChanges,
        "compass" to Icons.Outlined.Explore,
        "explore" to Icons.Outlined.Explore,
        "navigation" to Icons.Outlined.Explore,
        "flag" to Icons.Outlined.Flag,

        // Reading, Writing & Content
        "book" to Icons.AutoMirrored.Outlined.MenuBook,
        "book-open" to Icons.AutoMirrored.Outlined.MenuBook,
        "read" to Icons.AutoMirrored.Outlined.MenuBook,
        "theory" to Icons.AutoMirrored.Outlined.MenuBook,
        "bookmark" to Icons.Outlined.BookmarkBorder,
        "pin" to Icons.Outlined.PushPin,
        "pencil" to Icons.Outlined.Edit,
        "pen" to Icons.Outlined.Edit,
        "edit" to Icons.Outlined.Edit,
        "write" to Icons.Outlined.Edit,
        "quote" to Icons.Outlined.FormatQuote,
        "quotes" to Icons.Outlined.FormatQuote,
        "quotation" to Icons.Outlined.FormatQuote,
        "file-text" to Icons.AutoMirrored.Outlined.Article,
        "file" to Icons.AutoMirrored.Outlined.Article,
        "document" to Icons.AutoMirrored.Outlined.Article,
        "doc" to Icons.AutoMirrored.Outlined.Article,
        "tag" to Icons.AutoMirrored.Outlined.Label,
        "label" to Icons.AutoMirrored.Outlined.Label,
        "list" to Icons.AutoMirrored.Outlined.FormatListBulleted,
        "layers" to Icons.Outlined.Layers,

        // Tech, Code & Data
        "code" to Icons.Outlined.Code,
        "terminal" to Icons.Outlined.Terminal,
        "cpu" to Icons.Outlined.Memory,
        "chip" to Icons.Outlined.Memory,
        "database" to Icons.Outlined.Storage,
        "storage" to Icons.Outlined.Storage,

        // Time & Calendar
        "clock" to Icons.Outlined.Schedule,
        "time" to Icons.Outlined.Schedule,
        "schedule" to Icons.Outlined.Schedule,
        "timer" to Icons.Outlined.Schedule,
        "hourglass" to Icons.Outlined.HourglassEmpty,
        "calendar" to Icons.Outlined.CalendarToday,
        "date" to Icons.Outlined.CalendarToday,

        // Security & Privacy
        "shield" to Icons.Outlined.Shield,
        "security" to Icons.Outlined.Shield,
        "protect" to Icons.Outlined.Shield,
        "lock" to Icons.Outlined.Lock,
        "unlock" to Icons.Outlined.LockOpen,
        "key" to Icons.Outlined.VpnKey,
        "eye" to Icons.Outlined.Visibility,
        "eye-off" to Icons.Outlined.VisibilityOff,

        // Rewards & Praise
        "award" to Icons.Outlined.EmojiEvents,
        "trophy" to Icons.Outlined.EmojiEvents,
        "medal" to Icons.Outlined.MilitaryTech,
        "prize" to Icons.Outlined.EmojiEvents,
        "heart" to Icons.Outlined.FavoriteBorder,
        "love" to Icons.Outlined.FavoriteBorder,
        "thumbs-up" to Icons.Outlined.ThumbUp,
        "thumbs-down" to Icons.Outlined.ThumbDown,

        // Actions & Utilities
        "bell" to Icons.Outlined.Notifications,
        "notification" to Icons.Outlined.Notifications,
        "link" to Icons.Outlined.Link,
        "external-link" to Icons.AutoMirrored.Outlined.OpenInNew,
        "search" to Icons.Outlined.Search,
        "find" to Icons.Outlined.Search,
        "copy" to Icons.Outlined.ContentCopy,
        "share" to Icons.Outlined.Share,
        "share-2" to Icons.Outlined.Share,
        "trash" to Icons.Outlined.Delete,
        "trash-2" to Icons.Outlined.Delete,
        "delete" to Icons.Outlined.Delete,
        "refresh" to Icons.Outlined.Refresh,
        "sync" to Icons.Outlined.Refresh,
        "plus" to Icons.Outlined.Add,
        "add" to Icons.Outlined.Add,
        "plus-circle" to Icons.Outlined.AddCircleOutline,
        "minus" to Icons.Outlined.Remove,
        "minus-circle" to Icons.Outlined.RemoveCircleOutline,
        "arrow-right" to Icons.AutoMirrored.Outlined.ArrowForward,
        "arrow-left" to Icons.AutoMirrored.Outlined.ArrowBack,
        "arrow-up" to Icons.Outlined.ArrowUpward,
        "arrow-down" to Icons.Outlined.ArrowDownward,

        // Social & Communication
        "user" to Icons.Outlined.Person,
        "person" to Icons.Outlined.Person,
        "users" to Icons.Outlined.People,
        "people" to Icons.Outlined.People,
        "chat" to Icons.Outlined.ChatBubbleOutline,
        "message-circle" to Icons.Outlined.ChatBubbleOutline,
        "message-square" to Icons.Outlined.ChatBubbleOutline,
        "comment" to Icons.Outlined.ChatBubbleOutline,

        // Media & Visual
        "music" to Icons.Outlined.MusicNote,
        "audio" to Icons.Outlined.MusicNote,
        "volume" to Icons.AutoMirrored.Outlined.VolumeUp,
        "volume-2" to Icons.AutoMirrored.Outlined.VolumeUp,
        "volume-x" to Icons.AutoMirrored.Outlined.VolumeOff,
        "mute" to Icons.AutoMirrored.Outlined.VolumeOff,
        "image" to Icons.Outlined.Image,
        "video" to Icons.Outlined.Videocam,
        "download" to Icons.Outlined.Download,
        "upload" to Icons.Outlined.Upload,
        "settings" to Icons.Outlined.Settings,
        "filter" to Icons.Outlined.FilterList,
        "pie-chart" to Icons.Outlined.PieChart,
        "bar-chart" to Icons.Outlined.BarChart,
        "bar-chart-2" to Icons.Outlined.BarChart,
        "folder" to Icons.Outlined.Folder,
        "folder-open" to Icons.Outlined.FolderOpen,
        "brush" to Icons.Outlined.Brush
    )

    fun isValidIcon(name: String): Boolean {
        val norm = normalizeIconName(name)
        return LucideGlyphMap.hasGlyph(norm) || ICONS.containsKey(norm)
    }

    fun getIcon(name: String): ImageVector? {
        val norm = normalizeIconName(name)
        return ICONS[norm]
    }

    /**
     * Resolves an icon with a clean fallback so vector rendering never breaks.
     */
    fun getIconOrDefault(name: String, fallback: ImageVector = Icons.Outlined.AutoAwesome): ImageVector {
        return getIcon(name) ?: fallback
    }

    /**
     * Infers a harmonious Material pastel badge color when AI specifies only an icon without a color.
     * E.g. x-circle -> rose, triangle-alert -> amber, check-circle -> emerald, scale -> purple.
     */
    fun resolveDefaultColor(iconName: String): String {
        val norm = normalizeIconName(iconName)
        when (norm) {
            "x-circle", "circle-x", "x-circle-2", "cancel", "ban", "cross-circle", "stop-circle",
            "x", "close", "cross", "times", "trash", "trash-2", "delete", "remove",
            "volume-x", "mute", "thumbs-down", "skull" -> return "rose"

            "triangle-alert", "alert-triangle", "warning", "warn", "caution",
            "circle-alert", "alert-circle", "error", "danger",
            "flame", "fire", "hot", "flag", "siren" -> return "amber"

            "check", "check-circle", "circle-check", "check-circle-2", "check-check", "done", "done-all",
            "checklist", "list-checks", "thumbs-up", "shield", "security", "protect", "leaf", "flower", "tree" -> return "emerald"

            "scale", "scales", "balance", "scale-3d", "justice", "law",
            "book", "book-open", "read", "theory",
            "school", "graduation-cap", "academic-cap", "academy", "university", "college", "student",
            "layers" -> return "purple"

            "globe", "globe-2", "earth", "world", "planet", "web", "public", "language",
            "compass", "explore", "navigation",
            "link", "external-link",
            "info", "circle-info", "information", "help-circle", "circle-help", "help", "question", "faq", "wifi" -> return "blue"

            "lightbulb", "bulb", "idea", "lamp", "tips",
            "zap", "bolt", "flash", "lightning", "speed", "power",
            "sparkles", "sparkle", "magic", "star", "favorite",
            "award", "trophy", "medal", "prize", "crown", "sun" -> return "yellow"

            "calculator", "calc", "target", "goal", "focus", "aim", "crosshair",
            "atom", "science", "physics", "quantum",
            "flask", "chemistry", "lab", "biotech", "dna",
            "percent", "percentage", "microscope", "syringe" -> return "teal"

            "landmark", "history", "history-edu", "museum", "temple", "heritage",
            "psychology", "brain", "mind", "logic",
            "cpu", "chip", "database", "storage",
            "terminal", "code" -> return "indigo"

            "rocket", "launch", "boost", "heart", "love" -> return "pink"

            "quote", "quotes", "quotation", "tag", "label", "bookmark", "pin",
            "clock", "time", "schedule", "timer", "hourglass",
            "calendar", "date", "file-text", "file", "document", "doc",
            "pencil", "pen", "edit", "write" -> return "gray"
        }

        // Secondary semantic heuristics for all 2,100+ Lucide icons
        return when {
            norm.contains("alert") || norm.contains("warn") || norm.contains("flame") || norm.contains("fire") -> "amber"
            norm.contains("check") || norm.contains("shield") || norm.contains("protect") || norm.contains("leaf") -> "emerald"
            norm.contains("remove") || norm.contains("delete") || norm.contains("trash") || norm.contains("ban") || norm.contains("close") || norm.contains("cross") || norm.contains("error") -> "rose"
            norm.contains("book") || norm.contains("school") || norm.contains("scale") || norm.contains("law") -> "purple"
            norm.contains("globe") || norm.contains("map") || norm.contains("compass") || norm.contains("network") || norm.contains("link") || norm.contains("cloud") || norm.contains("info") -> "blue"
            norm.contains("sun") || norm.contains("star") || norm.contains("zap") || norm.contains("light") || norm.contains("sparkle") || norm.contains("award") -> "yellow"
            norm.contains("atom") || norm.contains("chem") || norm.contains("dna") || norm.contains("calc") || norm.contains("target") -> "teal"
            norm.contains("brain") || norm.contains("code") || norm.contains("cpu") || norm.contains("data") -> "indigo"
            norm.contains("heart") || norm.contains("rocket") -> "pink"
            else -> "yellow"
        }
    }

    /**
     * Documentation list for AI prompt templates.
     */
    val SUPPORTED_ICON_NAMES: List<String> = ICONS.keys.distinct().sorted()
}
