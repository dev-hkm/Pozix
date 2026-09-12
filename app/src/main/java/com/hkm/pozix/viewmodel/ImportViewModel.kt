package com.hkm.pozix.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hkm.pozix.R
import com.hkm.pozix.data.model.QuizValidationResult
import com.hkm.pozix.data.cloud.CloudBackupApi
import com.hkm.pozix.data.model.SavedQuizSet
import com.hkm.pozix.data.repository.QuizRepository
import com.hkm.pozix.data.repository.SavedQuizRepository
import com.hkm.pozix.util.QuizJsonParser
import com.hkm.pozix.util.QuizMediaBundleImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

sealed class ValidationState {
    object Idle : ValidationState()
    object Validating : ValidationState()
    data class Success(val result: QuizValidationResult.Success) : ValidationState()
    data class Error(val message: String) : ValidationState()
}

data class ImportUiState(
    val jsonText: String = "",
    val validationState: ValidationState = ValidationState.Idle,
    val isLoading: Boolean = false,
    val showSaveDialog: Boolean = false,
    val quizSetName: String = "",
    val shareLink: String = "",
    val shareStatus: String = "",
    val shareStatusIsError: Boolean = false,
    val importedFileName: String? = null,
    val fileError: String? = null
)

class ImportViewModel(application: Application) : AndroidViewModel(application) {
    
    private val quizRepository = QuizRepository(application)
    private val savedQuizRepository = SavedQuizRepository(application)
    private val cloudApi = CloudBackupApi()
    
    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()
    
    fun updateJsonText(text: String) {
        _uiState.value = _uiState.value.copy(
            jsonText = text,
            validationState = ValidationState.Idle,
            fileError = null
        )
    }

    fun smartPasteAndValidate(text: String) {
        if (text.isBlank()) return
        _uiState.value = _uiState.value.copy(
            jsonText = text,
            validationState = ValidationState.Idle,
            fileError = null,
            importedFileName = null
        )
        validateJson()
    }

    fun formatJsonText(): Boolean {
        val raw = _uiState.value.jsonText.trim()
        if (raw.isBlank()) return false
        return try {
            val formatted = if (raw.startsWith("{")) {
                org.json.JSONObject(raw).toString(2)
            } else if (raw.startsWith("[")) {
                org.json.JSONArray(raw).toString(2)
            } else {
                raw
            }
            _uiState.value = _uiState.value.copy(jsonText = formatted)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearFileError() {
        _uiState.value = _uiState.value.copy(fileError = null)
    }

    fun importJsonFromFile(uri: Uri, contentResolver: ContentResolver) {
        val context = getApplication<Application>()
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            fileError = null
        )

        viewModelScope.launch {
            try {
                val fileName = getFileName(contentResolver, uri)
                val content = withContext(Dispatchers.IO) {
                    val stream: InputStream = contentResolver.openInputStream(uri)
                        ?: throw java.io.IOException(context.getString(R.string.import_file_open_failed))
                    
                    stream.use { input ->
                        val bytes = QuizMediaBundleImporter.readBounded(input)
                        val text = QuizMediaBundleImporter.decode(context, bytes, fileName.orEmpty())
                        if (text.isBlank()) {
                            throw IllegalArgumentException(context.getString(R.string.import_file_empty))
                        }
                        text
                    }
                }

                _uiState.value = _uiState.value.copy(
                    jsonText = content,
                    validationState = ValidationState.Idle,
                    importedFileName = fileName,
                    fileError = null,
                    isLoading = false
                )
            } catch (e: Exception) {
                val errorMessage = e.message?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.import_file_read_error, e.localizedMessage ?: "Unknown")
                _uiState.value = _uiState.value.copy(
                    fileError = errorMessage,
                    isLoading = false
                )
            }
        }
    }

    fun loadJsonFromContent(content: String, fileName: String? = null) {
        val context = getApplication<Application>()
        if (content.isBlank()) {
            _uiState.value = _uiState.value.copy(
                fileError = context.getString(R.string.import_file_empty),
                isLoading = false
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            jsonText = content,
            validationState = ValidationState.Idle,
            importedFileName = fileName,
            fileError = null,
            isLoading = false
        )
    }

    private fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            } ?: uri.lastPathSegment
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    fun updateShareLink(link: String) {
        _uiState.value = _uiState.value.copy(
            shareLink = link.trim(),
            shareStatus = "",
            shareStatusIsError = false
        )
    }

