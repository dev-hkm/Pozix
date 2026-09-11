package com.hkm.pozix.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hkm.pozix.R
import com.hkm.pozix.data.cloud.BackupCrypto
import com.hkm.pozix.data.cloud.CloudBackupApi
import com.hkm.pozix.data.model.AiProvider
import com.hkm.pozix.data.model.BackupData
import com.hkm.pozix.data.model.BackupPreferences
import com.hkm.pozix.data.repository.AiProviderRepository
import com.hkm.pozix.data.repository.ExamSessionRepository
import com.hkm.pozix.data.repository.QuizProgressRepository
import com.hkm.pozix.data.repository.QuizRepository
import com.hkm.pozix.data.repository.SavedQuizRepository
import com.hkm.pozix.data.repository.SettingsRepository
import com.hkm.pozix.util.QuizJsonParser
import com.hkm.pozix.data.model.QuizValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class CloudNoticeType {
    SUCCESS,
    ERROR,
    INFO
}

data class CloudNotice(
    val type: CloudNoticeType,
    val message: String
)

data class SettingsUiState(
    val themeMode: String = "system",
    val language: String = "en",
    val font: String = "default",
    val shuffleQuestions: Boolean = false,
    val shuffleAnswers: Boolean = false,
    val showExplanation: Boolean = true,
    val geminiApiKey: String = "",
    val aiProviders: List<AiProvider> = emptyList(),
    val activeProviderId: String = "",
    val backupStatus: String = "",
    val showRestoreConfirm: Boolean = false,
    val pendingRestoreJson: String = "",
    val restoreQuizSetCount: Int = 0,
    val restoreProgressCount: Int = 0,
    val cloudToken: String = "",
    val cloudNotice: CloudNotice? = null,
    val cloudBusy: Boolean = false,
    val codeHighlight: Boolean = true
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val savedQuizRepository = SavedQuizRepository(application)
    private val progressRepository = QuizProgressRepository(application)
    private val quizRepository = QuizRepository(application)
    private val examSessionRepository = ExamSessionRepository(application)
    private val cloudApi = CloudBackupApi()
    private val aiProviderRepository = AiProviderRepository(application)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        allowStructuredMapKeys = true
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.getThemeMode().collect { value ->
                _uiState.value = _uiState.value.copy(themeMode = value)
            }
        }
        viewModelScope.launch {
            repository.getLanguage().collect { value ->
                _uiState.value = _uiState.value.copy(language = value)
            }
        }
        viewModelScope.launch {
            repository.getFont().collect { value ->
                _uiState.value = _uiState.value.copy(font = value)
            }
        }
        viewModelScope.launch {
            repository.getShuffleQuestions().collect { value ->
                _uiState.value = _uiState.value.copy(shuffleQuestions = value)
            }
        }
        viewModelScope.launch {
            repository.getShuffleAnswers().collect { value ->
                _uiState.value = _uiState.value.copy(shuffleAnswers = value)
            }
        }
        viewModelScope.launch {
            repository.getShowExplanation().collect { value ->
                _uiState.value = _uiState.value.copy(showExplanation = value)
            }
        }
        viewModelScope.launch {
            repository.getGeminiApiKey().collect { value ->
                _uiState.value = _uiState.value.copy(geminiApiKey = value)
            }
        }
        viewModelScope.launch {
            repository.getCloudBackupToken().collect { value ->
                _uiState.value = _uiState.value.copy(cloudToken = value)
            }
        }
        viewModelScope.launch {
            aiProviderRepository.migrateLegacyGeminiKeyIfNeeded()
        }
        viewModelScope.launch {
            aiProviderRepository.getProviders().collect { providers ->
                val active = aiProviderRepository.getActiveProvider()
                _uiState.value = _uiState.value.copy(
                    aiProviders = providers,
                    activeProviderId = active?.id.orEmpty()
                )
            }
        }
        viewModelScope.launch {
            repository.getCodeHighlight().collect { value ->
                _uiState.value = _uiState.value.copy(codeHighlight = value)
            }
        }
    }

    fun setCodeHighlight(enabled: Boolean) {
        viewModelScope.launch {
            repository.setCodeHighlight(enabled)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setLanguage(languageCode: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.setLanguage(languageCode)
            onComplete?.invoke()
        }
    }

    fun setFont(fontName: String) {
        viewModelScope.launch { repository.setFont(fontName) }
    }

    fun setShuffleQuestions(enabled: Boolean) {
        viewModelScope.launch { repository.setShuffleQuestions(enabled) }
    }

    fun setShuffleAnswers(enabled: Boolean) {
        viewModelScope.launch { repository.setShuffleAnswers(enabled) }
    }

    fun setShowExplanation(enabled: Boolean) {
        viewModelScope.launch { repository.setShowExplanation(enabled) }
    }

    fun setGeminiApiKey(apiKey: String) {
        viewModelScope.launch { repository.setGeminiApiKey(apiKey) }
    }

    // ---------- AI providers (BYOK) ----------

    fun saveAiProvider(provider: AiProvider, setActive: Boolean = true) {
        viewModelScope.launch { aiProviderRepository.saveProvider(provider, setActive) }
    }

    fun deleteAiProvider(id: String) {
        viewModelScope.launch { aiProviderRepository.deleteProvider(id) }
    }

    fun setActiveAiProvider(id: String) {
        viewModelScope.launch {
            aiProviderRepository.setActiveProvider(id)
            _uiState.value = _uiState.value.copy(activeProviderId = id)
        }
    }

    suspend fun fetchProviderModels(baseUrl: String, apiKey: String): Result<List<String>> {
        val normalized = AiProvider.normalizeBaseUrl(baseUrl)
        return com.hkm.pozix.network.OpenAiCompatClient.fetchModels(normalized, apiKey.trim())
    }

    suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        val state = _uiState.value
        val backup = BackupData(
            version = 2,
            exportedAt = System.currentTimeMillis(),
            quizSets = savedQuizRepository.getSavedQuizSets().first(),
            progress = progressRepository.getAllProgress().first().values.toList(),
            currentQuizJson = quizRepository.getQuizJson().first(),
            currentQuizSetId = quizRepository.getQuizSetId().first(),
            preferences = BackupPreferences(
                language = state.language,
                font = state.font,
                shuffleQuestions = state.shuffleQuestions,
                shuffleAnswers = state.shuffleAnswers,
                showExplanation = state.showExplanation
            ),
            activeExamSession = examSessionRepository.activeSession().first()
        )
        json.encodeToString(backup)
    }

    fun backupToCloud(password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cloudBusy = true, cloudNotice = null)
            try {
                require(password.length >= 8) {
                    getApplication<Application>().getString(R.string.cloud_password_too_short)
                }
                val existingToken = _uiState.value.cloudToken
                val token = existingToken.ifBlank { BackupCrypto.createToken() }
                val existingSalt = if (existingToken.isNotBlank()) {
                    cloudApi.getSalt(token).getOrThrow()
                } else {
                    null
                }
                val backupJson = exportBackup()
                val encrypted = withContext(Dispatchers.Default) {
                    BackupCrypto.encrypt(backupJson, password, existingSalt)
                }
                cloudApi.upload(token, encrypted, replace = existingToken.isNotBlank()).getOrThrow()
                repository.setCloudBackupToken(token)
                _uiState.value = _uiState.value.copy(
                    cloudToken = token,
                    cloudNotice = CloudNotice(
                        type = CloudNoticeType.SUCCESS,
                        message = getApplication<Application>().getString(R.string.cloud_backup_success)
                    )
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    cloudNotice = CloudNotice(
                        type = CloudNoticeType.ERROR,
                        message = cloudErrorMessage(error, R.string.cloud_backup_failed)
                    )
                )
            } finally {
                _uiState.value = _uiState.value.copy(cloudBusy = false)
            }
        }
    }

    fun restoreFromCloud(tokenInput: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cloudBusy = true, cloudNotice = null)
            try {
                require(password.length >= 8) {
                    getApplication<Application>().getString(R.string.cloud_password_too_short)
                }
                val token = tokenInput.trim()
                require(token.matches(Regex("hkm-[A-Za-z0-9]{36}"))) {
                    "Invalid Pozix backup token"
                }
                val salt = cloudApi.getSalt(token).getOrThrow()
                val verifier = withContext(Dispatchers.Default) {
                    BackupCrypto.verifierFor(password, salt)
                }
                val encrypted = cloudApi.restore(token, verifier).getOrThrow()
                val restoredJson = withContext(Dispatchers.Default) {
                    BackupCrypto.decrypt(encrypted, password)
                }
                val backup = decodeAndValidateBackup(restoredJson)
                repository.setCloudBackupToken(token)
                showPreparedRestore(backup, restoredJson)
                _uiState.value = _uiState.value.copy(cloudToken = token)
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    cloudNotice = CloudNotice(
                        type = CloudNoticeType.ERROR,
                        message = cloudErrorMessage(error, R.string.cloud_restore_failed)
                    )
                )
            } finally {
                _uiState.value = _uiState.value.copy(cloudBusy = false)
            }
        }
    }

    fun forgetCloudToken() {
        viewModelScope.launch {
            repository.clearCloudBackupToken()
            _uiState.value = _uiState.value.copy(
                cloudToken = "",
                cloudNotice = CloudNotice(
                    type = CloudNoticeType.INFO,
                    message = getApplication<Application>().getString(R.string.cloud_token_forgotten)
                )
            )
        }
    }

    fun prepareRestore(jsonString: String) {
        viewModelScope.launch {
            try {
                val backup = decodeAndValidateBackup(jsonString)
                showPreparedRestore(backup, jsonString)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    backupStatus = "error:invalid",
                    cloudNotice = CloudNotice(
                        type = CloudNoticeType.ERROR,
                        message = getApplication<Application>().getString(R.string.backup_invalid)
                    )
                )
            }
        }
    }

    fun confirmRestore(onComplete: () -> Unit) {
        val jsonString = _uiState.value.pendingRestoreJson
        viewModelScope.launch {
            try {
                val backup = decodeAndValidateBackup(jsonString)
                withContext(Dispatchers.IO) {
                    savedQuizRepository.replaceQuizSets(backup.quizSets)
                    progressRepository.replaceAllProgress(backup.progress)
                    if (backup.version >= 2) {
                        if (backup.currentQuizJson != null) {
                            quizRepository.saveQuizJson(
                                backup.currentQuizJson,
                                backup.currentQuizSetId.orEmpty()
                            )
                        } else {
                            quizRepository.clearQuiz()
                        }
                        backup.preferences?.let { preferences ->
                            repository.setLanguage(preferences.language)
                            repository.setFont(preferences.font)
                            repository.setShuffleQuestions(preferences.shuffleQuestions)
                            repository.setShuffleAnswers(preferences.shuffleAnswers)
                            repository.setShowExplanation(preferences.showExplanation)
                        }
                        if (backup.activeExamSession != null) {
                            examSessionRepository.save(backup.activeExamSession)
                        } else {
                            examSessionRepository.clear()
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    showRestoreConfirm = false,
                    pendingRestoreJson = "",
                    backupStatus = "success:restore:${backup.quizSets.size}",
                    cloudNotice = CloudNotice(
                        type = CloudNoticeType.SUCCESS,
                        message = getApplication<Application>().getString(R.string.backup_restore_success)
                    )
                )
                onComplete()
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    showRestoreConfirm = false,
                    pendingRestoreJson = "",
                    backupStatus = "error:restore",
                    cloudNotice = CloudNotice(
                        type = CloudNoticeType.ERROR,
                        message = getApplication<Application>().getString(R.string.backup_restore_failed)
                    )
                )
            }
        }
    }

    fun cancelRestore() {
        _uiState.value = _uiState.value.copy(
            showRestoreConfirm = false,
            pendingRestoreJson = "",
            restoreQuizSetCount = 0,
            restoreProgressCount = 0
        )
    }

    fun clearBackupStatus() {
        _uiState.value = _uiState.value.copy(backupStatus = "")
    }

    fun showBackupNotice(type: CloudNoticeType, message: String) {
        _uiState.value = _uiState.value.copy(
            cloudNotice = CloudNotice(type = type, message = message)
        )
    }

    private suspend fun decodeAndValidateBackup(jsonString: String): BackupData =
        withContext(Dispatchers.Default) {
            val backup = json.decodeFromString<BackupData>(jsonString)
            require(backup.version in 1..2) { "Unsupported backup version" }
            require(backup.quizSets.map { it.id }.distinct().size == backup.quizSets.size) {
                "Duplicate quiz set IDs"
            }
            backup.quizSets.forEach { quizSet ->
                require(quizSet.id.isNotBlank()) { "Quiz set ID is missing" }
                require(QuizJsonParser.parseAndValidate(quizSet.jsonContent) is QuizValidationResult.Success) {
                    "Invalid quiz set"
                }
            }
            backup.currentQuizJson?.let { currentQuiz ->
                require(QuizJsonParser.parseAndValidate(currentQuiz) is QuizValidationResult.Success) {
                    "Invalid current quiz"
                }
            }
            require(backup.progress.all { it.quizSetId.isNotBlank() }) {
                "Invalid progress record"
            }
            backup.activeExamSession?.let { session ->
                require(session.questions.isNotEmpty()) { "Invalid exam session" }
                require(session.currentIndex in session.questions.indices) { "Invalid exam position" }
                require(session.timeLimitMillis > 0L) { "Invalid exam duration" }
            }
            backup
        }

    private fun showPreparedRestore(backup: BackupData, jsonString: String) {
        _uiState.value = _uiState.value.copy(
            showRestoreConfirm = true,
            pendingRestoreJson = jsonString,
            restoreQuizSetCount = backup.quizSets.size,
            restoreProgressCount = backup.progress.size,
            backupStatus = "",
            cloudNotice = null
        )
    }

    private fun cloudErrorMessage(error: Exception, fallbackRes: Int): String {
        val context = getApplication<Application>()
        return when (error.message) {
            "Incorrect password", "Invalid credentials" ->
                context.getString(R.string.cloud_invalid_credentials)
            "Backup not found" ->
                context.getString(R.string.cloud_backup_not_found)
            "Too many requests" ->
                context.getString(R.string.cloud_rate_limited)
            else -> error.message?.takeIf { it.isNotBlank() }
                ?: context.getString(fallbackRes)
        }
    }
}
