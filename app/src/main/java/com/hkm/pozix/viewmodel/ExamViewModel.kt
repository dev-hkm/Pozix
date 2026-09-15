package com.hkm.pozix.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hkm.pozix.data.model.Question
import com.hkm.pozix.data.model.ExamSession
import com.hkm.pozix.data.model.QuizProgress
import com.hkm.pozix.data.model.QuizValidationResult
import com.hkm.pozix.data.repository.QuizProgressRepository
import com.hkm.pozix.data.repository.QuizRepository
import com.hkm.pozix.data.repository.SavedQuizRepository
import com.hkm.pozix.data.repository.ExamSessionRepository
import com.hkm.pozix.data.repository.SettingsRepository
import com.hkm.pozix.util.QuizJsonParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ExamConfig(
    val timeLimitMinutes: Int = 10,
    val shuffleQuestions: Boolean = false,
    val shuffleAnswers: Boolean = false
)

internal fun calculateExamRemainingMillis(
    savedRemainingMillis: Long,
    deadlineEpochMillis: Long,
    nowEpochMillis: Long,
    pausedByUser: Boolean
): Long {
    val savedRemaining = savedRemainingMillis.coerceAtLeast(0L)
    if (pausedByUser) return savedRemaining
    return minOf(
        savedRemaining,
        (deadlineEpochMillis - nowEpochMillis).coerceAtLeast(0L)
    )
}

internal fun isValidExamQuestionIndex(index: Int, questionCount: Int): Boolean =
    questionCount > 0 && index in 0 until questionCount

sealed class ExamState {
    object Loading : ExamState()

    data class Setup(
        val quizTitle: String,
        val questionCount: Int,
        val config: ExamConfig = ExamConfig()
    ) : ExamState()

    data class Playing(
        val quizSetId: String,
        val quizTitle: String,
        val questions: List<Question>,
        val currentIndex: Int = 0,
        val answers: Map<Int, Int> = emptyMap(),
        val textAnswers: Map<Int, String> = emptyMap(),
        val flaggedQuestions: Set<Int> = emptySet(),
        val timeLimitMillis: Long,
        val remainingMillis: Long,
        val deadlineEpochMillis: Long,
        val isTimeWarning: Boolean = false,
        val showSubmitConfirm: Boolean = false,
        val showPalette: Boolean = false,
        val showExitConfirm: Boolean = false,
        val showResumeConfirm: Boolean = false,
        val resumePausedByUser: Boolean = false
    ) : ExamState()

    data class Finished(
        val quizTitle: String,
        val questions: List<Question>,
        val answers: Map<Int, Int>,
        val textAnswers: Map<Int, String> = emptyMap(),
        val correctCount: Int,
        val totalQuestions: Int,
        val scoreOutOf10: Double,
        val grade: String,
        val gradeColor: String,
        val timeUsedMillis: Long,
        val timeLimitMillis: Long,
        val unansweredCount: Int,
        val questionTimes: Map<Int, Long> = emptyMap()
    ) : ExamState()

    object Error : ExamState()
}

class ExamViewModel(application: Application) : AndroidViewModel(application) {

    private val quizRepository = QuizRepository(application)
    private val savedQuizRepository = SavedQuizRepository(application)
    private val progressRepository = QuizProgressRepository(application)
    private val sessionRepository = ExamSessionRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _examState = MutableStateFlow<ExamState>(ExamState.Loading)
    val examState: StateFlow<ExamState> = _examState.asStateFlow()

    private var timerJob: Job? = null
    private var questionStartTime: Long = 0L
    private val questionTimes = mutableMapOf<Int, Long>()
    private val sessionWriteMutex = Mutex()
    private var sessionWriteVersion = 0L

    init {
        loadQuizForSetup()
    }

