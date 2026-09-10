package com.hkm.pozix.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imagePaths: List<String> = emptyList()
)
