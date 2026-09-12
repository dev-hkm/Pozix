package com.hkm.pozix.data.model

import kotlinx.serialization.Serializable

/** Structured result artifact sent to the AI review flow. It is not quiz-generation JSON. */
@Serializable
data class QuizReviewPayload(
    val title: String,
    val score: Int,
    val totalQuestions: Int,
    val elapsedTimeMillis: Long,
    val items: List<QuizReviewItem>,
    val reviewId: String = ""
)

@Serializable
data class QuizReviewItem(
    val question: String,
    val options: List<String>,
    val selectedIndex: Int? = null,
    val correctIndex: Int,
    val explanation: String? = null
) {
    val isCorrect: Boolean get() = selectedIndex != null && selectedIndex == correctIndex
}