    private fun loadQuizForSetup() {
        viewModelScope.launch {
            val jsonString = quizRepository.getQuizJson().first()
            if (jsonString == null) {
                _examState.value = ExamState.Error
                return@launch
            }
            val result = QuizJsonParser.parseAndValidate(jsonString)
            if (result is QuizValidationResult.Success) {
                val quizSetId = quizRepository.getQuizSetId().first().orEmpty()
                val saved = sessionRepository.activeSession().first()
                if (saved != null && saved.quizSetId == quizSetId && saved.questions.size == result.parsedQuestions.size) {
                    val now = System.currentTimeMillis()
                    val remaining = calculateExamRemainingMillis(
                        savedRemainingMillis = saved.remainingMillis,
                        deadlineEpochMillis = saved.deadlineEpochMillis,
                        nowEpochMillis = now,
                        pausedByUser = saved.pausedByUser
                    ).coerceAtMost(saved.timeLimitMillis)
                    questionTimes.clear()
                    questionTimes.putAll(saved.questionTimes)
                    val restored = ExamState.Playing(
                        quizSetId = saved.quizSetId,
                        quizTitle = saved.quizTitle,
                        questions = saved.questions,
                        currentIndex = saved.currentIndex.coerceIn(saved.questions.indices),
                        answers = saved.answers,
                        textAnswers = saved.textAnswers,
                        flaggedQuestions = saved.flaggedQuestions.filterTo(mutableSetOf()) {
                            isValidExamQuestionIndex(it, saved.questions.size)
                        },
                        timeLimitMillis = saved.timeLimitMillis,
                        remainingMillis = remaining,
                        deadlineEpochMillis = if (saved.pausedByUser) now + remaining else saved.deadlineEpochMillis,
                        isTimeWarning = remaining <= 60_000,
                        showResumeConfirm = remaining > 0L,
                        resumePausedByUser = saved.pausedByUser
                    )
                    questionStartTime = now
                    _examState.value = restored
                    if (remaining > 0L) {
                        return@launch
                    }
                    finishExam()
                    return@launch
                }
                val shuffleQuestions = settingsRepository.getShuffleQuestions().first()
                val shuffleAnswers = settingsRepository.getShuffleAnswers().first()
                _examState.value = ExamState.Setup(
                    quizTitle = result.quiz.title,
                    questionCount = result.parsedQuestions.size,
                    config = ExamConfig(
                        shuffleQuestions = shuffleQuestions,
                        shuffleAnswers = shuffleAnswers
                    )
                )
            } else {
                _examState.value = ExamState.Error
            }
        }
    }

    fun updateConfig(config: ExamConfig) {
        val current = _examState.value
        if (current is ExamState.Setup) {
            _examState.value = current.copy(config = config)
        }
    }

    fun startExam() {
        val setup = _examState.value as? ExamState.Setup ?: return
        viewModelScope.launch {
            val jsonString = quizRepository.getQuizJson().first() ?: return@launch
            val result = QuizJsonParser.parseAndValidate(jsonString)
            if (result !is QuizValidationResult.Success) {
                _examState.value = ExamState.Error
                return@launch
            }

            var questions = result.parsedQuestions
            val config = setup.config

            if (config.shuffleQuestions) {
                questions = questions.shuffled()
            }
            if (config.shuffleAnswers) {
                questions = questions.map { question ->
                    when (question) {
                        is Question.SingleChoice -> {
                            val shuffledIndices = question.options.indices.shuffled()
                            val shuffledOptions = shuffledIndices.map { question.options[it] }
                            val newCorrectIndex = shuffledIndices.indexOf(question.correctIndex)
                            question.copy(options = shuffledOptions, correctIndex = newCorrectIndex)
                        }
                        is Question.TrueFalse -> question
                        is Question.ShortAnswer -> question
                    }
                }
            }

            val quizSetId = quizRepository.getQuizSetId().first()
                ?: "exam_${System.currentTimeMillis()}"
            val timeLimitMillis = config.timeLimitMinutes * 60 * 1000L
            val deadline = System.currentTimeMillis() + timeLimitMillis

            questionStartTime = System.currentTimeMillis()
            questionTimes.clear()

            _examState.value = ExamState.Playing(
                quizSetId = quizSetId,
                quizTitle = result.quiz.title,
                questions = questions,
                timeLimitMillis = timeLimitMillis,
                remainingMillis = timeLimitMillis,
                deadlineEpochMillis = deadline
            )
            persist(_examState.value as ExamState.Playing)
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _examState.value
                if (current !is ExamState.Playing) break

                val newRemaining = (current.deadlineEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                if (newRemaining <= 0) {
                    finishExam()
                    break
                }

                val updated = current.copy(
                    remainingMillis = newRemaining,
                    isTimeWarning = newRemaining <= 60_000
                )
                _examState.value = updated
            }
        }
    }

    fun goToQuestion(index: Int) {
        val current = _examState.value as? ExamState.Playing ?: return
        if (!isValidExamQuestionIndex(index, current.questions.size)) return
        recordTimeForQuestion(current.currentIndex)
        val updated = current.copy(currentIndex = index)
        _examState.value = updated
        questionStartTime = System.currentTimeMillis()
        persist(updated)
    }

    fun nextQuestion() {
        val current = _examState.value as? ExamState.Playing ?: return
        goToQuestion(current.currentIndex + 1)
    }

    fun previousQuestion() {
        val current = _examState.value as? ExamState.Playing ?: return
        goToQuestion(current.currentIndex - 1)
    }

