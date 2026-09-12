package com.hkm.pozix.util

import com.hkm.pozix.data.model.QuizValidationResult

object AiQuizOutput {
    data class Artifact(val json: String, val displayText: String)

    /**
     * Quiz generation is an explicit tool invocation, not a keyword detector.
     * Mentions of a quiz/question while asking for an explanation, a review, or a
     * capability answer must remain ordinary chat.
     */
    fun requested(text: String): Boolean {
        val normalized = text.trim()
        if (normalized.isBlank()) return false

        val negative = Regex(
            "(?i)(không\\s+(?:cần|tạo|soạn)|đừng\\s+(?:tạo|soạn)|" +
                "do not|don't|no new quiz|review|giải thích|giảng|dạy|" +
                "phân tích|tóm tắt|giải bài|solve|explain|teach|summarize|" +
                "cho tôi biết|tell me|how many|bao nhiêu|giới hạn|limit|" +
                "có thể.*(?:không|được)|can I|could I|is it possible)"
        )
        if (negative.containsMatchIn(normalized)) return false

        val quizNoun = Regex(
            "(?i)(\\bquiz\\b|trắc\\s*nghiệm|bài\\s*kiểm\\s*tra|đề\\s*(?:thi|kiểm\\s*tra)|" +
                "\\b\\d+\\s*(?:câu|questions?)\\b|\\bquestions?\\b)"
        )
        if (!quizNoun.containsMatchIn(normalized)) return false

        // Strong creation verbs cover Vietnamese and English requests. "cho tôi"
        // is accepted only with a quiz noun/count, and capability questions were
        // rejected above before reaching this branch.
        val createVerb = Regex(
            "(?i)(\\btạo\\b|\\bsoạn\\b|\\bgenerate\\b|\\bcreate\\b|\\bbuild\\b|" +
                "\\bprepare\\b|\\bmake\\b|\\bgive\\s+me\\b|\\bcho\\s+(?:tôi|mình)\\b|" +
                "\\blàm\\s+(?:cho\\s+)?(?:tôi|mình)\\b|\\bra\\s+(?:đề|câu))"
        )
        return createVerb.containsMatchIn(normalized)
    }

    /** True after the stream exposes enough schema markers to identify a quiz artifact. */
    fun looksLikeQuizArtifact(text: String): Boolean {
        val normalized = text.lowercase()
        return (normalized.contains("\"title\"") && normalized.contains("\"questions\"")) ||
            (normalized.contains("\"type\"") && normalized.contains("\"question\"") &&
                (normalized.contains("\"options\"") || normalized.contains("\"correctanswer\"") || normalized.contains("\"correctindex\"")))
    }

    /** Returns only safe prose while a quiz JSON artifact is still arriving. */
    fun visibleWhileStreaming(text: String, expectedQuiz: Boolean): String {
        // A normal assistant turn is never allowed to disappear just because it
        // happens to contain JSON-looking words. Only the active quiz tool may
        // hide its transport artifact.
        if (!expectedQuiz) return text
        val fence = Regex("```(?:json)?\\s*", RegexOption.IGNORE_CASE).find(text)?.range?.first
        val objectStart = text.indexOf('{').takeIf { it >= 0 }
        val cutAt = listOfNotNull(fence, objectStart).minOrNull() ?: return text
        return text.substring(0, cutAt).trimEnd()
    }

    /** Scan complete objects, respecting escaped quotes and braces inside JSON strings. */
    fun extract(text: String): Artifact? {
        // Prose may contain unmatched braces; a JSON fence is a stronger starting point.
        for (fence in Regex("```json\\s*", RegexOption.IGNORE_CASE).findAll(text)) {
            scan(text, fence.range.last + 1)?.let { return it }
        }
        return scan(text, 0)
    }

    private fun scan(text: String, offset: Int): Artifact? {
        var start = -1
        var depth = 0
        var quoted = false
        var escaped = false
        for (i in offset until text.length) {
            val c = text[i]
            if (start < 0) {
                if (c == '{') { start = i; depth = 1 }
                continue
            }
            if (quoted) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') quoted = false
            } else when (c) {
                '"' -> quoted = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        val json = text.substring(start, i + 1)
                        if (QuizJsonParser.parseAndValidate(json) is QuizValidationResult.Success) {
                            var before = text.substring(0, start)
                            var after = text.substring(i + 1)
                            before = before.replace(Regex("```(?:json)?\\s*$", RegexOption.IGNORE_CASE), "")
                            after = after.replace(Regex("^\\s*```"), "")
                            return Artifact(json, (before + after).trim())
                        }
                        start = -1
                    }
                }
            }
        }
        return null
    }
}
