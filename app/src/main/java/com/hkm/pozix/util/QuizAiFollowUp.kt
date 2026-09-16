package com.hkm.pozix.util

import android.content.Context
import com.hkm.pozix.data.model.Question
import com.hkm.pozix.data.model.QuizReviewItem
import com.hkm.pozix.data.model.QuizReviewPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** In-memory, user-confirmed draft. Nothing is uploaded until Send is pressed in chat. */
object QuizAiFollowUp {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val pending = MutableStateFlow<String?>(null)
    // A review draft is navigation state, not durable work. Restoring it after a
    // cold start used to unexpectedly hijack the user into AI Chat.
    fun restore(context: Context) = Unit
    fun queue(context: Context, text: String) {
        pending.value = text
    }
    fun clear(context: Context) {
        pending.value = null
    }

    fun decode(payload: String): QuizReviewPayload? = runCatching {
        json.decodeFromString<QuizReviewPayload>(payload)
    }.getOrNull()

    fun report(
        title: String,
        questions: List<Question>,
        answers: Map<Int, Int>,
        textAnswers: Map<Int, String> = emptyMap(),
        score: Int,
        elapsed: Long
    ): String {
        val payload = QuizReviewPayload(
            title = title,
            score = score,
            totalQuestions = questions.size,
            elapsedTimeMillis = elapsed,
            reviewId = java.util.UUID.randomUUID().toString(),
            items = questions.mapIndexed { index, question ->
                when (question) {
                    is Question.SingleChoice -> {
                        QuizReviewItem(
                            question = question.question,
                            options = question.options,
                            selectedIndex = answers[index],
                            correctIndex = question.correctIndex,
                            explanation = question.explanation
                        )
                    }
                    is Question.TrueFalse -> {
                        QuizReviewItem(
                            question = question.question,
                            options = listOf("True", "False"),
                            selectedIndex = answers[index],
                            correctIndex = if (question.correctAnswer) 0 else 1,
                            explanation = question.explanation
                        )
                    }
                    is Question.ShortAnswer -> {
                        QuizReviewItem(
                            question = question.question,
                            options = emptyList(),
                            selectedIndex = null,
                            correctIndex = -1,
                            explanation = question.explanation,
                            userTextAnswer = textAnswers[index],
                            correctTextAnswer = question.correctAnswer
                        )
                    }
                }
            }
        )
        return json.encodeToString(payload)
    }

    fun report(title: String, questions: List<Question>, answers: Map<Int, Int>, score: Int, elapsed: Long): String =
        report(title, questions, answers, emptyMap(), score, elapsed)
}
