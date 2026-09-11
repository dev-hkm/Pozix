package com.hkm.pozix.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String, // "user" or "model"
    val text: String,
    val reasoning: String? = null,
    val thinkingDurationMs: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val imagePaths: List<String> = emptyList(),
    val attachments: List<ChatAttachment> = emptyList(),
    val quizJson: String? = null
)
