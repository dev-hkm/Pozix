package com.hkm.pozix.util

/** Small, adaptive frame buffer. Never split a UTF-16 surrogate pair. */
object StreamPresentation {
    fun splitThinking(raw: String, complete: Boolean = false): Pair<String, String> {
        val open = "<think>"
        val close = "</think>"
        fun withoutPartialTag(text: String, tag: String): String {
            for (size in tag.length - 1 downTo 1) {
                if (text.endsWith(tag.take(size))) return text.dropLast(size)
            }
            return text
        }
        val start = raw.indexOf(open)
        if (start < 0) return (if (complete) raw else withoutPartialTag(raw, open)) to ""
        val end = raw.indexOf(close, start + open.length)
        return if (end < 0) {
            raw.substring(0, start) to withoutPartialTag(raw.substring(start + open.length), close)
        } else {
            (raw.substring(0, start) + raw.substring(end + close.length)).trimStart() to
                raw.substring(start + open.length, end)
        }
    }
    fun nextRevealIndex(text: String, previous: Int): Int {
        val start = previous.coerceIn(0, text.length)
        val remaining = text.length - start
        var end = (start + maxOf(1, (remaining + 7) / 8)).coerceAtMost(text.length)
        if (end in 1 until text.length && text[end - 1].isHighSurrogate() && text[end].isLowSurrogate()) end++
        return end
    }
}
