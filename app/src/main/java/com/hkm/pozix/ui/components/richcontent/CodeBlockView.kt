package com.hkm.pozix.ui.components.richcontent

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hkm.pozix.R
import com.hkm.pozix.ui.theme.readableContentColorFor
import com.hkm.pozix.util.HapticUtil
import kotlinx.coroutines.delay

/**
 * Ambient composition local controlling whether code syntax highlighting is active globally.
 */
val LocalCodeHighlight = compositionLocalOf { true }

/**
 * Modern Jetpack Compose Code Block component designed for Computer Science (Tin học).
 * Provides:
 * - Language tag with icon
 * - Live HTML & SVG rendering preview toggle
 * - One-tap copy to clipboard with haptic feedback
 * - Line numbers gutter
 * - Horizontal scrolling (never breaks line structure or indentation)
 * - Clean syntax highlighting with on/off switch support
 */
@Composable
fun CodeBlockView(
    code: String,
    language: String = "",
    highlightEnabled: Boolean = LocalCodeHighlight.current,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    val lines = remember(code) { code.lines() }
    val displayLanguage = remember(language) {
        language.trim().ifBlank { "CODE" }.uppercase()
    }

    val isPreviewable = remember(displayLanguage, code) {
        val upperLang = displayLanguage.uppercase()
        val trimmed = code.trimStart()
        upperLang in listOf("HTML", "HTM", "SVG", "XML") ||
        trimmed.startsWith("<!DOCTYPE html", ignoreCase = true) ||
        trimmed.startsWith("<html", ignoreCase = true) ||
        trimmed.startsWith("<svg", ignoreCase = true) ||
        (code.contains("<html", ignoreCase = true) && code.contains("</html>", ignoreCase = true)) ||
        (code.contains("<svg", ignoreCase = true) && code.contains("</svg>", ignoreCase = true)) ||
        (code.contains("<div", ignoreCase = true) && code.contains("</div>", ignoreCase = true))
    }
    var isPreviewMode by remember { mutableStateOf(false) }
    var showFullscreenBrowser by remember { mutableStateOf(false) }

    // Code editor palette: Tokyo Night Pro for Dark Mode, GitHub High-Contrast for Light Mode.
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val editorBg = if (isDark) Color(0xFF1A1B26) else Color(0xFFF8FAFC)
    val headerBg = if (isDark) Color(0xFF24283B) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF414868).copy(alpha = 0.6f) else Color(0xFFCBD5E1)
    val lineNumberColor = if (isDark) Color(0xFF565F89) else Color(0xFF94A3B8)
    val codeTextColor = if (isDark) Color(0xFFC0CAF5) else Color(0xFF0F172A)
    val badgeBg = if (isDark) Color(0xFF7AA2F7).copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondaryContainer
    val badgeText = if (isDark) Color(0xFF7AA2F7) else readableContentColorFor(
        background = badgeBg,
        preferred = MaterialTheme.colorScheme.onSecondaryContainer
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = editorBg,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header bar with macOS-style terminal dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Terminal traffic dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Box(modifier = Modifier.size(9.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFFFF5F56)))
                        Box(modifier = Modifier.size(9.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFFFFBD2E)))
                        Box(modifier = Modifier.size(9.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFF27C93F)))
                    }
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = displayLanguage,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            color = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPreviewable) {
                        Surface(
                            onClick = {
                                isPreviewMode = !isPreviewMode
                                HapticUtil.actionConfirm(context)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPreviewMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPreviewMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPreviewMode) Icons.Default.Code else Icons.Default.Visibility,
                                    contentDescription = if (isPreviewMode) "Xem mã nguồn" else "Xem trước HTML",
                                    tint = if (isPreviewMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPreviewMode) "Mã" else "Xem trước",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPreviewMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isPreviewMode) {
                            IconButton(
                                onClick = {
                                    HapticUtil.actionConfirm(context)
                                    showFullscreenBrowser = true
                                },
                                modifier = Modifier.size(32.dp).padding(end = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInFull,
                                    contentDescription = "Toàn màn hình",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    val copySuccessText = stringResource(R.string.code_copied)
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(code))
                            HapticUtil.actionConfirm(context)
                            isCopied = true
                            Toast.makeText(context, copySuccessText, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        AnimatedContent(
                            targetState = isCopied,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "copyIcon"
                        ) { copied ->
                            if (copied) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.code_copied),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.code_copy_content_desc),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = isPreviewMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "codeVsPreview"
            ) { inPreview ->
                if (inPreview) {
                    HtmlLivePreview(
                        code = code,
                        isDark = isDark,
                        isSvg = displayLanguage == "SVG" || code.trimStart().startsWith("<svg", ignoreCase = true),
                        onExpand = {
                            HapticUtil.actionConfirm(context)
                            showFullscreenBrowser = true
                        }
                    )
                } else {
                    // Code content with line numbers, vertical limit (~9 lines), and 2D scrolling
                    val vScrollState = rememberScrollState()
                    val hScrollState = rememberScrollState()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 182.dp)
                            .verticalScroll(vScrollState)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(hScrollState)
                                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 10.dp)
                        ) {
                            // Line numbers gutter
                            Column(horizontalAlignment = Alignment.End) {
                                lines.forEachIndexed { index, _ ->
                                    Text(
                                        text = "${index + 1}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = lineNumberColor,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Code lines
                            Column {
                                lines.forEach { line ->
                                    Text(
                                        text = highlightCodeLine(line, displayLanguage, highlightEnabled, isDark),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = codeTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFullscreenBrowser) {
        HtmlMiniBrowserDialog(
            code = code,
            language = displayLanguage,
            onDismiss = { showFullscreenBrowser = false }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HtmlLivePreview(
    code: String,
    isDark: Boolean,
    isSvg: Boolean,
    modifier: Modifier = Modifier,
    onExpand: (() -> Unit)? = null
) {
    val bgHex = if (isDark) "#1E1E2E" else "#FFFFFF"
    val textColor = if (isDark) "#CDD6F4" else "#1E293B"

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                loadUrl("about:blank")
                onPause()
                removeAllViews()
                destroy()
            }
            webViewRef = null
        }
    }

    val formattedHtml = remember(code, isDark, isSvg) {
        val canvasTouchCss = """
            <style id="pozix-canvas-fix">
                canvas {
                    touch-action: none;
                    -webkit-touch-callout: none;
                    user-select: none;
                }
                * {
                    -webkit-tap-highlight-color: transparent;
                }
            </style>
        """.trimIndent()

        if (isSvg) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0">
                <style>
                    html, body {
                        margin: 0;
                        padding: 12px;
                        background: $bgHex;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100%;
                        box-sizing: border-box;
                    }
                    svg {
                        max-width: 100%;
                        height: auto;
                    }
                </style>
            </head>
            <body>
                $code
            </body>
            </html>
            """.trimIndent()
        } else {
            val hasHtmlWrapper = code.contains("<html", ignoreCase = true) && code.contains("</html>", ignoreCase = true)
            if (hasHtmlWrapper) {
                var modified = code
                if (!modified.contains("<meta name=\"viewport\"", ignoreCase = true)) {
                    if (modified.contains("<head>", ignoreCase = true)) {
                        modified = modified.replaceFirst("<head>", "<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">", ignoreCase = true)
                    } else if (modified.contains("<html>", ignoreCase = true)) {
                        modified = modified.replaceFirst("<html>", "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>", ignoreCase = true)
                    }
                }
                if (modified.contains("</head>", ignoreCase = true)) {
                    modified = modified.replaceFirst("</head>", "$canvasTouchCss</head>", ignoreCase = true)
                } else if (modified.contains("<body>", ignoreCase = true)) {
                    modified = modified.replaceFirst("<body>", "<head>$canvasTouchCss</head><body>", ignoreCase = true)
                }
                modified
            } else {
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0">
                    $canvasTouchCss
                    <style>
                        html, body {
                            margin: 0;
                            padding: 12px;
                            background-color: $bgHex;
                            color: $textColor;
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                            font-size: 14px;
                            line-height: 1.5;
                            word-break: break-word;
                            box-sizing: border-box;
                        }
                        * {
                            box-sizing: border-box;
                        }
                        img, svg {
                            max-width: 100%;
                            height: auto;
                        }
                        table {
                            border-collapse: collapse;
                            width: 100%;
                            margin: 8px 0;
                        }
                        th, td {
                            border: 1px solid ${if (isDark) "#45475A" else "#CBD5E1"};
                            padding: 6px 10px;
                            text-align: left;
                        }
                        th {
                            background-color: ${if (isDark) "#313244" else "#F1F5F9"};
                            font-weight: 600;
                        }
                        button, input, select {
                            font-family: inherit;
                            font-size: 13px;
                            padding: 4px 8px;
                            border-radius: 6px;
                            border: 1px solid ${if (isDark) "#585B70" else "#94A3B8"};
                            background: ${if (isDark) "#313244" else "#F8FAFC"};
                            color: $textColor;
                        }
                    </style>
                </head>
                <body>
                    $code
                </body>
                </html>
                """.trimIndent()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp, max = 280.dp)
            .background(if (isDark) Color(0xFF1E1E2E) else Color.White)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    isFocusable = true
                    isFocusableInTouchMode = true
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    isVerticalScrollBarEnabled = true
                    isHorizontalScrollBarEnabled = true
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = false
                        allowContentAccess = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }
                    webViewClient = object : WebViewClient() {
                        @Suppress("DEPRECATION")
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = true
                        override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean = true
                    }
                }
            },
            update = { webView ->
                val loadKey = "$isDark:$isSvg:${code.hashCode()}"
                if (webView.tag != loadKey) {
                    webView.tag = loadKey
                    webView.loadDataWithBaseURL("https://sandbox.local/", formattedHtml, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp, max = 280.dp)
        )

        if (onExpand != null) {
            Surface(
                onClick = onExpand,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Toàn màn hình",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Toàn màn hình",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Universal high-performance syntax highlighter for Pozix.
 * Supports HTML/XML, CSS, JavaScript, TypeScript, Python, C, C++, Java, Kotlin, SQL.
 * Falls back to clean monospace text when highlighting is toggled off in Settings.
 */
private fun highlightCodeLine(
    line: String,
    language: String,
    enabled: Boolean,
    darkMode: Boolean
): AnnotatedString {
    if (!enabled) return AnnotatedString(line)

    // Tokyo Night Pro Vibrant (Dark) & GitHub Studio High-Contrast (Light)
    val commentColor = if (darkMode) Color(0xFF565F89) else Color(0xFF64748B)  // Slate italic
    val stringColor = if (darkMode) Color(0xFF9ECE6A) else Color(0xFF047857)   // Vivid Emerald
    val tagColor = if (darkMode) Color(0xFF7DCFFF) else Color(0xFF6F42C1)      // Electric Cyan / Purple
    val attributeColor = if (darkMode) Color(0xFFFF9E64) else Color(0xFFD97706) // Vivid Peach Orange / Amber
    val keywordColor = if (darkMode) Color(0xFFBB9AF7) else Color(0xFFD73A49)   // Neon Purple / Crimson
    val typeColor = if (darkMode) Color(0xFF2AC3DE) else Color(0xFF0284C7)      // Turquoise / Electric Blue
    val constantColor = if (darkMode) Color(0xFFE0AF68) else Color(0xFF005CC5)  // Warm Gold / Cobalt

    val trimmed = line.trimStart()

    // 1. Full-line Comments
    if (trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("--") || trimmed.startsWith("<!--")) {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)) {
                append(line)
            }
        }
    }

    val isHtml = language in listOf("HTML", "XML", "SVG", "HTM")

    return buildAnnotatedString {
        var i = 0
        val len = line.length

        while (i < len) {
            // HTML comments <!-- ... -->
            if (isHtml && line.startsWith("<!--", i)) {
                val endIdx = line.indexOf("-->", i + 4)
                val commentEnd = if (endIdx != -1) endIdx + 3 else len
                withStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)) {
                    append(line.substring(i, commentEnd))
                }
                i = commentEnd
                continue
            }

            // String literals: "..." or '...'
            if (line[i] == '"' || line[i] == '\'') {
                val quote = line[i]
                val endIdx = line.indexOf(quote, i + 1)
                if (endIdx != -1) {
                    withStyle(SpanStyle(color = stringColor)) {
                        append(line.substring(i, endIdx + 1))
                    }
                    i = endIdx + 1
                    continue
                }
            }

            // HTML Tags: <tag_name, </tag_name, >, />
            if (isHtml && line[i] == '<' && i + 1 < len && (line[i + 1].isLetter() || line[i + 1] == '/' || line[i + 1] == '!')) {
                withStyle(SpanStyle(color = tagColor, fontWeight = FontWeight.Bold)) {
                    val start = i
                    i++
                    if (i < len && line[i] == '/') i++
                    while (i < len && (line[i].isLetterOrDigit() || line[i] == '-' || line[i] == '_')) i++
                    append(line.substring(start, i))
                }
                continue
            }

            if (isHtml && line[i] == '>') {
                withStyle(SpanStyle(color = tagColor, fontWeight = FontWeight.Bold)) {
                    append('>')
                }
                i++
                continue
            }

            // Identifiers / Keywords / Types / HTML attributes
            if (line[i].isLetter() || line[i] == '_') {
                val start = i
                while (i < len && (line[i].isLetterOrDigit() || line[i] == '_' || line[i] == '-')) {
                    i++
                }
                val word = line.substring(start, i)

                val isAttr = isHtml && i < len && line.substring(i).trimStart().startsWith("=")

                when {
                    isAttr -> {
                        withStyle(SpanStyle(color = attributeColor, fontWeight = FontWeight.SemiBold)) {
                            append(word)
                        }
                    }
                    word in KEYWORDS -> {
                        withStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                    }
                    word in TYPES -> {
                        withStyle(SpanStyle(color = typeColor, fontWeight = FontWeight.SemiBold)) {
                            append(word)
                        }
                    }
                    word in CONSTANTS -> {
                        withStyle(SpanStyle(color = constantColor, fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                    }
                    else -> {
                        append(word)
                    }
                }
                continue
            }

            // Numbers
            if (line[i].isDigit()) {
                val start = i
                while (i < len && (line[i].isDigit() || line[i] == '.' || line[i] == 'f' || line[i] == 'L' || line[i] == 'x')) {
                    i++
                }
                withStyle(SpanStyle(color = constantColor)) {
                    append(line.substring(start, i))
                }
                continue
            }

            append(line[i])
            i++
        }
    }
}

private val KEYWORDS = setOf(
    "def", "class", "fun", "val", "var", "function", "return", "if", "else", "elif",
    "for", "while", "do", "switch", "case", "break", "continue", "import", "export",
    "from", "package", "public", "private", "protected", "static", "const", "final",
    "SELECT", "FROM", "WHERE", "JOIN", "ORDER", "BY", "GROUP", "INSERT", "INTO",
    "UPDATE", "DELETE", "AND", "OR", "NOT", "IN", "IS", "NULL", "CREATE", "TABLE",
    "try", "catch", "finally", "throw", "throws", "new", "this", "super", "override",
    "async", "await", "yield", "interface", "struct", "enum", "typealias"
)

private val TYPES = setOf(
    "int", "float", "double", "char", "bool", "boolean", "void", "string", "String",
    "Int", "Boolean", "Float", "Double", "List", "Map", "Set", "vector", "auto",
    "long", "short", "unsigned", "Any", "Unit", "Array", "Object"
)

private val CONSTANTS = setOf(
    "true", "false", "True", "False", "null", "None", "nullptr", "nil", "undefined", "NaN"
)
