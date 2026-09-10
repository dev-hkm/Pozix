package com.hkm.pozix.ui.components.richcontent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Represents a parsed segment of rich educational STEM content.
 */
sealed interface ContentBlock {
    data class Paragraph(val text: String) : ContentBlock
    data class Code(val code: String, val language: String) : ContentBlock
    data class MathDisplay(val latex: String) : ContentBlock
}

/**
 * Universal rich educational content component for Pozix.
 * Intelligently handles:
 * - Markdown Code Blocks (```lang ... ```)
 * - Display Math Formulas ($$ ... $$)
 * - Inline Math ($ ... $ or \( ... \))
 * - Inline Code (`...`)
 * - Markdown bold (**...**) and italic (*...*)
 *
 * Designed to never break layout, overflow, or truncate.
 */
@Composable
fun RichContentText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    lineHeight: TextUnit = 24.sp,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    inlineOnly: Boolean = false
) {
    if (text.isBlank()) return

    if (inlineOnly) {
        // Fast path for answer options (A, B, C, D)
        val annotated = remember(text) {
            LatexMathParser.parseToAnnotatedString(text)
        }
        Text(
            text = annotated,
            modifier = modifier,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            lineHeight = lineHeight,
            style = style
        )
        return
    }

    val blocks = remember(text) { parseContentBlocks(text) }

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is ContentBlock.Paragraph -> {
                    val annotated = remember(block.text) {
                        LatexMathParser.parseToAnnotatedString(block.text)
                    }
                    Text(
                        text = annotated,
                        color = textColor,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        lineHeight = lineHeight,
                        style = style,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is ContentBlock.Code -> {
                    CodeBlockView(
                        code = block.code,
                        language = block.language
                    )
                }
                is ContentBlock.MathDisplay -> {
                    KaTeXMathView(
                        latex = block.latex,
                        textColor = textColor,
                        fontSizeSp = fontSize.value
                    )
                }
            }

            if (index < blocks.lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

/**
 * Split text into Paragraphs, Code Blocks, and Display Math blocks.
 */
internal fun parseContentBlocks(input: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    var currentIndex = 0
    val length = input.length

    while (currentIndex < length) {
        // Check for code block ```lang ... ```
        val codeStart = input.indexOf("```", currentIndex)
        // Check for display math $$ ... $$ or \[ ... \]
        val mathStart1 = input.indexOf("$$", currentIndex)
        val mathStart2 = input.indexOf("\\[", currentIndex)
        val mathStart = when {
            mathStart1 != -1 && mathStart2 != -1 -> minOf(mathStart1, mathStart2)
            mathStart1 != -1 -> mathStart1
            else -> mathStart2
        }
        val isBracketMath = mathStart != -1 && mathStart == mathStart2
        val mathDelimiterLen = if (isBracketMath) 2 else 2
        val mathClosingDelimiter = if (isBracketMath) "\\]" else "$$"

        // Find the earliest delimiter
        val hasCode = codeStart != -1
        val hasMath = mathStart != -1

        if (!hasCode && !hasMath) {
            // Remainder is a single paragraph
            val remaining = input.substring(currentIndex).trim()
            if (remaining.isNotEmpty()) {
                blocks.add(ContentBlock.Paragraph(remaining))
            }
            break
        }

        if (hasCode && (!hasMath || codeStart < mathStart)) {
            // Process leading text before code block
            if (codeStart > currentIndex) {
                val leading = input.substring(currentIndex, codeStart).trim()
                if (leading.isNotEmpty()) {
                    blocks.add(ContentBlock.Paragraph(leading))
                }
            }

            // Find end of code block
            val codeContentStart = input.indexOf('\n', codeStart + 3)
            val lang = if (codeContentStart != -1 && codeContentStart > codeStart + 3) {
                input.substring(codeStart + 3, codeContentStart).trim()
            } else ""

            val searchFrom = if (codeContentStart != -1) codeContentStart + 1 else codeStart + 3
            val codeEnd = input.indexOf("```", searchFrom)

            if (codeEnd != -1) {
                val code = input.substring(searchFrom, codeEnd).trimEnd()
                blocks.add(ContentBlock.Code(code = code, language = lang))
                currentIndex = codeEnd + 3
            } else {
                // Unclosed code block
                val code = input.substring(searchFrom).trimEnd()
                blocks.add(ContentBlock.Code(code = code, language = lang))
                break
            }
        } else if (hasMath) {
            // Process leading text before math block
            if (mathStart > currentIndex) {
                val leading = input.substring(currentIndex, mathStart).trim()
                if (leading.isNotEmpty()) {
                    blocks.add(ContentBlock.Paragraph(leading))
                }
            }

            val mathEnd = input.indexOf(mathClosingDelimiter, mathStart + mathDelimiterLen)
            if (mathEnd != -1) {
                val math = input.substring(mathStart + mathDelimiterLen, mathEnd).trim()
                if (math.isNotEmpty()) {
                    blocks.add(ContentBlock.MathDisplay(latex = math))
                }
                currentIndex = mathEnd + mathDelimiterLen
            } else {
                // Unclosed math block
                val math = input.substring(mathStart + mathDelimiterLen).trim()
                if (math.isNotEmpty()) {
                    blocks.add(ContentBlock.MathDisplay(latex = math))
                }
                break
            }
        }
    }

    return if (blocks.isEmpty() && input.isNotBlank()) {
        listOf(ContentBlock.Paragraph(input.trim()))
    } else {
        blocks
    }
}
