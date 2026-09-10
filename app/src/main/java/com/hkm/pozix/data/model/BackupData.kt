package com.hkm.pozix.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val quizSets: List<SavedQuizSet> = emptyList(),
    val progress: List<QuizProgress> = emptyList(),
    val currentQuizJson: String? = null,
    val currentQuizSetId: String? = null,
    val preferences: BackupPreferences? = null,
    val activeExamSession: ExamSession? = null
)

@Serializable
data class BackupPreferences(
    val language: String = "en",
    val font: String = "default",
    val shuffleQuestions: Boolean = false,
    val shuffleAnswers: Boolean = false,
    val showExplanation: Boolean = true
)
