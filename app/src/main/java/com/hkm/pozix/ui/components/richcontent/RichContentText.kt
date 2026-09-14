package com.hkm.pozix.ui.components.richcontent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.pozix.ui.theme.readableContentColorFor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextAlign

/**
 * Represents a parsed segment of rich educational STEM content.
 */
@Composable
private fun rememberStyledInline(text: String): androidx.compose.ui.text.AnnotatedString {
    val background = MaterialTheme.colorScheme.tertiaryContainer
    val foreground = readableContentColorFor(
        background = background,
        preferred = MaterialTheme.colorScheme.onTertiaryContainer
    )
    return remember(text, background, foreground) { LatexMathParser.parseToAnnotatedString(text, background, foreground) }
}

sealed interface ContentBlock {
    data class Paragraph(val text: String) : ContentBlock
    data class Heading(val level: Int, val text: String) : ContentBlock
    data class ListItem(val bullet: String, val text: String, val depth: Int = 0) : ContentBlock
    data class Quote(val text: String) : ContentBlock
    data class Code(val code: String, val language: String) : ContentBlock
    data class MathDisplay(val latex: String) : ContentBlock
    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>,
        val alignments: List<TextAlign>
    ) : ContentBlock
    object Divider : ContentBlock
}

/**
 * Universal rich educational content component for Pozix.
 * Intelligently handles:
 * - Markdown Headings (#, ##, ###)
 * - Bulleted & numbered lists (-, *, 1.)
 * - Markdown Code Blocks (```lang ... ```)
 * - Display Math Formulas ($$ ... $$ or \[ ... \])
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
    inlineOnly: Boolean = false,
    quoteDepth: Int = 0,
    blocksOverride: List<ContentBlock>? = null
) {
    if (text.isBlank()) return

    if (inlineOnly && !text.contains("```")) {
        // Fast path for answer options (A, B, C, D)
        val annotated = rememberStyledInline(text)
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

    val blocks = blocksOverride ?: remember(text) { parseContentBlocks(text) }

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is ContentBlock.Quote -> {
                    Row(Modifier.fillMaxWidth().background(textColor.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                        .padding(10.dp), verticalAlignment = Alignment.Top) {
                        Text("▎", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        if (quoteDepth < 8) RichContentText(block.text, Modifier.weight(1f), textColor = textColor,
                            fontSize = fontSize, fontWeight = fontWeight, lineHeight = lineHeight, style = style, quoteDepth = quoteDepth + 1)
                        else Text(block.text, Modifier.weight(1f), color = textColor, style = style)
                    }
                }
                is ContentBlock.Heading -> {
                    val (headingStyle, topPad) = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp) to 10.dp
                        2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp) to 8.dp
                        3 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 16.5.sp, lineHeight = 22.sp) to 6.dp
                        4 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp) to 5.dp
                        5 -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.5.sp, lineHeight = 20.sp) to 4.dp
                        else -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp) to 4.dp
                    }
                    val annotated = rememberStyledInline(block.text)
                    Text(
                        text = annotated,
                        color = MaterialTheme.colorScheme.primary,
                        style = headingStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = topPad, bottom = 2.dp)
                    )
                }
                is ContentBlock.ListItem -> {
                    val annotated = rememberStyledInline(block.text)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (2 + block.depth * 12).dp, top = 2.dp, bottom = 2.dp, end = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = block.bullet,
                            color = MaterialTheme.colorScheme.primary,
                            style = style.copy(fontWeight = FontWeight.Bold, fontSize = fontSize),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = annotated,
                            color = textColor,
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            lineHeight = lineHeight,
                            style = style,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is ContentBlock.Paragraph -> {
                    val annotated = rememberStyledInline(block.text)
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
                is ContentBlock.Table -> {
                    MarkdownTableView(
                        headers = block.headers,
                        rows = block.rows,
                        alignments = block.alignments,
                        textColor = textColor,
                        fontSize = 14.sp
                    )
                }
                is ContentBlock.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }

            if (index < blocks.lastIndex) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

private fun isTableSeparator(line: String): Boolean {
    val trimmed = line.trim()
    if (!trimmed.contains("-")) return false
    return trimmed.matches(Regex("""^\|?\s*:?-+:?\s*(\|(\s*:?-+:?\s*))+\|?$"""))
}

private fun parseTableRow(line: String): List<String> {
    var trimmed = line.trim()
    if (trimmed.startsWith("|")) trimmed = trimmed.substring(1)
    if (trimmed.endsWith("|")) trimmed = trimmed.substring(0, trimmed.length - 1)
    return trimmed.split("|").map { it.trim() }
}

private fun parseTableAlignments(line: String): List<TextAlign> {
    val cells = parseTableRow(line)
    return cells.map { c ->
        when {
            c.startsWith(":") && c.endsWith(":") -> TextAlign.Center
            c.endsWith(":") -> TextAlign.End
            else -> TextAlign.Start
        }
    }
}

/**
 * Parses normal markdown text lines into Headings, ListItems, Paragraphs, and Tables.
 */
