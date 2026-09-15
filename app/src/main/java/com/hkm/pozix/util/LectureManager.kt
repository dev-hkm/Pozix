package com.hkm.pozix.util

import com.hkm.pozix.ui.components.richcontent.ContentBlock
import com.hkm.pozix.ui.components.richcontent.LatexMathParser
import com.hkm.pozix.ui.components.richcontent.parseContentBlocks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LectureData(
    val title: String,
    val content: String,
    val precomputedBlocks: List<ContentBlock>? = null
)

/**
 * Global reactive state holder for opening and reading theory lectures & study notes in Pozix.
 * Features asynchronous background pre-parsing so heavy Markdown, LaTeX, and Lucide icons
 * are completely resolved BEFORE the screen navigation transition runs ("load xong mới vào"),
 * ensuring a rock-solid 60/120 FPS animation without frame drops or lag.
 */
object LectureManager {
    private val _currentLecture = MutableStateFlow<LectureData?>(null)
    val currentLecture = _currentLecture.asStateFlow()

    private val _isPreparing = MutableStateFlow(false)
    val isPreparing = _isPreparing.asStateFlow()

    fun openLecture(title: String, content: String) {
        _currentLecture.value = LectureData(title = title, content = content)
    }

    /**
     * Pre-computes all heavy block parsing on Dispatchers.Default,
     * and only calls [onReady] when completely prepared so the screen transition
     * has zero lag.
     */
    fun openLectureWithPreload(
        title: String,
        content: String,
        scope: CoroutineScope,
        onReady: () -> Unit
    ) {
        if (_isPreparing.value) return // Guard against double taps
        _isPreparing.value = true

        scope.launch {
            try {
                val blocks = withContext(Dispatchers.Default) {
                    val parsed = parseContentBlocks(content)
                    // Pre-warm parsing for text blocks so AnnotatedStrings are pre-calculated
                    parsed.forEach { block ->
                        when (block) {
                            is ContentBlock.Paragraph -> LatexMathParser.parseToAnnotatedString(block.text)
                            is ContentBlock.Heading -> LatexMathParser.parseToAnnotatedString(block.text)
                            is ContentBlock.ListItem -> LatexMathParser.parseToAnnotatedString(block.text)
                            is ContentBlock.Quote -> LatexMathParser.parseToAnnotatedString(block.text)
                            else -> {}
                        }
                    }
                    parsed
                }
                _currentLecture.value = LectureData(
                    title = title,
                    content = content,
                    precomputedBlocks = blocks
                )
                withContext(Dispatchers.Main) {
                    onReady()
                }
            } catch (e: Exception) {
                // Fallback to direct navigation if pre-warm fails
                _currentLecture.value = LectureData(title = title, content = content)
                withContext(Dispatchers.Main) {
                    onReady()
                }
            } finally {
                _isPreparing.value = false
            }
        }
    }

    fun clear() {
        _currentLecture.value = null
        _isPreparing.value = false
    }
}
