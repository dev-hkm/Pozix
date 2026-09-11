package com.hkm.pozix.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hkm.pozix.data.model.AiProvider
import com.hkm.pozix.data.model.AttachmentType
import com.hkm.pozix.data.model.ChatAttachment
import com.hkm.pozix.data.model.ChatMessage
import com.hkm.pozix.data.model.ChatSession
import com.hkm.pozix.data.model.QuizValidationResult
import com.hkm.pozix.data.model.SavedQuizSet
import com.hkm.pozix.data.repository.AiChatHistoryRepository
import com.hkm.pozix.data.repository.AiProviderRepository
import com.hkm.pozix.data.repository.QuizRepository
import com.hkm.pozix.data.repository.SavedQuizRepository
import com.hkm.pozix.network.OpenAiCompatClient
import com.hkm.pozix.util.ChatAttachmentHelper
import com.hkm.pozix.util.QuizJsonParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException

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
    val pendingAttachments: List<ChatAttachment> = emptyList(),
    val isLoading: Boolean = false,
    val isAttaching: Boolean = false,
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

    private var currentGenerationJob: Job? = null

    private val systemInstruction = """
        You are Zix Bot, a brilliant, helpful AI Assistant for Pozix specialized in creating custom quiz sets and solving academic problems.
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
        5. Multimodal & Attached Files Inputs:
           - When the user provides images (textbook pages, exam photos, geometry figures, diagrams) or attached files (JSON quiz sets, text files, question banks):
             * Read and analyze the entire attached file content or image thoroughly.
             * If the user wants solutions: provide step-by-step mathematical reasoning and solutions formatted in standard LaTeX.
             * If the user wants a quiz: convert the questions from the file/image into the Pozix Quiz JSON schema.
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
        cancelGeneration()
        val newId = UUID.randomUUID().toString()
        _uiState.value = _uiState.value.copy(
            currentSessionId = newId,
            currentSessionTitle = "Cuộc trò chuyện mới",
            messages = emptyList(),
            pendingAttachments = emptyList(),
            isLoading = false
        )
    }

    fun loadSession(sessionId: String) {
        cancelGeneration()
        val session = _uiState.value.sessions.find { it.id == sessionId }
        if (session != null) {
            _uiState.value = _uiState.value.copy(
                currentSessionId = session.id,
                currentSessionTitle = session.title,
                messages = session.messages,
                pendingAttachments = emptyList(),
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

    fun attachMultipleUris(uris: List<Uri>, isExplicitImage: Boolean = false) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAttaching = true)
            val newAttachments = mutableListOf<ChatAttachment>()
            for (uri in uris) {
                val attachment = ChatAttachmentHelper.processUri(getApplication(), uri, isExplicitImage)
                if (attachment != null) {
                    newAttachments.add(attachment)
                }
            }
            _uiState.value = _uiState.value.copy(
                pendingAttachments = _uiState.value.pendingAttachments + newAttachments,
                isAttaching = false
            )
        }
    }

    fun attachUri(uri: Uri, isExplicitImage: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAttaching = true)
            val attachment = ChatAttachmentHelper.processUri(getApplication(), uri, isExplicitImage)
            _uiState.value = if (attachment != null) {
                _uiState.value.copy(
                    pendingAttachments = _uiState.value.pendingAttachments + attachment,
                    isAttaching = false
                )
            } else {
                _uiState.value.copy(isAttaching = false)
            }
        }
    }

    fun removePendingAttachment(attachmentId: String) {
        val target = _uiState.value.pendingAttachments.find { it.id == attachmentId }
        target?.let { ChatAttachmentHelper.deleteAttachment(it.localPath) }
        _uiState.value = _uiState.value.copy(
            pendingAttachments = _uiState.value.pendingAttachments.filter { it.id != attachmentId }
        )
    }

    fun clearPendingAttachments() {
        _uiState.value.pendingAttachments.forEach {
            ChatAttachmentHelper.deleteAttachment(it.localPath)
        }
        _uiState.value = _uiState.value.copy(pendingAttachments = emptyList())
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

    fun cancelGeneration() {
        currentGenerationJob?.cancel()
        currentGenerationJob = null
        if (_uiState.value.isLoading) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun sendMessage(text: String) {
        val trimmedText = text.trim()
        val currentAttachments = _uiState.value.pendingAttachments.toList()

        if (trimmedText.isBlank() && currentAttachments.isEmpty()) return
        if (_uiState.value.isLoading) return

        // Separate images and document files
        val imagePaths = currentAttachments.filter { it.type == AttachmentType.IMAGE }.map { it.localPath }
        val docAttachments = currentAttachments.filter { it.type == AttachmentType.DOCUMENT }

        // Build prompt sent to model (inject text file contents if available)
        val promptToSend = buildString {
            if (docAttachments.isNotEmpty()) {
                appendLine("User attached the following file(s):")
                docAttachments.forEach { doc ->
                    appendLine("--- File: ${doc.name} (${doc.formattedSize}) ---")
                    if (!doc.textContent.isNullOrBlank()) {
                        appendLine(doc.textContent)
                    } else {
                        appendLine("[Document file stored locally: ${doc.name}]")
                    }
                    appendLine("--- End of file ---")
                    appendLine()
                }
            }
            if (trimmedText.isNotBlank()) {
                append(trimmedText)
            } else if (imagePaths.isNotEmpty()) {
                append("Hãy phân tích đề bài trong ảnh, giải chi tiết và tạo bộ câu hỏi trắc nghiệm tương ứng.")
            } else if (docAttachments.isNotEmpty()) {
                append("Hãy đọc tệp tin đính kèm ở trên, phân tích và tạo bộ câu hỏi trắc nghiệm hoặc giải bài tập.")
            }
        }.trim()

        // Display text shown in the user chat bubble
        val userBubbleText = if (trimmedText.isNotBlank()) {
            trimmedText
        } else if (imagePaths.isNotEmpty() && docAttachments.isEmpty()) {
            "Phân tích ảnh và giải đề bài"
        } else if (docAttachments.isNotEmpty() && imagePaths.isEmpty()) {
            "Phân tích tệp ${docAttachments.joinToString { it.name }}"
        } else {
            "Phân tích tài liệu và hình ảnh đính kèm"
        }

        val sessionId = _uiState.value.currentSessionId ?: UUID.randomUUID().toString()
        val userMessage = ChatMessage(
            role = "user",
            text = userBubbleText,
            imagePaths = imagePaths,
            attachments = currentAttachments
        )

        val updatedMessages = _uiState.value.messages + userMessage
        val isFirstMessage = _uiState.value.messages.isEmpty()
        val derivedTitle = if (isFirstMessage) {
            val preview = userBubbleText.lines().firstOrNull { it.isNotBlank() }?.trim() ?: userBubbleText
            if (preview.length > 32) preview.take(30) + "..." else preview
        } else {
            _uiState.value.currentSessionTitle
        }

        _uiState.value = _uiState.value.copy(
            currentSessionId = sessionId,
            currentSessionTitle = derivedTitle,
            messages = updatedMessages,
            pendingAttachments = emptyList(),
            isLoading = true
        )

        // Save session immediately
        viewModelScope.launch {
            historyRepository.updateSessionMessages(sessionId, updatedMessages, derivedTitle)
        }

        currentGenerationJob = viewModelScope.launch {
            val provider = providerRepository.getActiveProvider()
            if (provider == null) {
                val errorMsg = ChatMessage(
                    role = "model",
                    text = "Chưa cấu hình AI Provider nào. Vui lòng vào Cài đặt để thêm Provider trước."
                )
                val finalMessages = updatedMessages + errorMsg
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    messages = finalMessages
                )
                historyRepository.updateSessionMessages(sessionId, finalMessages, derivedTitle)
                return@launch
            }
            _uiState.value = _uiState.value.copy(activeProvider = provider)

            // We construct history for API: replace last user message with promptToSend
            val apiHistory = updatedMessages.dropLast(1) + userMessage.copy(text = promptToSend)

            val assistantText = StringBuilder()
            var hasStartedReceiving = false
            var lastUiUpdateTime = 0L
            val modelMsgTimestamp = System.currentTimeMillis()

            try {
                OpenAiCompatClient.chatCompletionStream(
                    baseUrl = provider.normalizedBaseUrl(),
                    apiKey = provider.apiKey,
                    model = provider.modelId,
                    history = apiHistory,
                    systemInstructionText = systemInstruction,
                    reasoningEffort = provider.reasoningEffort
                ).collect { chunk ->
                    if (!hasStartedReceiving) {
                        hasStartedReceiving = true
                    }
                    assistantText.append(chunk)

                    val now = System.currentTimeMillis()
                    // Throttle state emission every ~25ms for fluid streaming
                    if (now - lastUiUpdateTime > 25L) {
                        lastUiUpdateTime = now
                        val currentText = assistantText.toString()
                        val currentModelMsg = ChatMessage(role = "model", text = currentText, timestamp = modelMsgTimestamp)
                        _uiState.value = _uiState.value.copy(messages = updatedMessages + currentModelMsg)
                    }
                }

                // Final flush on stream completion
                val finalText = assistantText.toString().trim()
                val finalModelMsg = ChatMessage(role = "model", text = finalText, timestamp = modelMsgTimestamp)
                val finalMessages = updatedMessages + finalModelMsg
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    messages = finalMessages
                )
                historyRepository.updateSessionMessages(sessionId, finalMessages, derivedTitle)

            } catch (e: Exception) {
                if (e is CancellationException) {
                    // User explicitly cancelled or stopped generation
                    withContext(NonCancellable) {
                        val partialText = assistantText.toString().trim()
                        if (partialText.isNotEmpty()) {
                            val partialMsg = ChatMessage(role = "model", text = partialText, timestamp = modelMsgTimestamp)
                            val finalMessages = updatedMessages + partialMsg
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                messages = finalMessages
                            )
                            historyRepository.updateSessionMessages(sessionId, finalMessages, derivedTitle)
                        } else {
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                    }
                    return@launch
                }

                // If error happened BEFORE any tokens were streamed, fallback to non-streaming
                if (!hasStartedReceiving) {
                    val fallbackResult = OpenAiCompatClient.chatCompletion(
                        baseUrl = provider.normalizedBaseUrl(),
                        apiKey = provider.apiKey,
                        model = provider.modelId,
                        history = apiHistory,
                        systemInstructionText = systemInstruction,
                        reasoningEffort = provider.reasoningEffort
                    )

                    fallbackResult.fold(
                        onSuccess = { responseText ->
                            val modelMsg = ChatMessage(role = "model", text = responseText)
                            val finalMessages = updatedMessages + modelMsg
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                messages = finalMessages
                            )
                            historyRepository.updateSessionMessages(sessionId, finalMessages, derivedTitle)
                        },
                        onFailure = { fallbackError ->
                            val errorMsg = ChatMessage(
                                role = "model",
                                text = "Lỗi khi tạo nội dung: ${fallbackError.message ?: "Lỗi không xác định"}"
                            )
                            val finalMessages = updatedMessages + errorMsg
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                messages = finalMessages
                            )
                            historyRepository.updateSessionMessages(sessionId, finalMessages, derivedTitle)
                        }
                    )
                } else {
                    // Interrupted stream: preserve partial text and append notice
                    val partialText = assistantText.toString().trim()
                    val errorSuffix = "\n\n*(Đã dừng hoặc ngắt kết nối: ${e.message ?: "Lỗi mạng"})*"
                    val finalMsg = ChatMessage(role = "model", text = partialText + errorSuffix)
                    val finalMessages = updatedMessages + finalMsg
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = finalMessages
                    )
                    historyRepository.updateSessionMessages(sessionId, finalMessages, derivedTitle)
                }
            }
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
                    savedQuizRepository.saveQuizSet(quizSet)
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
        cancelGeneration()
        val sessionId = _uiState.value.currentSessionId
        _uiState.value = _uiState.value.copy(
            messages = emptyList(),
            pendingAttachments = emptyList()
        )
        if (sessionId != null) {
            viewModelScope.launch {
                historyRepository.updateSessionMessages(sessionId, emptyList())
            }
        }
    }
}
