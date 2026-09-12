package com.hkm.pozix.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Quiz(
    val title: String,
    val description: String? = null,
    val language: String? = null,
    val questions: List<QuestionRaw>
)

@Serializable
data class QuestionRaw(
    val type: String,
    val question: String,
    val options: List<String>? = null,
    val correctIndex: Int? = null,
    val correctAnswer: Boolean? = null,
    val explanation: String? = null,
    val media: List<QuestionMedia>? = null
)
