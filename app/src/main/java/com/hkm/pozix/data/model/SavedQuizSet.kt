package com.hkm.pozix.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedQuizSet(
    val id: String,
    val name: String,
    val jsonContent: String,
    val questionCount: Int,
    val singleChoiceCount: Int,
    val trueFalseCount: Int,
    val shortAnswerCount: Int = 0,
    val description: String = "",
    val savedTimestamp: Long = System.currentTimeMillis(),
    val lastUsedTimestamp: Long = 0L,
    val progressPercentage: Float = 0f,
    val isCompleted: Boolean = false,
    val hasInProgressSession: Boolean = false
)
