package com.hkm.pozix.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hkm.pozix.data.model.AiProvider
import com.hkm.pozix.data.model.ChatMessage
import com.hkm.pozix.data.model.QuizValidationResult
import com.hkm.pozix.data.model.SavedQuizSet
import com.hkm.pozix.data.repository.AiProviderRepository
import com.hkm.pozix.data.repository.QuizRepository
import com.hkm.pozix.data.repository.SavedQuizRepository
import com.hkm.pozix.network.OpenAiCompatClient
import com.hkm.pozix.util.QuizJsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AIChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val providers: List<AiProvider> = emptyList(),
    val activeProvider: AiProvider? = null,
    val importStatus: ImportStatus = ImportStatus.Idle
)

sealed class ImportStatus {
    object Idle : ImportStatus()
    object Success : ImportStatus()
    data class Error(val message: String) : ImportStatus()
}

class AIChatViewModel(application: Application) : AndroidViewModel(application) {

    private val providerRepository = AiProviderRepository(application)
    private val savedQuizRepository = SavedQuizRepository(application)
    private val quizRepository = QuizRepository(application)

    private val _uiState = MutableStateFlow(AIChatUiState())
    val uiState: StateFlow<AIChatUiState> = _uiState.asStateFlow()

    private val systemInstruction = """
        You are Pozix AI Quiz Assistant, a helpful assistant specialized in creating custom quiz sets.
        Your goal is to help users generate high-quality quizzes on any topic they request.
        When users ask you to generate a quiz, you MUST explain the quiz briefly and then output the quiz JSON inside a standard ```json ... ``` code block.
        
        The JSON must strictly conform to the following schema:
        {
          "title": "Quiz Title",
          "description": "A brief description of the quiz",
          "questions": [
            {
              "type": "single_choice",
              "question": "Question text?",
              "options": ["Option A", "Option B", "Option C", "Option D"],
              "correctIndex": 0,
              "explanation": "Optional explanation of why this option is correct"
            },
            {
              "type": "true_false",
              "question": "True/False statement...",
              "correctAnswer": true,
              "explanation": "Optional explanation of why the statement is true/false"
            }
          ]
        }
        
        Rules:
        1. Always output valid JSON inside the markdown code block.
        2. Never use emojis in the quiz content (title, description, question, options, explanation). You may use emojis in conversational chit-chat, but NEVER inside the JSON quiz object.
        3. Keep the tone friendly, encouraging, and helpful.
        4. Support generating between 5 to 20 questions based on user preference.
    """.trimIndent()

    init {
        loadProviders()
    }

    private fun loadProviders() {
        viewModelScope.launch {
            providerRepository.migrateLegacyGeminiKeyIfNeeded()
        }
        viewModelScope.launch {
            providerRepository.getProviders().collect { providers ->
                val active = providerRepository.getActiveProvider()
                _uiState.value = _uiState.value.copy(
                    providers = providers,
                    activeProvider = active
                )
            }
        }
    }

    fun refreshProviders() {
        viewModelScope.launch {
            val active = providerRepository.getActiveProvider()
            _uiState.value = _uiState.value.copy(activeProvider = active)
        }
    }

    fun selectProvider(id: String) {
        viewModelScope.launch {
            providerRepository.setActiveProvider(id)
            val active = providerRepository.getActiveProvider()
            _uiState.value = _uiState.value.copy(activeProvider = active)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isLoading) return

        val currentMessages = _uiState.value.messages.toMutableList()
        val userMessage = ChatMessage(role = "user", text = text)
        currentMessages.add(userMessage)

        _uiState.value = _uiState.value.copy(
            messages = currentMessages,
            isLoading = true
        )

        viewModelScope.launch {
            val provider = providerRepository.getActiveProvider()
            if (provider == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    messages = _uiState.value.messages + ChatMessage(
                        role = "model",
                        text = "No AI provider configured. Please add a provider in Settings first."
                    )
                )
                return@launch
            }
            // Keep the header chip in sync in case the provider changed in Settings.
            _uiState.value = _uiState.value.copy(activeProvider = provider)

            val result = OpenAiCompatClient.chatCompletion(
                baseUrl = provider.normalizedBaseUrl(),
                apiKey = provider.apiKey,
                model = provider.modelId,
                history = currentMessages,
                systemInstructionText = systemInstruction
            )

            result.fold(
                onSuccess = { responseText ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = _uiState.value.messages + ChatMessage(role = "model", text = responseText)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = _uiState.value.messages + ChatMessage(
                            role = "model",
                            text = "Error generating content: ${error.message ?: "Unknown error"}"
                        )
                    )
                }
            )
        }
    }

    fun importQuizSet(jsonText: String, onPlay: () -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val validation = QuizJsonParser.parseAndValidate(jsonText)
                if (validation is QuizValidationResult.Success) {
                    val quizSetId = UUID.randomUUID().toString()
                    val quizSet = SavedQuizSet(
                        id = quizSetId,
                        name = validation.quiz.title.trim(),
                        jsonContent = jsonText,
                        questionCount = validation.parsedQuestions.size,
                        singleChoiceCount = validation.singleChoiceCount,
                        trueFalseCount = validation.trueFalseCount,
                        description = validation.quiz.description ?: "",
                        lastUsedTimestamp = System.currentTimeMillis()
                    )
                    // Save to library
                    savedQuizRepository.saveQuizSet(quizSet)

                    // Set as current active quiz
                    quizRepository.saveQuizJson(jsonText, quizSetId)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        importStatus = ImportStatus.Success
                    )
                    onPlay()
                } else if (validation is QuizValidationResult.Error) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        importStatus = ImportStatus.Error(validation.message)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    importStatus = ImportStatus.Error(e.message ?: "Import failed")
                )
            }
        }
    }

    fun saveQuizSetOnly(jsonText: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val validation = QuizJsonParser.parseAndValidate(jsonText)
                if (validation is QuizValidationResult.Success) {
                    val quizSetId = UUID.randomUUID().toString()
                    val quizSet = SavedQuizSet(
                        id = quizSetId,
                        name = validation.quiz.title.trim(),
                        jsonContent = jsonText,
                        questionCount = validation.parsedQuestions.size,
                        singleChoiceCount = validation.singleChoiceCount,
                        trueFalseCount = validation.trueFalseCount,
                        description = validation.quiz.description ?: ""
                    )
                    // Save to library
                    savedQuizRepository.saveQuizSet(quizSet)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        importStatus = ImportStatus.Success
                    )
                } else if (validation is QuizValidationResult.Error) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        importStatus = ImportStatus.Error(validation.message)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    importStatus = ImportStatus.Error(e.message ?: "Import failed")
                )
            }
        }
    }

    fun clearImportStatus() {
        _uiState.value = _uiState.value.copy(importStatus = ImportStatus.Idle)
    }

    fun clearChat() {
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }
}
