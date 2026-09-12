package com.hkm.pozix

import com.hkm.pozix.util.AiQuizOutput
import com.hkm.pozix.util.StreamingMarkdown
import com.hkm.pozix.data.model.ChatMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class AiQuizOutputTest {
    private val quiz = """{"title":"Python","questions":[{"type":"single_choice","question":"What does {x} mean? Use \"quotes\".","options":["A","B"],"correctIndex":0}]}"""

    @Test fun extractsNestedJsonAndPreservesSurroundingProse() {
        val artifact = AiQuizOutput.extract("Before\n```json\n$quiz\n```\nAfter")!!
        assertEquals(quiz, artifact.json)
        assertTrue(artifact.displayText.contains("Before"))
        assertTrue(artifact.displayText.contains("After"))
        assertFalse(artifact.displayText.contains("```"))
    }
    @Test fun rejectsClaimsAndInvalidArtifacts() {
        assertNull(AiQuizOutput.extract("Tôi đã tạo 15 câu hỏi Python."))
        assertNull(AiQuizOutput.extract("```json\n{\"title\":\"Broken\",\"questions\":[]}\n```"))
        assertNull(AiQuizOutput.extract(quiz.dropLast(1)))
    }
    @Test fun skipsUnrelatedObjects() {
        assertEquals(quiz, AiQuizOutput.extract("{\"example\":true}\n$quiz")!!.json)
    }
    @Test fun recognizesVietnameseRequest() {
        assertFalse(AiQuizOutput.requested("không hơn 25 câu được?"))
        assertFalse(AiQuizOutput.requested("Can I have more than 25 questions?"))
        assertFalse(AiQuizOutput.requested("Review my completed quiz. Do not create a new quiz; explain my mistakes."))
        assertTrue(AiQuizOutput.requested("Tạo cho tôi 200 câu hỏi Python"))
        assertTrue(AiQuizOutput.requested("tôi muốn học Python cơ bản, tạo cho tôi 1 khoá, tầm 15 câu trước đi"))
        assertFalse(AiQuizOutput.requested("đừng tạo quiz"))
        assertFalse(AiQuizOutput.requested("Xin chào"))
        assertFalse(AiQuizOutput.requested("Giải thích câu hỏi trong quiz này"))
        assertFalse(AiQuizOutput.requested("Có thể tạo quiz hơn 25 câu được không?"))
        assertFalse(AiQuizOutput.requested("Hãy giải thích JSON schema của quiz này"))
        assertEquals(quiz, AiQuizOutput.extract("A stray { brace\n```json\n$quiz\n```")!!.json)
    }
    @Test fun artifactSurvivesHistoryRoundTripAndOldHistoryLoads() {
        val message = ChatMessage(role = "model", text = "Ready", quizJson = quiz)
        assertEquals(message, Json.decodeFromString<ChatMessage>(Json.encodeToString(message)))
        assertNull(Json.decodeFromString<ChatMessage>("""{"role":"model","text":"old"}""").quizJson)
    }
    @Test fun streamingNeverPromotesUnclosedCodeOrMath() {
        for (tail in listOf("```python\nprint(1)\n\n", "$$\nx + y\n\n", "\\[\nx + y\n\n")) {
            val input = "Hello\n\n$tail"
            val (prefix, pending) = StreamingMarkdown.split(input)
            assertEquals("Hello\n\n", prefix)
            assertEquals(input, prefix + pending)
        }
    }

    @Test fun quizJsonIsHiddenWhileTheArtifactStreams() {
        val raw = "Đang chuẩn bị quiz...\n```json\n{\"title\":\"Test\",\"questions\":["
        assertTrue(AiQuizOutput.looksLikeQuizArtifact(raw))
        assertEquals("Đang chuẩn bị quiz...", AiQuizOutput.visibleWhileStreaming(raw, expectedQuiz = true))
        assertEquals(raw, AiQuizOutput.visibleWhileStreaming(raw, expectedQuiz = false))
    }
    @Test fun streamingPreservesEveryCharacterAcrossAllPrefixes() {
        val text = "Hello 😀\n\n```kotlin\nval x = 1\n```\n\n$$ x $$\n\nEnd"
        for (size in 0..text.length) {
            val input = text.take(size)
            val parts = StreamingMarkdown.split(input)
            assertEquals(input, parts.first + parts.second)
        }
        assertTrue(StreamingMarkdown.split(text).first.contains("val x"))
    }
}
