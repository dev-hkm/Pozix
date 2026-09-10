package com.hkm.pozix.data.model

import kotlinx.serialization.Serializable

@Serializable
data class QuizProgress(
    val quizSetId: String,
    val currentQuestionIndex: Int,
    val score: Int,
    val answeredQuestions: List<Int>, // Indices of answered questions
    val elapsedTimeMillis: Long,
    val totalQuestions: Int,
    val isCompleted: Boolean = false,
    val completedTimestamp: Long = 0L
)
