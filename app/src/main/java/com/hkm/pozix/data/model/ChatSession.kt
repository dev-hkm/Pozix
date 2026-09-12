package com.hkm.pozix.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList()
)
