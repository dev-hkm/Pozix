package com.hkm.pozix.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hkm.pozix.data.model.ChatMessage
import com.hkm.pozix.data.model.ChatSession
import com.hkm.pozix.util.ChatImageStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.aiChatHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_chat_history")

class AiChatHistoryRepository(private val context: Context) {

    private val SESSIONS_KEY = stringPreferencesKey("chat_sessions")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    fun getSessions(): Flow<List<ChatSession>> {
        return context.aiChatHistoryDataStore.data.map { preferences ->
            getSessionsSync(preferences)
        }.flowOn(Dispatchers.Default)
    }

    fun getSessionById(sessionId: String): Flow<ChatSession?> {
        return getSessions().map { list ->
            list.find { it.id == sessionId }
        }
    }

    suspend fun saveSession(session: ChatSession) {
        context.aiChatHistoryDataStore.edit { preferences ->
            val list = getSessionsSync(preferences).toMutableList()
            val index = list.indexOfFirst { it.id == session.id }
            if (index != -1) {
                list[index] = session.copy(updatedAt = System.currentTimeMillis())
            } else {
                list.add(0, session.copy(updatedAt = System.currentTimeMillis()))
            }
            // Keep most recent first
            list.sortByDescending { it.updatedAt }
            preferences[SESSIONS_KEY] = json.encodeToString(list)
        }
    }

    suspend fun updateSessionMessages(sessionId: String, messages: List<ChatMessage>, title: String? = null) {
        context.aiChatHistoryDataStore.edit { preferences ->
            val list = getSessionsSync(preferences).toMutableList()
            val index = list.indexOfFirst { it.id == sessionId }
            val now = System.currentTimeMillis()
            if (index != -1) {
                val current = list[index]
                val newTitle = title ?: current.title
                list[index] = current.copy(
                    messages = messages,
                    title = newTitle,
                    updatedAt = now
                )
            } else {
                val autoTitle = title ?: deriveTitle(messages)
                val newSession = ChatSession(
                    id = sessionId,
                    title = autoTitle,
                    createdAt = now,
                    updatedAt = now,
                    messages = messages
                )
                list.add(0, newSession)
            }
            list.sortByDescending { it.updatedAt }
            preferences[SESSIONS_KEY] = json.encodeToString(list)
        }
    }

    suspend fun renameSession(sessionId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        context.aiChatHistoryDataStore.edit { preferences ->
            val list = getSessionsSync(preferences).map { session ->
                if (session.id == sessionId) {
                    session.copy(title = newTitle.trim(), updatedAt = System.currentTimeMillis())
                } else session
            }.sortedByDescending { it.updatedAt }
            preferences[SESSIONS_KEY] = json.encodeToString(list)
        }
    }

    suspend fun deleteSession(sessionId: String) {
        context.aiChatHistoryDataStore.edit { preferences ->
            val list = getSessionsSync(preferences).toMutableList()
            val target = list.find { it.id == sessionId }
            target?.messages?.forEach { msg ->
                msg.imagePaths.forEach { path ->
                    ChatImageStorage.deleteImage(path)
                }
            }
            list.removeAll { it.id == sessionId }
            preferences[SESSIONS_KEY] = json.encodeToString(list)
        }
    }

    suspend fun clearAllSessions() {
        context.aiChatHistoryDataStore.edit { preferences ->
            val list = getSessionsSync(preferences)
            list.forEach { session ->
                session.messages.forEach { msg ->
                    msg.imagePaths.forEach { path ->
                        ChatImageStorage.deleteImage(path)
                    }
                }
            }
            preferences.remove(SESSIONS_KEY)
        }
    }

    fun createNewSession(initialTitle: String = "Cuộc trò chuyện mới"): ChatSession {
        return ChatSession(
            id = UUID.randomUUID().toString(),
            title = initialTitle,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            messages = emptyList()
        )
    }

    private fun deriveTitle(messages: List<ChatMessage>): String {
        val firstUserMsg = messages.firstOrNull { it.role == "user" }?.text?.trim()
        return if (!firstUserMsg.isNullOrBlank()) {
            val preview = firstUserMsg.lines().firstOrNull { it.isNotBlank() }?.trim() ?: firstUserMsg
            if (preview.length > 32) preview.take(30) + "..." else preview
        } else {
            "Cuộc trò chuyện mới"
        }
    }

    private fun getSessionsSync(preferences: Preferences): List<ChatSession> {
        val jsonString = preferences[SESSIONS_KEY] ?: return emptyList()
        return try {
            json.decodeFromString<List<ChatSession>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