    fun selectAnswer(answerIndex: Int) {
        val current = _examState.value as? ExamState.Playing ?: return
        val currentQuestion = current.questions[current.currentIndex]
        if (currentQuestion is Question.ShortAnswer) return
        val newAnswers = current.answers.toMutableMap()
        newAnswers[current.currentIndex] = answerIndex
        val updated = current.copy(answers = newAnswers)
        _examState.value = updated
        persist(updated)
    }

    fun setTextAnswer(index: Int, text: String) {
        val current = _examState.value as? ExamState.Playing ?: return
        if (!isValidExamQuestionIndex(index, current.questions.size)) return
        val updatedTextAnswers = if (text.isBlank()) {
            current.textAnswers - index
        } else {
            current.textAnswers + (index to text)
        }
        val updated = current.copy(textAnswers = updatedTextAnswers)
        _examState.value = updated
        persist(updated)
    }

    fun clearAnswer() {
        val current = _examState.value as? ExamState.Playing ?: return
        if (current.currentIndex !in current.answers && current.currentIndex !in current.textAnswers) return
        val updated = current.copy(
            answers = current.answers - current.currentIndex,
            textAnswers = current.textAnswers - current.currentIndex
        )
        _examState.value = updated
        persist(updated)
    }

    fun toggleFlag() {
        val current = _examState.value as? ExamState.Playing ?: return
        val updatedFlags = if (current.currentIndex in current.flaggedQuestions) {
            current.flaggedQuestions - current.currentIndex
        } else {
            current.flaggedQuestions + current.currentIndex
        }
        val updated = current.copy(flaggedQuestions = updatedFlags)
        _examState.value = updated
        persist(updated)
    }

    fun togglePalette() {
        val current = _examState.value as? ExamState.Playing ?: return
        _examState.value = current.copy(showPalette = !current.showPalette)
    }

    fun requestExit() {
        val current = _examState.value as? ExamState.Playing ?: return
        _examState.value = current.copy(showExitConfirm = true, showSubmitConfirm = false)
    }

    fun cancelExit() {
        val current = _examState.value as? ExamState.Playing ?: return
        _examState.value = current.copy(showExitConfirm = false)
    }

    fun saveAndExit(onSaved: () -> Unit) {
        val current = _examState.value as? ExamState.Playing ?: return
        timerJob?.cancel()
        recordTimeForQuestion(current.currentIndex)
        val remaining = (current.deadlineEpochMillis - System.currentTimeMillis())
            .coerceIn(0L, current.timeLimitMillis)
        val paused = current.copy(remainingMillis = remaining)
        val writeVersion = ++sessionWriteVersion
        viewModelScope.launch {
            sessionWriteMutex.withLock {
                if (writeVersion == sessionWriteVersion) {
                    sessionRepository.save(paused.toSession(pausedByUser = true))
                }
            }
            onSaved()
        }
    }

    fun continueSavedExam() {
        val current = _examState.value as? ExamState.Playing ?: return
        val now = System.currentTimeMillis()
        val remaining = if (current.resumePausedByUser) {
            current.remainingMillis
        } else {
            (current.deadlineEpochMillis - now).coerceAtLeast(0L)
        }
        if (remaining <= 0L) {
            _examState.value = current.copy(
                remainingMillis = 0L,
                showResumeConfirm = false
            )
            questionStartTime = now
            finishExam()
            return
        }
        questionStartTime = now
        val resumed = current.copy(
            showResumeConfirm = false,
            resumePausedByUser = false,
            remainingMillis = remaining,
            deadlineEpochMillis = now + remaining
        )
        _examState.value = resumed
        persist(resumed)
        startTimer()
    }

    fun restartSavedExam() {
        timerJob?.cancel()
        val writeVersion = ++sessionWriteVersion
        viewModelScope.launch {
            sessionWriteMutex.withLock {
                if (writeVersion == sessionWriteVersion) {
                    sessionRepository.clear()
                }
            }
            loadQuizForSetup()
        }
    }

    fun requestSubmit() {
        val current = _examState.value as? ExamState.Playing ?: return
        _examState.value = current.copy(showSubmitConfirm = true)
    }

    fun cancelSubmit() {
        val current = _examState.value as? ExamState.Playing ?: return
        _examState.value = current.copy(showSubmitConfirm = false)
    }

    fun confirmSubmit() {
        finishExam()
    }

    private fun recordTimeForQuestion(index: Int) {
        val elapsed = System.currentTimeMillis() - questionStartTime
        questionTimes[index] = (questionTimes[index] ?: 0L) + elapsed
    }

