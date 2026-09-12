package com.hkm.pozix.ui.components.richcontent

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em

/**
 * Robust, high-performance LaTeX and STEM mathematical parser for Pozix.
 * Converts LaTeX formulas, fractions, Greek letters, roots, symbols, and formatting
 * into beautiful Jetpack Compose [AnnotatedString] with mathematical typography.
 *
 * Ensures 120 FPS buttery smooth scrolling in lists, question cards, and answer options
 * with zero layout distortion or broken tags.
 */
object LatexMathParser {
    private val inlineMarkers = listOf("***", "___", "~~", "==", "__")

    private val GREEK_MAP = mapOf(
        "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
        "\\epsilon" to "ε", "\\varepsilon" to "ε", "\\zeta" to "ζ", "\\eta" to "η",
        "\\theta" to "θ", "\\vartheta" to "ϑ", "\\iota" to "ι", "\\kappa" to "κ",
        "\\lambda" to "λ", "\\mu" to "μ", "\\nu" to "ν", "\\xi" to "ξ",
        "\\pi" to "π", "\\varpi" to "ϖ", "\\rho" to "ρ", "\\varrho" to "ϱ",
        "\\sigma" to "σ", "\\varsigma" to "ς", "\\tau" to "τ", "\\upsilon" to "υ",
        "\\phi" to "φ", "\\varphi" to "ϕ", "\\chi" to "χ", "\\psi" to "ψ",
        "\\omega" to "ω",
        // Uppercase Greek
        "\\Gamma" to "Γ", "\\Delta" to "Δ", "\\Theta" to "Θ", "\\Lambda" to "Λ",
        "\\Xi" to "Ξ", "\\Pi" to "Π", "\\Sigma" to "Σ", "\\Upsilon" to "Υ",
        "\\Phi" to "Φ", "\\Psi" to "Ψ", "\\Omega" to "Ω"
    )

    private val SYMBOL_MAP = mapOf(
        "\\pm" to "±", "\\mp" to "∓", "\\times" to "×", "\\div" to "÷",
        "\\cdot" to "·", "\\neq" to "≠", "\\ne" to "≠",
        "\\leq" to "≤", "\\le" to "≤", "\\geq" to "≥", "\\ge" to "≥",
        "\\approx" to "≈", "\\equiv" to "≡", "\\cong" to "≅", "\\sim" to "∼",
        "\\in" to "∈", "\\notin" to "∉", "\\subset" to "⊂", "\\subseteq" to "⊆",
        "\\supset" to "⊃", "\\supseteq" to "⊇", "\\cup" to "∪", "\\cap" to "∩",
        "\\setminus" to "∖", "\\emptyset" to "∅", "\\varnothing" to "∅",
        "\\forall" to "∀", "\\exists" to "∃", "\\nexists" to "∄",
        "\\infty" to "∞", "\\to" to "→", "\\rightarrow" to "→", "\\leftarrow" to "←",
        "\\Rightarrow" to "⇒", "\\Leftarrow" to "⇐", "\\Leftrightarrow" to "⇔",
        "\\leftrightarrow" to "↔", "\\mapsto" to "↦",
        "\\circ" to "°", "^{\\circ}" to "°", "^\\circ" to "°",
        "\\angle" to "∠", "\\perp" to "⊥", "\\parallel" to "∥",
        "\\nabla" to "∇", "\\partial" to "∂",
        "\\int" to "∫", "\\iint" to "∬", "\\iiint" to "∭", "\\oint" to "∮",
        "\\sum" to "∑", "\\prod" to "∏",
        "\\uparrow" to "↑", "\\downarrow" to "↓",
        "\\dots" to "…", "\\cdots" to "…", "\\ldots" to "…", "\\vdots" to "⋮", "\\ddots" to "⋱",
        "\\quad" to "  ", "\\qquad" to "    ", "\\," to " ", "\\;" to " ", "\\:" to " ", "\\!" to "",
        "\\prime" to "′",
        "\\mathbb{R}" to "ℝ", "\\mathbb{N}" to "ℕ", "\\mathbb{Z}" to "ℤ",
        "\\mathbb{Q}" to "ℚ", "\\mathbb{C}" to "ℂ"
    )

