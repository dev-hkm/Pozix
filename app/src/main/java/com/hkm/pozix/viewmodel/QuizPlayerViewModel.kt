package com.hkm.pozix.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hkm.pozix.data.model.Question
import com.hkm.pozix.data.model.QuizProgress
import com.hkm.pozix.data.model.QuizValidationResult
import com.hkm.pozix.data.repository.QuizProgressRepository
import com.hkm.pozix.data.repository.QuizRepository
import com.hkm.pozix.data.repository.SettingsRepository
import com.hkm.pozix.util.QuizJsonParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class QuizState {
    object Loading : QuizState()
    object Error : QuizState()
    data class Playing(
        val quizSetId: String,
        val quizTitle: String,
        val questions: List<Question>,
        val currentQuestionIndex: Int,
        val score: Int,
        val selectedAnswerIndex: Int?,
        val isAnswered: Boolean,
        val isCorrect: Boolean,
        val showExplanation: Boolean,
        val elapsedTimeMillis: Long,
        val answeredQuestions: List<Int>,
        val selectedAnswers: Map<Int, Int> = emptyMap()
    ) : QuizState()
    data class Finished(
        val quizTitle: String,
        val score: Int,
        val totalQuestions: Int,
        val elapsedTimeMillis: Long
    ) : QuizState()
}

class QuizPlayerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val quizRepository = QuizRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val progressRepository = QuizProgressRepository(application)
    
    private val _quizState = MutableStateFlow<QuizState>(QuizState.Loading)
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()
    
    private var shuffleQuestions = false
    private var shuffleAnswers = false
    private var showExplanationSetting = true
    
    private var timerJob: Job? = null
    private var startTimeMillis = 0L
    
    init {
        loadQuiz()
        startTimer()
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val currentState = _quizState.value
                if (currentState is QuizState.Playing) {
                    val elapsed = System.currentTimeMillis() - startTimeMillis
                    _quizState.value = currentState.copy(elapsedTimeMillis = elapsed)
                    // Do not serialize the entire question bank every second on the UI thread.
                    if (elapsed / 1000 % 10 == 0L) saveProgress()
                }
            }
        }
    }
    
    private fun loadQuiz() {
        viewModelScope.launch {
            shuffleQuestions = settingsRepository.getShuffleQuestions().first()
            shuffleAnswers = settingsRepository.getShuffleAnswers().first()
            showExplanationSetting = settingsRepository.getShowExplanation().first()
            
            val jsonString = quizRepository.getQuizJson().first()
            if (jsonString == null) {
                _quizState.value = QuizState.Error
                return@launch
            }
            
            val quizSetId = quizRepository.getQuizSetId().first() ?: "temp_quiz_${System.currentTimeMillis()}"
            
            val result = QuizJsonParser.parseAndValidate(jsonString)
            if (result is QuizValidationResult.Success) {
                var questions = result.parsedQuestions
                
                if (shuffleQuestions) {
                    questions = questions.shuffled()
                }
                
                if (shuffleAnswers) {
                    questions = questions.map { question ->
                        when (question) {
                            is Question.SingleChoice -> {
                                val shuffledIndices = question.options.indices.shuffled()
                                val shuffledOptions = shuffledIndices.map { question.options[it] }
                                val newCorrectIndex = shuffledIndices.indexOf(question.correctIndex)
                                question.copy(
                                    options = shuffledOptions,
                                    correctIndex = newCorrectIndex
                                )
                            }
                            is Question.TrueFalse -> question
                        }
                    }
                }
                
                val existingProgress = progressRepository.getProgressForQuiz(quizSetId).first()
                
                if (existingProgress != null && !existingProgress.isCompleted && existingProgress.totalQuestions == questions.size) {
                    if (existingProgress.questionSnapshot.size == questions.size) questions = existingProgress.questionSnapshot
                    startTimeMillis = System.currentTimeMillis() - existingProgress.elapsedTimeMillis
                    _quizState.value = QuizState.Playing(
                        quizSetId = quizSetId,
                        quizTitle = result.quiz.title,
                        questions = questions,
                        currentQuestionIndex = existingProgress.currentQuestionIndex.coerceIn(questions.indices),
                        score = existingProgress.score,
                        selectedAnswerIndex = existingProgress.selectedAnswers[existingProgress.currentQuestionIndex],
                        isAnswered = existingProgress.currentQuestionIndex in existingProgress.answeredQuestions,
                        isCorrect = false,
                        showExplanation = false,
                        elapsedTimeMillis = existingProgress.elapsedTimeMillis,
                        answeredQuestions = existingProgress.answeredQuestions,
                        selectedAnswers = existingProgress.selectedAnswers
                    )
                    _quizState.value = QuizReviewNavigation.show(_quizState.value as QuizState.Playing,
                        existingProgress.currentQuestionIndex.coerceIn(questions.indices), showExplanationSetting)
                } else {
                    startTimeMillis = System.currentTimeMillis()
                    _quizState.value = QuizState.Playing(
                        quizSetId = quizSetId,
                        quizTitle = result.quiz.title,
                        questions = questions,
                        currentQuestionIndex = 0,
                        score = 0,
                        selectedAnswerIndex = null,
                        isAnswered = false,
                        isCorrect = false,
                        showExplanation = false,
                        elapsedTimeMillis = 0L,
                        answeredQuestions = emptyList()
                    )
                }
            } else {
                _quizState.value = QuizState.Error
            }
        }
    }
    
    private fun saveProgress() {
        viewModelScope.launch {
            val currentState = _quizState.value
            if (currentState is QuizState.Playing) {
                val progress = QuizProgress(
                    quizSetId = currentState.quizSetId,
                    currentQuestionIndex = currentState.currentQuestionIndex,
                    score = currentState.score,
                    answeredQuestions = currentState.answeredQuestions,
                    elapsedTimeMillis = currentState.elapsedTimeMillis,
                    totalQuestions = currentState.questions.size,
                    isCompleted = false,
                    selectedAnswers = currentState.selectedAnswers,
                    questionSnapshot = currentState.questions
                )
                progressRepository.saveProgressForQuiz(currentState.quizSetId, progress)
            }
        }
    }
    
    fun selectAnswer(answerIndex: Int) {
        val currentState = _quizState.value
        if (currentState !is QuizState.Playing || currentState.isAnswered ||
            currentState.currentQuestionIndex in currentState.answeredQuestions) return
        
        val currentQuestion = currentState.questions[currentState.currentQuestionIndex]
        val optionCount = if (currentQuestion is Question.SingleChoice) currentQuestion.options.size else 2
        if (answerIndex !in 0 until optionCount) return
        val isCorrect = when (currentQuestion) {
            is Question.SingleChoice -> answerIndex == currentQuestion.correctIndex
            is Question.TrueFalse -> {
                val selectedBoolean = answerIndex == 0
                selectedBoolean == currentQuestion.correctAnswer
            }
        }
        
        val updatedAnsweredQuestions = currentState.answeredQuestions + currentState.currentQuestionIndex
        
        _quizState.value = currentState.copy(
            selectedAnswerIndex = answerIndex,
            isAnswered = true,
            isCorrect = isCorrect,
            score = if (isCorrect) currentState.score + 1 else currentState.score,
            showExplanation = showExplanationSetting && currentQuestion.explanation != null,
            answeredQuestions = updatedAnsweredQuestions,
            selectedAnswers = currentState.selectedAnswers + (currentState.currentQuestionIndex to answerIndex)
        )
        
        saveProgress()
    }
    
    fun nextQuestion() {
        val currentState = _quizState.value
        if (currentState !is QuizState.Playing || !currentState.isAnswered) return
        
        val nextIndex = currentState.currentQuestionIndex + 1
        
        if (nextIndex >= currentState.questions.size) {
            timerJob?.cancel()
            
            viewModelScope.launch {
                val completedProgress = QuizProgress(
                    quizSetId = currentState.quizSetId,
                    currentQuestionIndex = currentState.currentQuestionIndex,
                    score = currentState.score,
                    answeredQuestions = currentState.answeredQuestions,
                    elapsedTimeMillis = currentState.elapsedTimeMillis,
                    totalQuestions = currentState.questions.size,
                    isCompleted = true,
                    completedTimestamp = System.currentTimeMillis(),
                    selectedAnswers = currentState.selectedAnswers,
                    questionSnapshot = currentState.questions
                )
                progressRepository.saveProgressForQuiz(currentState.quizSetId, completedProgress)
            }
            
            _quizState.value = QuizState.Finished(
                quizTitle = currentState.quizTitle,
                score = currentState.score,
                totalQuestions = currentState.questions.size,
                elapsedTimeMillis = currentState.elapsedTimeMillis
            )
        } else {
            _quizState.value = QuizReviewNavigation.show(currentState, nextIndex, showExplanationSetting)
            saveProgress()
        }
    }
    
    fun restartQuiz() {
        viewModelScope.launch {
            val currentState = _quizState.value
            if (currentState is QuizState.Playing) {
                progressRepository.clearProgressForQuiz(currentState.quizSetId)
            } else if (currentState is QuizState.Finished) {
                val quizSetId = quizRepository.getQuizSetId().first() ?: "temp_quiz_${System.currentTimeMillis()}"
                progressRepository.clearProgressForQuiz(quizSetId)
            }
            loadQuiz()
            startTimer()
        }
    }

    fun previousQuestion() {
        val state = _quizState.value as? QuizState.Playing ?: return
        val previous = state.currentQuestionIndex - 1
        // Legacy saves do not contain selections; never invent a user's previous answer.
        if (previous !in state.selectedAnswers) return
        _quizState.value = QuizReviewNavigation.show(state, previous, showExplanationSetting)
        saveProgress()
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
