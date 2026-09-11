package com.hkm.pozix.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hkm.pozix.R
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
import com.hkm.pozix.util.AiQuizOutput
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class AiPhase { IDLE, WAITING, REASONING, RESPONDING, VALIDATING, REPAIRING, SAVING }

data class AIChatUiState(
    val phase: AiPhase = AiPhase.IDLE,
    val currentSessionId: String? = null,
    val currentSessionTitle: String = "",
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
    private var generationVersion = 0L
    private var pendingSnapshot: (() -> List<ChatMessage>)? = null

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
        7. Code & Markup formatting in conversation:
           - ALWAYS wrap any programming code snippets, HTML, XML, JSON, SQL, Python, Java, C++, JavaScript, or scripts inside standard markdown code blocks with the language specifier (e.g. ```html ... ```, ```python ... ```, ```json ... ```) or inline backticks (`code`).
           - NEVER output raw unescaped HTML tags (such as <div>, <span>, <table>, <script>) directly in your conversational text unless wrapped in a markdown code block or formatted as standard Markdown tables (`| Column 1 | Column 2 |`).
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
            currentSessionTitle = getApplication<Application>().getString(R.string.ai_chat_new_conversation),
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
        generationVersion++
        currentGenerationJob?.cancel()
        currentGenerationJob = null
        if (_uiState.value.isLoading) {
            val state = _uiState.value.copy(messages = pendingSnapshot?.invoke() ?: _uiState.value.messages)
            _uiState.value = state
            state.currentSessionId?.let { id ->
                viewModelScope.launch {
                    historyRepository.updateSessionMessages(id, state.messages, state.currentSessionTitle)
                }
            }
            _uiState.value = _uiState.value.copy(isLoading = false, phase = AiPhase.IDLE)
        }
        pendingSnapshot = null
    }

    fun sendMessage(text: String, reasoningEffort: String? = null) {
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
            val app = getApplication<Application>()
            if (trimmedText.isNotBlank()) {
                append(trimmedText)
            } else if (imagePaths.isNotEmpty()) {
                append(app.getString(R.string.ai_chat_prompt_photo))
            } else if (docAttachments.isNotEmpty()) {
                append(app.getString(R.string.ai_chat_prompt_doc))
            }
        }.trim()

        // Display text shown in the user chat bubble
        val app = getApplication<Application>()
        val userBubbleText = if (trimmedText.isNotBlank()) {
            trimmedText
        } else if (imagePaths.isNotEmpty() && docAttachments.isEmpty()) {
            app.getString(R.string.ai_chat_title_photo)
        } else if (docAttachments.isNotEmpty() && imagePaths.isEmpty()) {
            app.getString(R.string.ai_chat_title_doc, docAttachments.joinToString { it.name })
        } else {
            app.getString(R.string.ai_chat_title_doc_and_photo)
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
            isLoading = true,
            phase = AiPhase.WAITING
        )

        val generation = ++generationVersion
        currentGenerationJob = viewModelScope.launch {
            val assistantText = StringBuilder()
            val assistantReasoning = StringBuilder()
            val timestamp = System.currentTimeMillis()
            val started = android.os.SystemClock.elapsedRealtime()
            val expectsQuiz = AiQuizOutput.requested(trimmedText)
            var reasoningFinished: Long? = null
            fun isCurrent() = generation == generationVersion && _uiState.value.currentSessionId == sessionId
            fun snapshot(complete: Boolean = false): ChatMessage {
                val (answer, embedded) = com.hkm.pozix.util.StreamPresentation.splitThinking(assistantText.toString(), complete)
                val reasoning = listOf(assistantReasoning.toString(), embedded)
                    .filter { it.isNotBlank() }.joinToString("\n").takeIf { it.isNotBlank() }
                if (reasoning != null && answer.isNotBlank() && reasoningFinished == null) {
                    reasoningFinished = android.os.SystemClock.elapsedRealtime() - started
                }
                // Quiz schema is an artifact, not thousands of chat characters to animate and then remove.
                val displayAnswer = if (!complete && expectsQuiz) {
                    val fence = Regex("```(?:json)?", RegexOption.IGNORE_CASE).find(answer)?.range?.first
                    val objectStart = answer.indexOf('{').takeIf { it >= 0 }
                    answer.take(listOfNotNull(fence, objectStart).minOrNull() ?: answer.length).trimEnd()
                } else answer
                return ChatMessage(role = "model", text = displayAnswer, reasoning = reasoning,
                    thinkingDurationMs = reasoningFinished, timestamp = timestamp)
            }
            var publisher: Job? = null
            var completedMessage: ChatMessage? = null
            pendingSnapshot = {
                if (assistantText.isNotEmpty() || assistantReasoning.isNotEmpty())
                    updatedMessages + snapshot() else updatedMessages
            }
            try {
                historyRepository.updateSessionMessages(sessionId, updatedMessages, derivedTitle)
                val provider = providerRepository.getActiveProvider()
                    ?: error(getApplication<Application>().getString(R.string.ai_chat_no_provider_error))
                if (!isCurrent()) return@launch
                _uiState.value = _uiState.value.copy(activeProvider = provider)
                val apiHistory = updatedMessages.dropLast(1) + userMessage.copy(text = promptToSend)
                var dirty = false
                // Flush independently of incoming tokens, so the last delta never waits for another packet.
                publisher = launch {
                    while (true) {
                        kotlinx.coroutines.delay(33L)
                        if (dirty && isCurrent()) {
                            dirty = false
                            val message = snapshot()
                            _uiState.value = _uiState.value.copy(messages = updatedMessages + message,
                                phase = if (assistantText.isNotEmpty() && (message.text.isNotBlank() || expectsQuiz))
                                    AiPhase.RESPONDING else AiPhase.REASONING)
                        }
                    }
                }
                val effectiveReasoning = when {
                    reasoningEffort != null -> reasoningEffort.takeIf { it != "off" && it != "default" }
                    else -> provider.reasoningEffort?.takeIf { it != "off" && it != "default" }
                }
                OpenAiCompatClient.chatCompletionStream(
                    baseUrl = provider.normalizedBaseUrl(), apiKey = provider.apiKey,
                    model = provider.modelId, history = apiHistory,
                    systemInstructionText = systemInstruction, reasoningEffort = effectiveReasoning
                ).collect { chunk ->
                    if (!isCurrent()) throw CancellationException()
                    assistantText.append(chunk.content)
                    assistantReasoning.append(chunk.reasoning)
                    dirty = true
                }
                publisher.cancel()
                if (!isCurrent()) return@launch
                _uiState.value = _uiState.value.copy(phase = AiPhase.VALIDATING)
                var result = snapshot(complete = true)
                var artifact = withContext(kotlinx.coroutines.Dispatchers.Default) { AiQuizOutput.extract(result.text) }
                if (artifact == null && expectsQuiz) {
                    // One bounded correction, only after a successful stream with a missing/invalid artifact.
                    _uiState.value = _uiState.value.copy(phase = AiPhase.REPAIRING)
                    val repair = StringBuilder()
                    OpenAiCompatClient.chatCompletionStream(
                        baseUrl = provider.normalizedBaseUrl(), apiKey = provider.apiKey,
                        model = provider.modelId,
                        history = apiHistory + result + ChatMessage(role = "user", text =
                            "The previous response did not contain a valid quiz artifact. Fulfil my original quiz request now. " +
                            "Return the COMPLETE quiz JSON following the system schema, with all requested questions and explanations. " +
                            "Do not merely describe or claim to have created it. Output JSON only."),
                        systemInstructionText = systemInstruction, reasoningEffort = effectiveReasoning
                    ).collect { chunk ->
                        if (!isCurrent()) throw CancellationException()
                        repair.append(chunk.content)
                    }
                    _uiState.value = _uiState.value.copy(phase = AiPhase.VALIDATING)
                    artifact = withContext(kotlinx.coroutines.Dispatchers.Default) { AiQuizOutput.extract(repair.toString()) }
                    result = result.copy(text = app.getString(if (artifact != null)
                        R.string.ai_quiz_ready else R.string.ai_quiz_missing))
                    artifact = artifact?.copy(displayText = result.text)
                }
                if (!isCurrent()) return@launch
                val finalMessages = updatedMessages + result.copy(
                    text = if (artifact != null) artifact.displayText.ifBlank { app.getString(R.string.ai_quiz_ready) } else result.text,
                    quizJson = artifact?.json)
                completedMessage = finalMessages.last()
                pendingSnapshot = { finalMessages }
                _uiState.value = _uiState.value.copy(phase = AiPhase.SAVING, messages = finalMessages)
                historyRepository.updateSessionMessages(sessionId, finalMessages, derivedTitle)
                if (!isCurrent()) return@launch
                _uiState.value = _uiState.value.copy(isLoading = false, messages = finalMessages)
            } catch (e: Exception) {
                publisher?.cancel()
                if (e is CancellationException || !isCurrent()) return@launch
                // Preserve partial output; never silently resend a paid request without streaming.
                val partial = completedMessage ?: if (_uiState.value.phase == AiPhase.REPAIRING)
                    snapshot().copy(text = app.getString(R.string.ai_quiz_missing)) else snapshot()
                val app = getApplication<Application>()
                val notice = (if (completedMessage != null) "\n\n" + app.getString(R.string.ai_history_save_failed)
                    else app.getString(R.string.ai_chat_stream_interrupted)) +
                    (e.message ?: app.getString(R.string.ai_chat_cannot_connect)).take(180)
                val finalMessages = updatedMessages + partial.copy(text = partial.text + notice)
                _uiState.value = _uiState.value.copy(isLoading = false, messages = finalMessages)
                try {
                    historyRepository.updateSessionMessages(sessionId, finalMessages, derivedTitle)
                } catch (_: Exception) { /* Keep the visible response when local storage is unavailable. */ }
            } finally {
                publisher?.cancel()
                if (isCurrent()) {
                    pendingSnapshot = null
                    _uiState.value = _uiState.value.copy(phase = AiPhase.IDLE)
                }
            }
        }
    }

    private fun extractEmbeddedThinking(raw: String): Pair<String, String> {
        return com.hkm.pozix.util.StreamPresentation.splitThinking(raw)
    }

    fun importQuizSet(jsonText: String, onPlay: () -> Unit) {
        if (_uiState.value.isLoading) return
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
        if (_uiState.value.isLoading) return
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
