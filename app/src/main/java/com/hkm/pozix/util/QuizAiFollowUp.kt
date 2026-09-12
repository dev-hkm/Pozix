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
    fun report(title: String, questions: List<Question>, answers: Map<Int, Int>, score: Int, elapsed: Long): String {
        val payload = QuizReviewPayload(
            title = title,
            score = score,
            totalQuestions = questions.size,
            elapsedTimeMillis = elapsed,
            items = questions.mapIndexed { index, question ->
                val options = when (question) {
                    is Question.SingleChoice -> question.options
                    is Question.TrueFalse -> listOf("True", "False")
                }
                val correct = when (question) {
                    is Question.SingleChoice -> question.correctIndex
                    is Question.TrueFalse -> if (question.correctAnswer) 0 else 1
                }
                QuizReviewItem(question.question, options, answers[index], correct, question.explanation)
            }
        )
        return json.encodeToString(payload)
    }
}
