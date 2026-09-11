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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.pozix.util.HapticUtil
import kotlinx.coroutines.delay

/**
 * Modern Jetpack Compose Code Block component designed for Computer Science (Tin học).
 * Provides:
 * - Language tag with icon
 * - One-tap copy to clipboard with haptic feedback
 * - Line numbers gutter
 * - Horizontal scrolling (never breaks line structure or indentation)
 * - Clean syntax highlighting
 */
@Composable
fun CodeBlockView(
    code: String,
    language: String = "",
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

    // Code editor palette
    val editorBg = Color(0xFF1E1E2E) // Modern Catppuccin Mocha-inspired dark slate
    val headerBg = Color(0xFF181825)
    val lineNumberColor = Color(0xFF6C7086)
    val codeTextColor = Color(0xFFCDD6F4)
    val badgeBg = Color(0xFF313244)
    val badgeText = Color(0xFF89B4FA)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = editorBg,
        shadowElevation = 3.dp,
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

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        HapticUtil.actionConfirm(context)
                        isCopied = true
                        Toast.makeText(context, "Copied code", Toast.LENGTH_SHORT).show()
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
                                contentDescription = "Copied",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy code",
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
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Line numbers gutter
                Column(horizontalAlignment = Alignment.End) {
                    lines.forEachIndexed { index, _ ->
                        Text(
                            text = "${index + 1}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = lineNumberColor,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Code lines
                Column {
                    lines.forEach { line ->
                        Text(
                            text = highlightCodeLine(line),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = codeTextColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lightweight syntax highlighter for popular languages (Python, C++, Java, Kotlin, C, SQL, JS).
 */
private fun highlightCodeLine(line: String): AnnotatedString {
    return buildAnnotatedString {
        val trimmed = line.trimStart()

        // Comments
        if (trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("--")) {
            pushStyle(SpanStyle(color = Color(0xFF6C7086), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
            append(line)
            pop()
            return@buildAnnotatedString
        }

        var i = 0
        val len = line.length

        while (i < len) {
            // String literal "..." or '...'
            if (line[i] == '"' || line[i] == '\'') {
                val quote = line[i]
                val endIdx = line.indexOf(quote, i + 1)
                if (endIdx != -1) {
                    pushStyle(SpanStyle(color = Color(0xFFA6E3A1))) // Soft green
                    append(line.substring(i, endIdx + 1))
                    pop()
                    i = endIdx + 1
                    continue
                }
            }

            // Words (keywords, types, identifiers)
            if (line[i].isLetter() || line[i] == '_') {
                val start = i
                while (i < len && (line[i].isLetterOrDigit() || line[i] == '_')) {
                    i++
                }
                val word = line.substring(start, i)
                when (word) {
                    // Control flow / keywords
                    "def", "class", "fun", "val", "var", "function", "return", "if", "else",
                    "for", "while", "do", "switch", "case", "break", "continue", "import",
                    "from", "package", "public", "private", "protected", "static", "const",
                    "SELECT", "FROM", "WHERE", "JOIN", "ORDER", "BY", "GROUP", "INSERT",
                    "UPDATE", "DELETE", "AND", "OR", "NOT", "IN", "IS", "NULL", "try", "catch" -> {
                        pushStyle(SpanStyle(color = Color(0xFFCBA6F7), fontWeight = FontWeight.Bold)) // Mauve/Purple
                        append(word)
                        pop()
                    }
                    // Types
                    "int", "float", "double", "char", "bool", "boolean", "void", "string",
                    "String", "Int", "Boolean", "Float", "Double", "List", "Map", "Set",
                    "vector", "auto", "long", "short", "unsigned" -> {
                        pushStyle(SpanStyle(color = Color(0xFF89B4FA), fontWeight = FontWeight.SemiBold)) // Blue
                        append(word)
                        pop()
                    }
                    // Constants / Booleans
                    "true", "false", "True", "False", "null", "None", "nullptr" -> {
                        pushStyle(SpanStyle(color = Color(0xFFFAB387), fontWeight = FontWeight.Bold)) // Peach/Orange
                        append(word)
                        pop()
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
                while (i < len && (line[i].isDigit() || line[i] == '.' || line[i] == 'f' || line[i] == 'L')) {
                    i++
                }
                pushStyle(SpanStyle(color = Color(0xFFFAB387))) // Peach
                append(line.substring(start, i))
                pop()
                continue
            }

            append(line[i])
            i++
        }
    }
}
