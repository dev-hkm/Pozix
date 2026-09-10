package com.hkm.pozix.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hkm.pozix.data.model.Question
import com.hkm.pozix.data.model.Quiz
import com.hkm.pozix.data.model.QuizValidationResult
import com.hkm.pozix.data.repository.QuizRepository
import com.hkm.pozix.util.QuizJsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val hasQuiz: Boolean = false,
    val quiz: Quiz? = null,
    val questions: List<Question> = emptyList(),
    val singleChoiceCount: Int = 0,
    val trueFalseCount: Int = 0
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = QuizRepository(application)
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadQuiz()
    }
    
    private fun loadQuiz() {
        viewModelScope.launch {
            repository.getQuizJson().collect { jsonString ->
                if (jsonString != null) {
                    val result = QuizJsonParser.parseAndValidate(jsonString)
                    if (result is QuizValidationResult.Success) {
                        _uiState.value = HomeUiState(
                            hasQuiz = true,
                            quiz = result.quiz,
                            questions = result.parsedQuestions,
                            singleChoiceCount = result.singleChoiceCount,
                            trueFalseCount = result.trueFalseCount
                        )
                    } else {
                        _uiState.value = HomeUiState()
                    }
                } else {
                    _uiState.value = HomeUiState()
                }
            }
        }
    }
    
    fun refreshQuiz() {
        loadQuiz()
    }
}