    fun importFromShare() = viewModelScope.launch {
        if (_uiState.value.isLoading) return@launch
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            shareStatus = "",
            shareStatusIsError = false
        )
        try {
            val imported = cloudApi.loadShare(_uiState.value.shareLink).getOrThrow()
            updateJsonText(imported)
            validateJson()
            _uiState.value = when (_uiState.value.validationState) {
                is ValidationState.Success -> _uiState.value.copy(
                    shareStatus = getApplication<Application>().getString(R.string.import_shared_success),
                    shareStatusIsError = false
                )
                is ValidationState.Error -> _uiState.value.copy(
                    shareStatus = getApplication<Application>().getString(R.string.import_shared_invalid),
                    shareStatusIsError = true
                )
                else -> _uiState.value
            }
        } catch (error: Exception) {
            val context = getApplication<Application>()
            val message = when (error.message) {
                "This is not a Pozix share link" ->
                    context.getString(R.string.import_invalid_share_link)
                "This share link is unavailable or expired", "Share unavailable" ->
                    context.getString(R.string.import_share_unavailable)
                else -> error.message
                    ?: context.getString(R.string.import_shared_failed)
            }
            _uiState.value = _uiState.value.copy(
                shareStatus = message,
                shareStatusIsError = true
            )
        } finally {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    fun validateJson() {
        val jsonText = _uiState.value.jsonText
        if (jsonText.isBlank()) {
            _uiState.value = _uiState.value.copy(
                validationState = ValidationState.Error(
                    getApplication<Application>().getString(R.string.import_empty_json)
                )
            )
            return
        }
        
        _uiState.value = _uiState.value.copy(validationState = ValidationState.Validating)
        
        val result = QuizJsonParser.parseAndValidate(jsonText)
        
        _uiState.value = when (result) {
            is QuizValidationResult.Success -> {
                _uiState.value.copy(
                    validationState = ValidationState.Success(result),
                    quizSetName = result.quiz.title // Pre-fill with quiz title
                )
            }
            is QuizValidationResult.Error -> {
                _uiState.value.copy(validationState = ValidationState.Error(result.message))
            }
        }
    }
    
    fun showSaveDialog() {
        _uiState.value = _uiState.value.copy(showSaveDialog = true)
    }
    
    fun hideSaveDialog() {
        _uiState.value = _uiState.value.copy(showSaveDialog = false)
    }
    
    fun updateQuizSetName(name: String) {
        _uiState.value = _uiState.value.copy(quizSetName = name)
    }
    
    fun saveAndLoadQuiz(onSuccess: () -> Unit) {
        val validationState = _uiState.value.validationState
        if (validationState !is ValidationState.Success) {
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                // Save to saved quiz sets
                val quizSetId = UUID.randomUUID().toString()
                val quizSet = SavedQuizSet(
                    id = quizSetId,
                    name = _uiState.value.quizSetName.trim().ifEmpty { validationState.result.quiz.title },
                    jsonContent = _uiState.value.jsonText,
                    questionCount = validationState.result.parsedQuestions.size,
                    singleChoiceCount = validationState.result.singleChoiceCount,
                    trueFalseCount = validationState.result.trueFalseCount,
                    description = validationState.result.quiz.description ?: "",
                    lastUsedTimestamp = System.currentTimeMillis()
                )
                savedQuizRepository.saveQuizSet(quizSet)
                
                // Also set as current quiz WITH quiz set ID
                quizRepository.saveQuizJson(_uiState.value.jsonText, quizSetId)
                
                hideSaveDialog()
                onSuccess()
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun saveOnlyQuiz(onSuccess: () -> Unit) {
        val validationState = _uiState.value.validationState
        if (validationState !is ValidationState.Success) {
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                // Save to saved quiz sets only
                val quizSet = SavedQuizSet(
                    id = UUID.randomUUID().toString(),
                    name = _uiState.value.quizSetName.trim().ifEmpty { validationState.result.quiz.title },
                    jsonContent = _uiState.value.jsonText,
                    questionCount = validationState.result.parsedQuestions.size,
                    singleChoiceCount = validationState.result.singleChoiceCount,
                    trueFalseCount = validationState.result.trueFalseCount,
                    description = validationState.result.quiz.description ?: ""
                )
                savedQuizRepository.saveQuizSet(quizSet)
                
                hideSaveDialog()
                onSuccess()
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun loadQuizWithoutSaving(onSuccess: () -> Unit) {
        val validationState = _uiState.value.validationState
        if (validationState !is ValidationState.Success) {
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                // Generate a temporary quiz set ID for progress tracking
                val tempQuizSetId = "temp_${UUID.randomUUID()}"
                quizRepository.saveQuizJson(_uiState.value.jsonText, tempQuizSetId)
                onSuccess()
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun clearJson() {
        _uiState.value = ImportUiState()
    }
}