private fun parseMarkdownText(text: String): List<ContentBlock> {
    val results = mutableListOf<ContentBlock>()
    val lines = text.lines()
    val currentParagraph = StringBuilder()

    fun flushParagraph() {
        val trimmed = currentParagraph.toString().trim()
        if (trimmed.isNotEmpty()) {
            results.add(ContentBlock.Paragraph(trimmed))
            currentParagraph.clear()
        }
    }

    var lineIdx = 0
    while (lineIdx < lines.size) {
        val rawLine = lines[lineIdx]
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) {
            flushParagraph()
            lineIdx++
            continue
        }

        // Detect Markdown Table: header row contains | and next line is table separator
        if (trimmed.contains("|") && lineIdx + 1 < lines.size && isTableSeparator(lines[lineIdx + 1])) {
            flushParagraph()
            val headers = parseTableRow(trimmed)
            val aligns = parseTableAlignments(lines[lineIdx + 1])
            val rows = mutableListOf<List<String>>()
            lineIdx += 2
            while (lineIdx < lines.size && lines[lineIdx].trim().contains("|")) {
                val rowLine = lines[lineIdx].trim()
                if (rowLine.isNotEmpty() && !isTableSeparator(rowLine)) {
                    rows.add(parseTableRow(rowLine))
                }
                lineIdx++
            }
            if (headers.isNotEmpty() && rows.isNotEmpty()) {
                results.add(ContentBlock.Table(headers, rows, aligns))
            }
            continue
        }

        when {
            trimmed.startsWith(">") -> {
                flushParagraph()
                val quote = mutableListOf<String>()
                while (lineIdx < lines.size && lines[lineIdx].trimStart().startsWith(">")) {
                    quote.add(lines[lineIdx].trimStart().removePrefix(">").removePrefix(" "))
                    lineIdx++
                }
                results.add(ContentBlock.Quote(quote.joinToString("\n")))
                continue
            }
            trimmed.matches(Regex("""^([-*_])\s*(\1\s*){2,}$""")) -> {
                flushParagraph()
                results.add(ContentBlock.Divider)
            }
            trimmed.matches(Regex("""^#{1,6}\s+.*""")) -> {
                flushParagraph()
                val hashes = trimmed.takeWhile { it == '#' }
                val level = hashes.length.coerceIn(1, 6)
                val headingText = trimmed.substring(hashes.length).trim()
                results.add(ContentBlock.Heading(level, headingText))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") || trimmed.startsWith("+ ") -> {
                flushParagraph()
                val itemText = trimmed.substring(2).trim()
                val task = Regex("^\\[([ xX])]\\s+(.*)$").matchEntire(itemText)
                results.add(ContentBlock.ListItem(if (task == null) "•" else if (task.groupValues[1] == " ") "☐" else "☑",
                    task?.groupValues?.get(2) ?: itemText, (rawLine.length - rawLine.trimStart().length).div(2).coerceAtMost(6)))
            }
            trimmed.matches(Regex("""^\d+[\.\)]\s+.*""")) -> {
                flushParagraph()
                val prefix = trimmed.takeWhile { it.isDigit() || it == '.' || it == ')' }
                val itemText = trimmed.removePrefix(prefix).trim()
                results.add(ContentBlock.ListItem(prefix, itemText))
            }
            else -> {
                if (currentParagraph.isNotEmpty()) {
                    currentParagraph.append("\n")
                }
                currentParagraph.append(rawLine)
            }
        }
        lineIdx++
    }
    flushParagraph()
    return results
}

