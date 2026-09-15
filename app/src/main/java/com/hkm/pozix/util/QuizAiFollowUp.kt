package com.hkm.pozix.util

import android.content.Context
import com.hkm.pozix.data.model.Question
import com.hkm.pozix.data.model.QuizReviewItem
import com.hkm.pozix.data.model.QuizReviewPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Durable, user-confirmed draft. Nothing is uploaded until Send is pressed in chat. */
object QuizAiFollowUp {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val pending = MutableStateFlow<String?>(null)
    fun restore(context: Context) {
        if (pending.value == null) pending.value = context.getSharedPreferences("ai_review_draft", Context.MODE_PRIVATE).getString("text", null)
    }
    fun queue(context: Context, text: String) {
        context.getSharedPreferences("ai_review_draft", Context.MODE_PRIVATE).edit().putString("text", text).apply()
        pending.value = text
    }
    fun clear(context: Context) {
        context.getSharedPreferences("ai_review_draft", Context.MODE_PRIVATE).edit().remove("text").apply()
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
