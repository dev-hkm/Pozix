package com.hkm.pozix.ui.components.richcontent

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.hkm.pozix.R
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

    // Code editor palette follows the app theme: bright paper-like light mode, rich contrast dark mode.
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val editorBg = MaterialTheme.colorScheme.surfaceContainer
    val headerBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val lineNumberColor = if (isDark) Color(0xFF6C7086) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    val codeTextColor = MaterialTheme.colorScheme.onSurface
    val badgeBg = MaterialTheme.colorScheme.secondaryContainer
    val badgeText = MaterialTheme.colorScheme.onSecondaryContainer

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = editorBg,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = badgeText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = displayLanguage,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
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

            // Code content with line numbers and horizontal scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
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

    val commentColor = if (darkMode) Color(0xFF6C7086) else Color(0xFF64748B)
    val stringColor = if (darkMode) Color(0xFFA6E3A1) else Color(0xFF047857)
    val tagColor = if (darkMode) Color(0xFF89DCEB) else Color(0xFF0369A1)
    val attributeColor = if (darkMode) Color(0xFFF9E2AF) else Color(0xFF9A3412)
    val keywordColor = if (darkMode) Color(0xFFCBA6F7) else Color(0xFF7C3AED)
    val typeColor = if (darkMode) Color(0xFF89B4FA) else Color(0xFF1D4ED8)
    val constantColor = if (darkMode) Color(0xFFFAB387) else Color(0xFFC2410C)

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
