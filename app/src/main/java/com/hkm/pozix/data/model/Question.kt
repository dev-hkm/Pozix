package com.hkm.pozix.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed class Question {
    abstract val question: String
    abstract val explanation: String?
    abstract val media: List<QuestionMedia>
    
    @Serializable
    data class SingleChoice(
        override val question: String,
        val options: List<String>,
        val correctIndex: Int,
        override val explanation: String? = null,
        override val media: List<QuestionMedia> = emptyList()
    ) : Question()
    
    @Serializable
    data class TrueFalse(
        override val question: String,
        val correctAnswer: Boolean,
        override val explanation: String? = null,
        override val media: List<QuestionMedia> = emptyList()
    ) : Question()
}