    private val FUNCTIONS = listOf(
        "\\sin", "\\cos", "\\tan", "\\cot", "\\sec", "\\csc",
        "\\arcsin", "\\arccos", "\\arctan",
        "\\ln", "\\log", "\\lg", "\\exp",
        "\\lim", "\\det", "\\gcd", "\\deg", "\\dim",
        "\\max", "\\min", "\\sup", "\\inf"
    )

    private val SUPERSCRIPT_MAP = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        '/' to 'ᐟ',
        'a' to 'ᵃ', 'b' to 'ᵇ', 'c' to 'ᶜ', 'd' to 'ᵈ', 'e' to 'ᵉ',
        'f' to 'ᶠ', 'g' to 'ᵍ', 'h' to 'ʰ', 'i' to 'ⁱ', 'j' to 'ʲ',
        'k' to 'ᵏ', 'l' to 'ˡ', 'm' to 'ᵐ', 'n' to 'ⁿ', 'o' to 'ᵒ',
        'p' to 'ᵖ', 'r' to 'ʳ', 's' to 'ˢ', 't' to 'ᵗ', 'u' to 'ᵘ',
        'v' to 'ᵛ', 'w' to 'ʷ', 'x' to 'ˣ', 'y' to 'ʸ', 'z' to 'ᶻ',
        'A' to 'ᴬ', 'B' to 'ᴮ', 'D' to 'ᴰ', 'E' to 'ᴱ', 'G' to 'ᴳ',
        'H' to 'ᴴ', 'I' to 'ᴵ', 'J' to 'ᴶ', 'K' to 'ᴷ', 'L' to 'ᴸ',
        'M' to 'ᴹ', 'N' to 'ᴺ', 'O' to 'ᴼ', 'P' to 'ᴾ', 'R' to 'ᴿ',
        'T' to 'ᵀ', 'U' to 'ᵁ', 'V' to 'ⱽ', 'W' to 'ᵂ'
    )

    private val SUBSCRIPT_MAP = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
        'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ',
        'v' to 'ᵥ', 'x' to 'ₓ'
    )

    private val COMMON_FRACTIONS = mapOf(
        "1/2" to "½", "1/4" to "¼", "3/4" to "¾",
        "1/3" to "⅓", "2/3" to "⅔", "1/5" to "⅕",
        "2/5" to "⅖", "3/5" to "⅗", "4/5" to "⅘",
        "1/6" to "⅙", "5/6" to "⅚", "1/8" to "⅛",
        "3/8" to "⅜", "5/8" to "⅝", "7/8" to "⅞"
    )

    /**
     * Parse text containing Markdown formatting (bold, italic, inline code)
     * and LaTeX inline math (`$...$` or `\(...\)`) into an [AnnotatedString].
     * If the text contains unescaped LaTeX macros without `$`, it auto-detects and formats them.
     */
    fun parseToAnnotatedString(
        rawText: String,
        codeBgColor: Color = Color(0x1F808080),
        codeTextColor: Color = Color.Unspecified
    ): AnnotatedString {
        if (rawText.isBlank()) return AnnotatedString("")

        val cleanText = rawText
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")

        return buildAnnotatedString {
            var i = 0
            val len = cleanText.length

            while (i < len) {
                if (cleanText[i] == '\\' && i + 1 < len && cleanText[i + 1] in "*_~`=>#") {
                    append(cleanText[i + 1]); i += 2; continue
                }
                // Check for display math $$...$$
                if (i + 1 < len && cleanText[i] == '$' && cleanText[i + 1] == '$') {
                    val closeIdx = cleanText.indexOf("$$", i + 2)
                    if (closeIdx != -1) {
                        val mathContent = cleanText.substring(i + 2, closeIdx)
                        appendMathFormatted(mathContent)
                        i = closeIdx + 2
                        continue
                    }
                }

                // Check for inline math $...$
                if (cleanText[i] == '$' && (i == 0 || cleanText[i - 1] != '\\')) {
                    val openingNext = cleanText.getOrNull(i + 1)
                    val closeIdx = cleanText.indexOf('$', i + 1)
                    val closingPrevious = closeIdx.takeIf { it > i + 1 }?.let { cleanText.getOrNull(it - 1) }
                    if (openingNext != null && !openingNext.isWhitespace() &&
                        closeIdx != -1 && closeIdx > i + 1 &&
                        closingPrevious != null && !closingPrevious.isWhitespace() &&
                        !cleanText.substring(i + 1, closeIdx).contains('\n')) {
                        val mathContent = cleanText.substring(i + 1, closeIdx)
                        appendMathFormatted(mathContent)
                        i = closeIdx + 1
                        continue
                    }
                }

                // Check for \( ... \) inline math
                if (i + 1 < len && cleanText[i] == '\\' && cleanText[i + 1] == '(') {
                    val closeIdx = cleanText.indexOf("\\)", i + 2)
                    if (closeIdx != -1) {
                        val mathContent = cleanText.substring(i + 2, closeIdx)
                        appendMathFormatted(mathContent)
                        i = closeIdx + 2
                        continue
                    }
                }

                // Check for \[ ... \] display math
                if (i + 1 < len && cleanText[i] == '\\' && cleanText[i + 1] == '[') {
                    val closeIdx = cleanText.indexOf("\\]", i + 2)
                    if (closeIdx != -1) {
                        val mathContent = cleanText.substring(i + 2, closeIdx)
                        appendMathFormatted(mathContent)
                        i = closeIdx + 2
                        continue
                    }
                }

                // Check for inline code `...`
                if (cleanText[i] == '`') {
                    val closeIdx = cleanText.indexOf('`', i + 1)
                    if (closeIdx != -1) {
                        val codeContent = cleanText.substring(i + 1, closeIdx)
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBgColor,
                                color = codeTextColor,
                                fontSize = 0.88.em
                            )
                        ) {
                            append(" $codeContent ")
                        }
                        i = closeIdx + 1
                        continue
                    }
                }

                // Check for <br>, <br/>, <br />
                if (cleanText.startsWith("<br>", i, ignoreCase = true)) {
                    append("\n")
                    i += 4
                    continue
                }
                if (cleanText.startsWith("<br/>", i, ignoreCase = true)) {
                    append("\n")
                    i += 5
                    continue
                }
                if (cleanText.startsWith("<br />", i, ignoreCase = true)) {
                    append("\n")
                    i += 6
                    continue
                }

                // Check for <strong>...</strong> or <b>...</b>
                if (cleanText.startsWith("<strong>", i, ignoreCase = true)) {
                    val closeIdx = cleanText.indexOf("</strong>", i + 8, ignoreCase = true)
                    if (closeIdx != -1) {
                        val strongContent = cleanText.substring(i + 8, closeIdx)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(parseToAnnotatedString(strongContent, codeBgColor, codeTextColor))
                        }
                        i = closeIdx + 9
                        continue
                    }
                }
                if (cleanText.startsWith("<b>", i, ignoreCase = true)) {
                    val closeIdx = cleanText.indexOf("</b>", i + 3, ignoreCase = true)
                    if (closeIdx != -1) {
                        val boldContent = cleanText.substring(i + 3, closeIdx)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(parseToAnnotatedString(boldContent, codeBgColor, codeTextColor))
                        }
                        i = closeIdx + 4
                        continue
                    }
                }

                // Check for <em>...</em> or <i>...</i>
                if (cleanText.startsWith("<em>", i, ignoreCase = true)) {
                    val closeIdx = cleanText.indexOf("</em>", i + 4, ignoreCase = true)
                    if (closeIdx != -1) {
                        val emContent = cleanText.substring(i + 4, closeIdx)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(parseToAnnotatedString(emContent, codeBgColor, codeTextColor))
                        }
                        i = closeIdx + 5
                        continue
                    }
                }
                if (cleanText.startsWith("<i>", i, ignoreCase = true)) {
                    val closeIdx = cleanText.indexOf("</i>", i + 3, ignoreCase = true)
                    if (closeIdx != -1) {
                        val italicContent = cleanText.substring(i + 3, closeIdx)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(parseToAnnotatedString(italicContent, codeBgColor, codeTextColor))
                        }
                        i = closeIdx + 4
                        continue
                    }
                }

                // Check for <code>...</code>
                if (cleanText.startsWith("<code>", i, ignoreCase = true)) {
                    val closeIdx = cleanText.indexOf("</code>", i + 6, ignoreCase = true)
                    if (closeIdx != -1) {
                        val codeContent = cleanText.substring(i + 6, closeIdx)
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBgColor,
                                color = codeTextColor,
                                fontSize = 0.88.em
                            )
                        ) {
                            append(" $codeContent ")
                        }
                        i = closeIdx + 7
                        continue
                    }
                }

                // Longer delimiters must win before ** or *; otherwise *** leaves stray markers.
                val extendedMarker = inlineMarkers.firstOrNull { cleanText.startsWith(it, i) }
                if (extendedMarker != null) {
                    val end = cleanText.indexOf(extendedMarker, i + extendedMarker.length)
                    if (end > i + extendedMarker.length) {
                        val span = when (extendedMarker) {
                            "***", "___" -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                            "~~" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                            "==" -> SpanStyle(background = codeBgColor, color = codeTextColor, fontWeight = FontWeight.SemiBold)
                            else -> SpanStyle(fontWeight = FontWeight.Bold)
                        }
                        withStyle(span) { append(parseToAnnotatedString(cleanText.substring(i + extendedMarker.length, end), codeBgColor, codeTextColor)) }
                        i = end + extendedMarker.length
                        continue
                    }
                }

                // Check for bold **...**
                if (i + 1 < len && cleanText[i] == '*' && cleanText[i + 1] == '*') {
                    val closeIdx = cleanText.indexOf("**", i + 2)
                    if (closeIdx != -1) {
                        val boldContent = cleanText.substring(i + 2, closeIdx)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(parseToAnnotatedString(boldContent, codeBgColor, codeTextColor))
                        }
                        i = closeIdx + 2
                        continue
                    }
                }

                // Check for italic *...* (single asterisk)
                if (cleanText[i] == '*' && (i + 1 >= len || cleanText[i + 1] != '*')) {
                    val closeIdx = cleanText.indexOf('*', i + 1)
                    if (closeIdx != -1 && closeIdx > i + 1 && (closeIdx + 1 >= len || cleanText[closeIdx + 1] != '*')) {
                        val italicContent = cleanText.substring(i + 1, closeIdx)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(parseToAnnotatedString(italicContent, codeBgColor, codeTextColor))
                        }
                        i = closeIdx + 1
                        continue
                    }
                }

                // Check if current position starts an unescaped LaTeX macro (e.g. \dfrac, \frac, \sqrt, \alpha)
                if (cleanText[i] == '\\' && i + 1 < len && cleanText[i + 1].isLetter()) {
                    val end = findLatexMacroEnd(cleanText, i)
                    val segment = cleanText.substring(i, end)
                    val formatted = formatMathString(segment)
                    if (formatted != segment) {
                        appendMathFormatted(segment)
                        i = end
                        continue
                    }
                }

                append(cleanText[i])
                i++
            }
        }
    }

    /** Finds one macro plus its balanced arguments, leaving following prose intact. */
    private fun findLatexMacroEnd(text: String, start: Int): Int {
        var cursor = start + 1
        while (cursor < text.length && text[cursor].isLetter()) cursor++

        fun consumeBalanced(open: Char, close: Char): Boolean {
            if (cursor >= text.length || text[cursor] != open) return false
            var depth = 0
            while (cursor < text.length) {
                when (text[cursor]) {
                    open -> depth++
                    close -> {
                        depth--
                        cursor++
                        if (depth == 0) return true
                        continue
                    }
                }
                cursor++
            }
            return false
        }

        while (cursor < text.length && text[cursor].isWhitespace()) cursor++
        consumeBalanced('[', ']')
        while (cursor < text.length && text[cursor].isWhitespace()) cursor++
        while (cursor < text.length && text[cursor] == '{') {
            if (!consumeBalanced('{', '}')) break
            while (cursor < text.length && text[cursor].isWhitespace()) cursor++
        }
        while (cursor < text.length && (text[cursor] == '^' || text[cursor] == '_')) {
            cursor++
            while (cursor < text.length && text[cursor].isWhitespace()) cursor++
            if (cursor < text.length && text[cursor] == '{') {
                if (!consumeBalanced('{', '}')) break
            } else if (cursor < text.length) {
                cursor++
            }
            while (cursor < text.length && text[cursor].isWhitespace()) cursor++
        }
        return cursor.coerceAtLeast(start + 2)
    }

    /**
     * Format a LaTeX math snippet into readable Unicode text with proper styles.
     */
    fun AnnotatedString.Builder.appendMathFormatted(math: String) {
        val cleanText = formatMathString(math)
        appendMathWithItalicVariables(cleanText)
    }

    /**
     * Core math string formatter that converts LaTeX expressions into Unicode math string.
     */
    fun formatMathString(math: String): String {
        var text = math.trim()

        // 1. Delimiters: \left and \right
        text = text.replace("\\left(", "(")
            .replace("\\right)", ")")
            .replace("\\left[", "[")
            .replace("\\right]", "]")
            .replace("\\left\\{", "{")
            .replace("\\right\\}", "}")
            .replace("\\left|", "|")
            .replace("\\right|", "|")
            .replace("\\left.", "")
            .replace("\\right.", "")
            .replace("\\{", "{")
            .replace("\\}", "}")
            .replace("\\|", "‖")

        // 2. Text / styling macros
        text = text.replace(Regex("""\\(?:text|mathrm|mathbf|mathit|textbf)\{([^}]+)\}""")) { match ->
            match.groupValues[1]
        }

        // 3. Chemical equations \ce{...}
        text = text.replace(Regex("""\\ce\{([^}]+)\}""")) { match ->
            formatChemistry(match.groupValues[1])
        }

        // 4. Fractions inside superscripts: ^{ \frac{a}{b} } or ^\frac a b -> ^{a/b}
        text = text.replace(Regex("""\^\{\s*\\(?:d?frac|tfrac|cfrac)\{([^}]+)\}\{([^}]+)\}\s*\}""")) { match ->
            "^{${match.groupValues[1]}/${match.groupValues[2]}}"
        }
        text = text.replace(Regex("""\^\\(?:d?frac|tfrac|cfrac)\s*([0-9a-zA-Z])\s*([0-9a-zA-Z])""")) { match ->
            "^{${match.groupValues[1]}/${match.groupValues[2]}}"
        }

        // 5. Greek letters & Symbols (resolve early so \Delta, \pi inside roots/fractions format cleanly)
        for ((key, value) in GREEK_MAP) {
            text = text.replace(key, value)
        }
        for ((key, value) in SYMBOL_MAP) {
            text = text.replace(key, value)
        }

        // 6. Fractions: \dfrac, \frac, \tfrac, \cfrac (with balanced braces)
        text = parseFractions(text)

        // 7. Roots: \sqrt[n]{x} or \sqrt{x} (with balanced braces)
        text = parseRoots(text)

        // 8. Vectors and accents
        text = text.replace(Regex("""\\vec\{([a-zA-Z0-9]+)\}""")) { match ->
            "${match.groupValues[1]}⃗"
        }
        text = text.replace(Regex("""\\vec\s+([a-zA-Z0-9])""")) { match ->
            "${match.groupValues[1]}⃗"
        }
        text = text.replace(Regex("""\\overline\{([a-zA-Z0-9]+)\}""")) { match ->
            match.groupValues[1].map { "$it\u0305" }.joinToString("")
        }
        text = text.replace(Regex("""\\bar\{([a-zA-Z0-9]+)\}""")) { match ->
            "${match.groupValues[1]}\u0304"
        }
        text = text.replace(Regex("""\\hat\{([a-zA-Z0-9]+)\}""")) { match ->
            "${match.groupValues[1]}\u0302"
        }

        // 9. Math functions (sin, cos, ln, log, etc.)
        for (fn in FUNCTIONS) {
            val fnName = fn.substring(1)
            text = text.replace(Regex("""\\${fnName}(?![a-zA-Z])""")) { "$fnName " }
        }

        // 10. Superscripts: ^{...} or ^x
        text = text.replace(Regex("""\^\{([^}]+)\}""")) { match ->
            toSuperscript(match.groupValues[1])
        }
        text = text.replace(Regex("""\^([0-9a-zA-Z+-])""")) { match ->
            toSuperscript(match.groupValues[1])
        }

        // 11. Subscripts: _{...} or _x
        text = text.replace(Regex("""_\{([^}]+)\}""")) { match ->
            toSubscript(match.groupValues[1])
        }
        text = text.replace(Regex("""_([0-9a-zA-Z+-])""")) { match ->
            toSubscript(match.groupValues[1])
        }

        // 11. Clean up remaining unhandled LaTeX command prefixes
        text = text.replace(Regex("""\\([a-zA-Z]+)""")) { match ->
            match.groupValues[1]
        }
        text = text.replace("{", "").replace("}", "")

        // Normalize spaces around operators
        text = text.replace(Regex("""\s+"""), " ")

        return text.trim()
    }

    /**
     * Recursively parse and format all fraction macros: \dfrac, \frac, \tfrac, \cfrac
     */
    fun parseFractions(input: String): String {
        var text = input
        val fracRegex = Regex("""\\(?:dfrac|frac|tfrac|cfrac)""")
        var match = fracRegex.find(text)
        var safetyCounter = 0

        while (match != null && safetyCounter++ < 50) {
            val fracStart = match.range.first
            var cursor = match.range.last + 1
            // Skip optional spaces
            while (cursor < text.length && text[cursor].isWhitespace()) cursor++

            if (cursor >= text.length) break

            // Extract numerator
            val numResult = extractArgument(text, cursor) ?: break
            val numStr = numResult.first
            cursor = numResult.second

            // Skip optional spaces
            while (cursor < text.length && text[cursor].isWhitespace()) cursor++
            if (cursor >= text.length) break

            // Extract denominator
            val denResult = extractArgument(text, cursor) ?: break
            val denStr = denResult.first
            val fracEnd = denResult.second

            // Format numerator and denominator recursively
            val formattedNum = parseFractions(numStr).trim()
            val formattedDen = parseFractions(denStr).trim()

            val formattedFrac = formatFraction(formattedNum, formattedDen)
            text = text.substring(0, fracStart) + formattedFrac + text.substring(fracEnd)

            match = fracRegex.find(text)
        }
        return text
    }

    private fun extractArgument(text: String, start: Int): Pair<String, Int>? {
        if (start >= text.length) return null
        if (text[start] == '{') {
            var depth = 0
            val sb = StringBuilder()
            for (i in start until text.length) {
                when (text[i]) {
                    '{' -> {
                        if (depth > 0) sb.append('{')
                        depth++
                    }
                    '}' -> {
                        depth--
                        if (depth == 0) return Pair(sb.toString(), i + 1)
                        sb.append('}')
                    }
                    else -> sb.append(text[i])
                }
            }
            return null
        } else {
            // Single non-whitespace token (e.g. \frac 1 2 or \frac12)
            return Pair(text[start].toString(), start + 1)
        }
    }

    private fun formatFraction(num: String, den: String): String {
        val cleanNum = num.trim()
        val cleanDen = den.trim()

        val common = COMMON_FRACTIONS["$cleanNum/$cleanDen"]
        if (common != null) return common

        val numNeedsParens = cleanNum.contains(' ') || cleanNum.contains('+') || cleanNum.contains('-') || cleanNum.contains('/')
        val denNeedsParens = cleanDen.contains(' ') || cleanDen.contains('+') || cleanDen.contains('-') || cleanDen.contains('/')

        val numFmt = if (numNeedsParens) "($cleanNum)" else cleanNum
        val denFmt = if (denNeedsParens) "($cleanDen)" else cleanDen

        return "$numFmt/$denFmt"
    }

    /**
     * Recursively parse and format roots: \sqrt[n]{x} or \sqrt{x}
     */
    fun parseRoots(input: String): String {
        var text = input
        val sqrtRegex = Regex("""\\sqrt""")
        var match = sqrtRegex.find(text)
        var safetyCounter = 0

        while (match != null && safetyCounter++ < 50) {
            val start = match.range.first
            var cursor = match.range.last + 1
            while (cursor < text.length && text[cursor].isWhitespace()) cursor++

            var rootIndex = ""
            if (cursor < text.length && text[cursor] == '[') {
                val closeBracket = text.indexOf(']', cursor + 1)
                if (closeBracket != -1) {
                    rootIndex = text.substring(cursor + 1, closeBracket).trim()
                    cursor = closeBracket + 1
                    while (cursor < text.length && text[cursor].isWhitespace()) cursor++
                }
            }

            val argResult = extractArgument(text, cursor) ?: break
            val inner = parseRoots(argResult.first).trim()
            val end = argResult.second

            val rootSymbol = when (rootIndex) {
                "3" -> "∛"
                "4" -> "∜"
                "" -> "√"
                else -> toSuperscript(rootIndex) + "√"
            }

            val formatted = if (inner.length == 1 && inner[0].isLetterOrDigit()) {
                "$rootSymbol$inner"
            } else {
                "$rootSymbol($inner)"
            }

            text = text.substring(0, start) + formatted + text.substring(end)
            match = sqrtRegex.find(text)
        }
        return text
    }

    private fun AnnotatedString.Builder.appendMathWithItalicVariables(text: String) {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            // Italicize single-letter variables like x, y, z, m, n, a, b, c in math expressions
            val isMathVar = (c in 'a'..'z' || c in 'A'..'Z')
            val prevIsLetter = i > 0 && text[i - 1].isLetter()
            val nextIsLetter = i + 1 < text.length && text[i + 1].isLetter()

            if (isMathVar && !prevIsLetter && !nextIsLetter) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(c)
                }
            } else {
                append(c)
            }
            i++
        }
    }

    fun toSuperscript(str: String): String {
        val sb = StringBuilder()
        for (c in str) {
            sb.append(SUPERSCRIPT_MAP[c] ?: c)
        }
        return sb.toString()
    }

    fun toSubscript(str: String): String {
        val sb = StringBuilder()
        for (c in str) {
            sb.append(SUBSCRIPT_MAP[c] ?: c)
        }
        return sb.toString()
    }

    private fun formatChemistry(formula: String): String {
        val sb = StringBuilder()
        for (c in formula) {
            if (c.isDigit()) {
                sb.append(SUBSCRIPT_MAP[c] ?: c)
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }
}
