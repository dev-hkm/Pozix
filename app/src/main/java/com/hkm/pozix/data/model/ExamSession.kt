package com.hkm.pozix.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ExamSession(
    val quizSetId: String,
    val quizTitle: String,
    val questions: List<Question>,
    val currentIndex: Int,
    val answers: Map<Int, Int>,
    val textAnswers: Map<Int, String> = emptyMap(),
    val timeLimitMillis: Long,
    val deadlineEpochMillis: Long,
    val remainingMillis: Long,
    val questionTimes: Map<Int, Long> = emptyMap(),
    val flaggedQuestions: Set<Int> = emptySet(),
    val pausedByUser: Boolean = false
)
