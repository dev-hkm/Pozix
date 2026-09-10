package com.hkm.pozix.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hkm.pozix.data.model.AiProvider
import com.hkm.pozix.data.model.ChatMessage
import com.hkm.pozix.data.model.ChatSession
import com.hkm.pozix.data.model.QuizValidationResult
import com.hkm.pozix.data.model.SavedQuizSet
import com.hkm.pozix.data.repository.AiChatHistoryRepository
import com.hkm.pozix.data.repository.AiProviderRepository
import com.hkm.pozix.data.repository.QuizRepository
import com.hkm.pozix.data.repository.SavedQuizRepository
import com.hkm.pozix.network.OpenAiCompatClient
import com.hkm.pozix.util.ChatImageStorage
import com.hkm.pozix.util.QuizJsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AIChatUiState(
    val currentSessionId: String? = null,
    val currentSessionTitle: String = "Cuộc trò chuyện mới",
    val sessions: List<ChatSession> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val pendingImages: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isAttachingImage: Boolean = false,
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
    private val historyRepository = AiChatHistoryRepository(application)

    private val _uiState = MutableStateFlow(AIChatUiState())
    val uiState: StateFlow<AIChatUiState> = _uiState.asStateFlow()

    private val systemInstruction = """
        You are Pozix AI Quiz Assistant, a helpful assistant specialized in creating custom quiz sets and solving academic problems.
        Your goal is to help users generate high-quality quizzes on any topic they request, and solve or explain STEM and humanities questions accurately.
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
        2. Never use emojis inside the JSON quiz object. Keep the quiz professional and clean.
        3. Support generating between 5 to 25 questions based on user preference.
        4. STEM formatting (Math, Physics, Chemistry, Computer Science):
           - For mathematical, physical, or chemical formulas in questions, options, and explanations: ALWAYS format them using standard LaTeX syntax enclosed in `${'$'} ... ${'$'}` (inline) or `${'$'}${'$'} ... ${'$'}${'$'}` (display block).
              * Fractions: ALWAYS wrap numerator and denominator in curly braces: `\\frac{a}{b}` or `\\dfrac{a}{b}` (e.g. `${'$'}f'(x) = \\frac{1}{x}${'$'}`, `${'$'}\\dfrac{x+1}{2}${'$'}`). NEVER write unbraced or raw macros like `dfrac1x`.
              * Inline math: Enclose in single dollar signs `${'$'} ... ${'$'}` (e.g. `${'$'}y = ax^2 + bx + c${'$'}`, `${'$'}\Delta = b^2 - 4ac${'$'}`, `${'$'}x = \\frac{-b \pm \sqrt{\Delta}}{2a}${'$'}`, `${'$'}\ln x${'$'}`, `${'$'}x \cdot \ln x${'$'}`).
              * Display / block math: Enclose in double dollar signs `${'$'}${'$'} ... ${'$'}${'$'}` on separate lines.
              * Chemistry: Use `\\text{...}` or `\\ce{...}` (e.g. `${'$'}\\text{Fe} + 2\\text{HCl} \to \\text{FeCl}_2 + \\text{H}_2\uparrow${'$'}`).
              * Critical for JSON: In JSON strings, ALWAYS escape all backslashes with double backslashes: `\\frac{a}{b}`, `\\sqrt{x}`, `\\cdot`, `\\alpha`, `\\beta`, `\\Delta`.
           - For Computer Science / Programming questions: Include multi-line code snippets inside the `question` or `explanation` string using markdown code blocks with the language tag (e.g. ```python ... ```, ```cpp ... ```, ```java ... ```) and backticks (` `code` `) for inline code.
        5. Multimodal & Image Inputs:
           - When the user uploads an image containing exam questions, textbook pages, math formulas, geometry diagrams, or science homework:
             * Carefully transcribe and read all questions and options from the image.
             * If the user wants solutions: provide step-by-step mathematical reasoning and solutions formatted in standard LaTeX.
             * If the user wants a quiz: convert the questions from the image into the Pozix Quiz JSON schema.
        6. Keep the conversational tone encouraging, smart, and helpful.
    """.trimIndent()

    init {
        loadProviders()
        loadChatHistory()
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

    private fun loadChatHistory() {
        viewModelScope.launch {
            historyRepository.getSessions().collect { sessionList ->
                val currentId = _uiState.value.currentSessionId
                val currentSession = if (currentId != null) {
                    sessionList.find { it.id == currentId }
                } else {
                    sessionList.firstOrNull()
                }

                if (currentSession != null && currentId == null) {
                    // Initialize with the most recent session
                    _uiState.value = _uiState.value.copy(
                        sessions = sessionList,
                        currentSessionId = currentSession.id,
                        currentSessionTitle = currentSession.title,
                        messages = currentSession.messages
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        sessions = sessionList
                    )
                }
            }
        }
    }

    fun startNewChat() {
        val newId = UUID.randomUUID().toString()
        _uiState.value = _uiState.value.copy(
            currentSessionId = newId,
            currentSessionTitle = "Cuộc trò chuyện mới",
            messages = emptyList(),
            pendingImages = emptyList(),
            isLoading = false
        )
    }

    fun loadSession(sessionId: String) {
        val session = _uiState.value.sessions.find { it.id == sessionId }
        if (session != null) {
            _uiState.value = _uiState.value.copy(
                currentSessionId = session.id,
                currentSessionTitle = session.title,
                messages = session.messages,
                pendingImages = emptyList(),
                isLoading = false
            )
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            historyRepository.deleteSession(sessionId)
            if (_uiState.value.currentSessionId == sessionId) {
                startNewChat()
            }
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            historyRepository.renameSession(sessionId, newTitle)
            if (_uiState.value.currentSessionId == sessionId) {
                _uiState.value = _uiState.value.copy(currentSessionTitle = newTitle.trim())
            }
        }
    }

    fun attachImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAttachingImage = true)
            val path = ChatImageStorage.saveAndOptimizeImage(getApplication(), uri)
            _uiState.value = if (path != null) {
                _uiState.value.copy(
                    pendingImages = _uiState.value.pendingImages + path,
                    isAttachingImage = false
                )
            } else {
                _uiState.value.copy(isAttachingImage = false)
            }
        }
    }

    fun removePendingImage(path: String) {
        ChatImageStorage.deleteImage(path)
        _uiState.value = _uiState.value.copy(
            pendingImages = _uiState.value.pendingImages.filter { it != path }
        )
    }

    fun clearPendingImages() {
        _uiState.value.pendingImages.forEach { path ->
            ChatImageStorage.deleteImage(path)
        }
        _uiState.value = _uiState.value.copy(pendingImages = emptyList())
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
        val trimmedText = text.trim()
        val images = _uiState.value.pendingImages.toList()

        if (trimmedText.isBlank() && images.isEmpty()) return
        if (_uiState.value.isLoading) return

        val promptText = if (trimmedText.isNotBlank()) {
            trimmedText
        } else {
            "Hãy phân tích đề bài trong ảnh, giải chi tiết và tạo bộ câu hỏi trắc nghiệm tương ứng."
        }

        val sessionId = _uiState.value.currentSessionId ?: UUID.randomUUID().toString()
        val userMessage = ChatMessage(
            role = "user",
            text = promptText,
            imagePaths = images
        )

        val updatedMessages = _uiState.value.messages + userMessage
        val isFirstMessage = _uiState.value.messages.isEmpty()
        val derivedTitle = if (isFirstMessage) {
            val preview = promptText.lines().firstOrNull { it.isNotBlank() }?.trim() ?: promptText
            if (preview.length > 32) preview.take(30) + "..." else preview
        } else {
            _uiState.value.currentSessionTitle
        }

        _uiState.value = _uiState.value.copy(
            currentSessionId = sessionId,
            currentSessionTitle = derivedTitle,
            messages = updatedMessages,
            pendingImages = emptyList(),
            isLoading = true
        )

        // Save session immediately
        viewModelScope.launch {
            historyRepository.updateSessionMessages(sessionId, updatedMessages, derivedTitle)
        }

        viewModelScope.launch {
            val provider = providerRepository.getActiveProvider()
            if (provider == null) {
                val errorMsg = ChatMessage(
                    role = "model",
                    text = "No AI provider configured. Please add a provider in Settings first."
                )
                val finalMessages = updatedMessages + errorMsg
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    messages = finalMessages
                )
                historyRepository.updateSessionMessages(sessionId, finalMessages, derivedTitle)
                return@launch
            }
            // Keep the header chip in sync in case the provider changed in Settings.
            _uiState.value = _uiState.value.copy(activeProvider = provider)

            val result = OpenAiCompatClient.chatCompletion(
                baseUrl = provider.normalizedBaseUrl(),
                apiKey = provider.apiKey,
                model = provider.modelId,
                history = updatedMessages,
                systemInstructionText = systemInstruction,
                reasoningEffort = provider.reasoningEffort
            )

            result.fold(
                onSuccess = { responseText ->
                    val modelMsg = ChatMessage(role = "model", text = responseText)
                    val finalMessages = updatedMessages + modelMsg
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = finalMessages
                    )
                    historyRepository.updateSessionMessages(sessionId, finalMessages, derivedTitle)
                },
                onFailure = { error ->
                    val errorMsg = ChatMessage(
                        role = "model",
                        text = "Error generating content: ${error.message ?: "Unknown error"}"
                    )
                    val finalMessages = updatedMessages + errorMsg
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = finalMessages
                    )
                    historyRepository.updateSessionMessages(sessionId, finalMessages, derivedTitle)
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
        val sessionId = _uiState.value.currentSessionId
        _uiState.value = _uiState.value.copy(
            messages = emptyList(),
            pendingImages = emptyList()
        )
        if (sessionId != null) {
            viewModelScope.launch {
                historyRepository.updateSessionMessages(sessionId, emptyList())
            }
        }
    }
}
