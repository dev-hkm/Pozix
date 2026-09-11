package com.hkm.pozix.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hkm.pozix.R
import com.hkm.pozix.data.model.Question
import com.hkm.pozix.data.model.SavedQuizSet
import com.hkm.pozix.data.model.QuizValidationResult
import com.hkm.pozix.data.cloud.CloudBackupApi
import com.hkm.pozix.data.repository.QuizProgressRepository
import com.hkm.pozix.data.repository.QuizRepository
import com.hkm.pozix.data.repository.SavedQuizRepository
import com.hkm.pozix.util.QuizJsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SavedQuizSetsUiState(
    val quizSets: List<SavedQuizSet> = emptyList(),
    val isLoading: Boolean = true,
    val showDeleteDialog: Boolean = false,
    val quizSetToDelete: SavedQuizSet? = null,
    val showRenameDialog: Boolean = false,
    val quizSetToRename: SavedQuizSet? = null,
    val renameText: String = "",
    val expandedCardId: String? = null,
    val showPreviewWarning: Boolean = false,
    val previewQuizSet: SavedQuizSet? = null,
    val showPreview: Boolean = false,
    val previewTitle: String = "",
    val previewDescription: String = "",
    val previewQuestions: List<Question> = emptyList(),
    val sharingSetId: String? = null,
    val shareUrl: String = "",
    val shareError: String = ""
)

class SavedQuizSetsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val savedQuizRepository = SavedQuizRepository(application)
    private val quizRepository = QuizRepository(application)
    private val progressRepository = QuizProgressRepository(application)
    private val cloudApi = CloudBackupApi()
    
    private val _uiState = MutableStateFlow(SavedQuizSetsUiState())
    val uiState: StateFlow<SavedQuizSetsUiState> = _uiState.asStateFlow()
    
    init {
        loadQuizSets()
    }
    
    private fun loadQuizSets() {
        viewModelScope.launch {
            combine(
                savedQuizRepository.getSavedQuizSets(),
                progressRepository.getAllProgress()
            ) { sets, allProgress ->
                val updatedSets = sets.map { quizSet ->
                    val progress = allProgress[quizSet.id]
                    
                    if (progress != null) {
                        val answeredCount = progress.answeredQuestions.size
                        val totalQuestions = progress.totalQuestions
                        val progressPercentage = if (totalQuestions > 0) {
                            ((answeredCount.toFloat() / totalQuestions) * 100f).coerceAtMost(100f)
                        } else {
                            0f
                        }
                        
                        when {
                            progress.isCompleted -> {
                                quizSet.copy(
                                    progressPercentage = 100f,
                                    hasInProgressSession = false,
                                    isCompleted = true
                                )
                            }
                            answeredCount > 0 -> {
                                quizSet.copy(
                                    progressPercentage = progressPercentage,
                                    hasInProgressSession = true,
                                    isCompleted = false
                                )
                            }
                            else -> {
                                quizSet.copy(
                                    progressPercentage = 0f,
                                    hasInProgressSession = false,
                                    isCompleted = false
                                )
                            }
                        }
                    } else {
                        quizSet.copy(
                            progressPercentage = 0f,
                            hasInProgressSession = false,
                            isCompleted = false
                        )
                    }
                }
                updatedSets.sortedByDescending { it.savedTimestamp }
            }.collect { sets ->
                _uiState.value = _uiState.value.copy(
                    quizSets = sets,
                    isLoading = false
                )
            }
        }
    }
    
    fun showDeleteDialog(quizSet: SavedQuizSet) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            quizSetToDelete = quizSet
        )
    }
    
    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            quizSetToDelete = null
        )
    }
    
    fun deleteQuizSet() {
        viewModelScope.launch {
            _uiState.value.quizSetToDelete?.let { quizSet ->
                savedQuizRepository.deleteQuizSet(quizSet.id)
                progressRepository.clearProgressForQuiz(quizSet.id)
                hideDeleteDialog()
            }
        }
    }
    
    fun showRenameDialog(quizSet: SavedQuizSet) {
        _uiState.value = _uiState.value.copy(
            showRenameDialog = true,
            quizSetToRename = quizSet,
            renameText = quizSet.name
        )
    }
    
    fun hideRenameDialog() {
        _uiState.value = _uiState.value.copy(
            showRenameDialog = false,
            quizSetToRename = null,
            renameText = ""
        )
    }
    
    fun updateRenameText(text: String) {
        _uiState.value = _uiState.value.copy(renameText = text)
    }
    
    fun renameQuizSet() {
        viewModelScope.launch {
            _uiState.value.quizSetToRename?.let { quizSet ->
                val newName = _uiState.value.renameText.trim()
                if (newName.isNotEmpty()) {
                    savedQuizRepository.updateQuizSetName(quizSet.id, newName)
                    hideRenameDialog()
                }
            }
        }
    }
    
    fun loadQuizSet(quizSet: SavedQuizSet, onLoaded: () -> Unit) {
        viewModelScope.launch {
            quizRepository.saveQuizJson(quizSet.jsonContent, quizSet.id)
            savedQuizRepository.updateLastUsedTimestamp(quizSet.id)
            onLoaded()
        }
    }
    
    fun toggleExpand(quizSetId: String) {
        _uiState.value = _uiState.value.copy(
            expandedCardId = if (_uiState.value.expandedCardId == quizSetId) null else quizSetId
        )
    }

    fun showPreviewWarning(quizSet: SavedQuizSet) {
        _uiState.value = _uiState.value.copy(
            showPreviewWarning = true,
            previewQuizSet = quizSet
        )
    }

    fun hidePreviewWarning() {
        _uiState.value = _uiState.value.copy(
            showPreviewWarning = false,
            previewQuizSet = null
        )
    }

    fun confirmPreview() {
        val quizSet = _uiState.value.previewQuizSet ?: return
        val result = QuizJsonParser.parseAndValidate(quizSet.jsonContent)
        if (result is QuizValidationResult.Success) {
            _uiState.value = _uiState.value.copy(
                showPreviewWarning = false,
                showPreview = true,
                previewTitle = result.quiz.title,
                previewDescription = result.quiz.description ?: "",
                previewQuestions = result.parsedQuestions
            )
        }
    }

    fun hidePreview() {
        _uiState.value = _uiState.value.copy(
            showPreview = false,
            previewTitle = "",
            previewDescription = "",
            previewQuestions = emptyList(),
            previewQuizSet = null
        )
    }

    fun shareQuizSet(quizSet: SavedQuizSet) = viewModelScope.launch {
        if (_uiState.value.sharingSetId != null) return@launch
        val validation = QuizJsonParser.parseAndValidate(quizSet.jsonContent)
        if (validation !is QuizValidationResult.Success) {
            _uiState.value = _uiState.value.copy(
                shareError = getApplication<Application>().getString(R.string.saved_quiz_sets_invalid)
            )
            return@launch
        }
        _uiState.value = _uiState.value.copy(sharingSetId = quizSet.id, shareUrl = "", shareError = "")
        try {
            val url = withContext(Dispatchers.IO) { cloudApi.createShare(quizSet.jsonContent).getOrThrow() }
            _uiState.value = _uiState.value.copy(sharingSetId = null, shareUrl = url)
        } catch (error: Exception) {
            _uiState.value = _uiState.value.copy(
                sharingSetId = null,
                shareError = error.message
                    ?: getApplication<Application>().getString(R.string.saved_quiz_sets_share_failed)
            )
        }
    }

    fun dismissShare() { _uiState.value = _uiState.value.copy(shareUrl = "", shareError = "") }
}
