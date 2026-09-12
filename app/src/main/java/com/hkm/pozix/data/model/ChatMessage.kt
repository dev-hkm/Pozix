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
    val quizJson: String? = null,
    /** Serialized QuizReviewPayload. Kept separate from quizJson so review can never trigger quiz creation. */
    val quizReviewJson: String? = null,
    /** True when the message is carrying a quiz artifact and its raw JSON should stay hidden while streaming. */
    val quizGeneration: Boolean = false,
    /** Compact reference to an app-private timestamped YouTube transcript. */
    val youtubeSource: YoutubeSourceReference? = null,
    /** Transport/storage failures are rendered outside rich text so they can never corrupt Markdown/LaTeX. */
    val errorNotice: String? = null
)