/**
 * Parse an HTML table string (<table>...</table>) into a structured [ContentBlock.Table].
 */
private fun parseHtmlTable(tableHtml: String): ContentBlock.Table? {
    val trRegex = Regex("""<tr[^>]*>(.*?)</tr>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    val cellRegex = Regex("""<(?:th|td)[^>]*>(.*?)</(?:th|td)>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    val thRegex = Regex("""<th[^>]*>(.*?)</th>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    val tagStripRegex = Regex("""<[^>]+>""")

    fun cleanCell(html: String): String {
        return html.replace(tagStripRegex, "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .trim()
    }

    var headers: List<String> = emptyList()
    val rows = mutableListOf<List<String>>()

    for (trMatch in trRegex.findAll(tableHtml)) {
        val trContent = trMatch.groupValues[1]
        val cells = cellRegex.findAll(trContent).map { cleanCell(it.groupValues[1]) }.toList()
        if (cells.isEmpty()) continue

        val hasTh = thRegex.containsMatchIn(trContent)
        if (hasTh && headers.isEmpty()) {
            headers = cells
        } else if (headers.isEmpty()) {
            // First row without th, tentatively treat as header
            headers = cells
        } else {
            rows.add(cells)
        }
    }

    if (headers.isEmpty() && rows.isNotEmpty()) {
        headers = rows.removeAt(0)
    }

    if (headers.isEmpty() && rows.isEmpty()) return null

    val colCount = maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 0)
    val paddedHeaders = if (headers.size < colCount) {
        headers + List(colCount - headers.size) { "" }
    } else headers

    val paddedRows = rows.map { r ->
        if (r.size < colCount) r + List(colCount - r.size) { "" } else r
    }

    val alignments = List(colCount) { TextAlign.Start }
    return ContentBlock.Table(headers = paddedHeaders, rows = paddedRows, alignments = alignments)
}

/**
 * Split text into Paragraphs/Headings/Lists, Code Blocks, Display Math blocks, and HTML Tables.
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
        val mathDelimiterLen = 2
        val mathClosingDelimiter = if (isBracketMath) "\\]" else "$$"

        // Check for HTML table <table ...> ... </table>
        val tableStart = input.indexOf("<table", currentIndex, ignoreCase = true)
        val tableClose = if (tableStart != -1) input.indexOf("</table>", tableStart, ignoreCase = true) else -1
        val hasTable = tableStart != -1 && tableClose != -1

        // Find earliest delimiter among code, math, and html table
        val validPositions = mutableListOf<Pair<Int, String>>()
        if (codeStart != -1) validPositions.add(codeStart to "code")
        if (mathStart != -1) validPositions.add(mathStart to "math")
        if (hasTable) validPositions.add(tableStart to "table")

        if (validPositions.isEmpty()) {
            // Remainder is text (paragraphs, headings, lists)
            val remaining = input.substring(currentIndex).trim()
            if (remaining.isNotEmpty()) {
                blocks.addAll(parseMarkdownText(remaining))
            }
            break
        }

        val earliest = validPositions.minByOrNull { it.first }!!
        val earliestType = earliest.second
        val earliestPos = earliest.first

        // Process leading text before the block
        if (earliestPos > currentIndex) {
            val leading = input.substring(currentIndex, earliestPos).trim()
            if (leading.isNotEmpty()) {
                blocks.addAll(parseMarkdownText(leading))
            }
        }

        when (earliestType) {
            "code" -> {
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
            }
            "math" -> {
                val mathEnd = input.indexOf(mathClosingDelimiter, mathStart + mathDelimiterLen)
                if (mathEnd != -1) {
                    val math = input.substring(mathStart + mathDelimiterLen, mathEnd).trim()
                    if (math.isNotEmpty()) {
                        blocks.add(ContentBlock.MathDisplay(latex = math))
                    }
                    currentIndex = mathEnd + mathDelimiterLen
                } else {
                    // An incomplete response must stay readable text. Sending an
                    // unclosed delimiter to KaTeX makes the parser swallow every
                    // later character (including a transport error) into one huge
                    // formula and can produce the clipped line seen in chat.
                    val remaining = input.substring(mathStart).trim()
                    if (remaining.isNotEmpty()) blocks.addAll(parseMarkdownText(remaining))
                    break
                }
            }
            "table" -> {
                val tableEnd = tableClose + 8 // length of "</table>"
                val tableHtml = input.substring(tableStart, tableEnd)
                val parsedTable = parseHtmlTable(tableHtml)
                if (parsedTable != null) {
                    blocks.add(parsedTable)
                } else {
                    blocks.addAll(parseMarkdownText(tableHtml))
                }
                currentIndex = tableEnd
            }
        }
    }

    return if (blocks.isEmpty() && input.isNotBlank()) {
        parseMarkdownText(input.trim())
    } else {
        blocks
    }
}

/**
 * Modern scrollable Markdown Table View.
 */
@Composable
fun MarkdownTableView(
    headers: List<String>,
    rows: List<List<String>>,
    alignments: List<TextAlign>,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 13.5.sp
) {
    if (headers.isEmpty()) return

    val scrollState = rememberScrollState()

    // Base column widths computed from content length
    val baseColWidths = remember(headers, rows) {
        headers.indices.map { colIdx ->
            val headerLen = headers.getOrElse(colIdx) { "" }.length
            val maxRowCellLen = rows.maxOfOrNull { r -> r.getOrElse(colIdx) { "" }.length } ?: 0
            val maxLen = maxOf(headerLen, maxRowCellLen)
            when {
                maxLen <= 4 -> 56.dp
                maxLen <= 8 -> 88.dp
                maxLen <= 16 -> 120.dp
                maxLen <= 30 -> 160.dp
                else -> 220.dp
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val availableWidth = maxWidth
            val totalBaseWidth = baseColWidths.fold(0.dp) { acc, w -> acc + w }
            val finalWidths = remember(baseColWidths, availableWidth) {
                if (availableWidth.value.isFinite() && totalBaseWidth < availableWidth && totalBaseWidth > 0.dp) {
                    val scale = (availableWidth / totalBaseWidth).coerceAtMost(3f)
                    baseColWidths.map { it * scale }
                } else {
                    baseColWidths
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Column {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        headers.forEachIndexed { colIdx, headerText ->
                            val align = alignments.getOrElse(colIdx) { TextAlign.Start }
                            val width = finalWidths.getOrElse(colIdx) { 100.dp }
                            val annotated = remember(headerText) { LatexMathParser.parseToAnnotatedString(headerText) }
                            Box(
                                modifier = Modifier
                                    .width(width)
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = annotated,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = fontSize,
                                    textAlign = align,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Data Rows
                    rows.forEachIndexed { rowIdx, rowCells ->
                        val rowBg = if (rowIdx % 2 == 1) {
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.35f)
                        } else {
                            Color.Transparent
                        }

                        Row(
                            modifier = Modifier
                                .background(rowBg)
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            headers.indices.forEach { colIdx ->
                                val cellText = rowCells.getOrElse(colIdx) { "" }
                                val align = alignments.getOrElse(colIdx) { TextAlign.Start }
                                val width = finalWidths.getOrElse(colIdx) { 100.dp }
                                val annotated = remember(cellText) { LatexMathParser.parseToAnnotatedString(cellText) }
                                Box(
                                    modifier = Modifier
                                        .width(width)
                                        .padding(horizontal = 12.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = annotated,
                                        color = textColor,
                                        fontSize = fontSize,
                                        textAlign = align,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        if (rowIdx < rows.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }
        }
    }
}
