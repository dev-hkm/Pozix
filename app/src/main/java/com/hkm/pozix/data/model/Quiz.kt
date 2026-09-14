package com.hkm.pozix.data.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class Quiz(
    val title: String,
    val description: String? = null,
    val language: String? = null,
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("studyNotes", "study_notes", "theory", "lesson", "notes")
    val lecture: String? = null,
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