    private fun finishExam() {
        timerJob?.cancel()
        val current = _examState.value as? ExamState.Playing ?: return
        recordTimeForQuestion(current.currentIndex)

        val answers = current.answers
        val textAnswers = current.textAnswers
        var correctCount = 0
        current.questions.forEachIndexed { index, question ->
            val isCorrect = when (question) {
                is Question.ShortAnswer -> {
                    val userText = textAnswers[index]
                    if (userText.isNullOrBlank()) {
                        false
                    } else {
                        com.hkm.pozix.util.ShortAnswerMatcher.isMatch(
                            userText,
                            question.correctAnswer,
                            question.acceptedAnswers
                        )
                    }
                }
                is Question.SingleChoice -> {
                    val selected = answers[index]
                    selected != null && selected == question.correctIndex
                }
                is Question.TrueFalse -> {
                    val selected = answers[index]
                    selected != null && (selected == 0) == question.correctAnswer
                }
            }
            if (isCorrect) correctCount++
        }

        val total = current.questions.size
        val rawScore = if (total > 0) correctCount.toDouble() / total * 10.0 else 0.0
        val scoreOutOf10 = Math.round(rawScore * 10.0) / 10.0

        val grade = when {
            scoreOutOf10 >= 9.5 -> "A+"
            scoreOutOf10 >= 9.0 -> "A"
            scoreOutOf10 >= 8.0 -> "B+"
            scoreOutOf10 >= 7.0 -> "B"
            scoreOutOf10 >= 6.0 -> "C+"
            scoreOutOf10 >= 5.0 -> "C"
            scoreOutOf10 >= 4.0 -> "D"
            else -> "F"
        }

        val gradeColor = when (grade) {
            "A+", "A" -> "green"
            "B+", "B" -> "blue"
            "C+", "C" -> "orange"
            "D" -> "red"
            else -> "dark_red"
        }

        val unansweredCount = current.questions.indices.count { it !in answers && textAnswers[it].isNullOrBlank() }

        _examState.value = ExamState.Finished(
            quizTitle = current.quizTitle,
            questions = current.questions,
            answers = answers,
            textAnswers = textAnswers,
            correctCount = correctCount,
            totalQuestions = total,
            scoreOutOf10 = scoreOutOf10,
            grade = grade,
            gradeColor = gradeColor,
            timeUsedMillis = current.timeLimitMillis - current.remainingMillis,
            timeLimitMillis = current.timeLimitMillis,
            unansweredCount = unansweredCount,
            questionTimes = questionTimes.toMap()
        )

        val writeVersion = ++sessionWriteVersion
        viewModelScope.launch {
            sessionWriteMutex.withLock {
                if (writeVersion == sessionWriteVersion) {
                    sessionRepository.clear()
                }
            }
            val allAnsweredIndices = (answers.keys + textAnswers.keys).distinct()
            val progress = QuizProgress(
                quizSetId = current.quizSetId,
                currentQuestionIndex = current.currentIndex,
                score = correctCount,
                answeredQuestions = allAnsweredIndices,
                elapsedTimeMillis = current.timeLimitMillis - current.remainingMillis,
                totalQuestions = total,
                isCompleted = true,
                completedTimestamp = System.currentTimeMillis(),
                selectedAnswers = answers,
                userTextAnswers = textAnswers,
                questionSnapshot = current.questions
            )
            progressRepository.saveProgressForQuiz(current.quizSetId, progress)
            savedQuizRepository.updateLastUsedTimestamp(current.quizSetId)
        }
    }

    fun restart() {
        timerJob?.cancel()
        questionTimes.clear()
        val writeVersion = ++sessionWriteVersion
        viewModelScope.launch {
            sessionWriteMutex.withLock {
                if (writeVersion == sessionWriteVersion) {
                    sessionRepository.clear()
                }
            }
            loadQuizForSetup()
        }
    }

    private fun persist(state: ExamState.Playing) {
        val writeVersion = ++sessionWriteVersion
        viewModelScope.launch {
            sessionWriteMutex.withLock {
                if (writeVersion == sessionWriteVersion) {
                    sessionRepository.save(state.toSession())
                }
            }
        }
    }

    private fun ExamState.Playing.toSession(pausedByUser: Boolean = false) = ExamSession(
        quizSetId = quizSetId, quizTitle = quizTitle, questions = questions,
        currentIndex = currentIndex, answers = answers, textAnswers = textAnswers,
        timeLimitMillis = timeLimitMillis,
        deadlineEpochMillis = deadlineEpochMillis,
        remainingMillis = remainingMillis,
        questionTimes = questionTimes.toMap(),
        flaggedQuestions = flaggedQuestions,
        pausedByUser = pausedByUser
    )

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
