package com.hkm.pozix.util

import com.hkm.pozix.data.model.QuizValidationResult

object AiQuizOutput {
    data class Artifact(val json: String, val displayText: String)
    fun requested(text: String): Boolean = Regex(
        "(?i)(quiz|trắc nghiệm|câu hỏi|\\d+\\s*câu|question[s]?|bài kiểm tra)"
    ).containsMatchIn(text) && Regex("(?i)(tạo|soạn|generate|create|make|give me|cho tôi|cho mình|\\d+\\s*(câu|questions?))")
        .containsMatchIn(text) && !Regex("(?i)(không tạo|đừng tạo|do not generate|don't generate)").containsMatchIn(text)

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
