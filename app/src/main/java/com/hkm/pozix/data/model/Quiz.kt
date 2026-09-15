package com.hkm.pozix.data.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class QuestionRaw(
    val type: String,
    val question: String,
    val options: List<String>? = null,
    val correctIndex: Int? = null,
    @JsonNames("answer", "solution", "correct_answer")
    val correctAnswer: JsonElement? = null,
    @JsonNames("acceptableAnswers", "accepted_answers")
    val acceptedAnswers: List<String>? = null,
    val explanation: String? = null,
    val media: List<QuestionMedia>? = null
)
