package com.hkm.pozix.util

object StreamingMarkdown {
    fun split(text: String): Pair<String, String> {
        var fence: String? = null
        var math = false
        var offset = 0
        var boundary = 0
        for (line in text.split('\n')) {
            val trimmed = line.trim()
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                val marker = trimmed.take(3)
                if (fence == null) fence = marker else if (fence == marker) fence = null
            } else if (fence == null) {
                var index = 0
                while (index < line.length - 1) {
                    if (line.startsWith("$$", index) || line.startsWith("\\[", index) || line.startsWith("\\]", index)) {
                        math = !math; index += 2
                    } else index++
                }
                if (trimmed.isEmpty() && !math && offset < text.length) boundary = offset + 1
            }
            offset += line.length + 1
        }
        return text.take(boundary) to text.drop(boundary)
    }
}
