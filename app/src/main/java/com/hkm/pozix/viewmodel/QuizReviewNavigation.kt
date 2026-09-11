package com.hkm.pozix.viewmodel

import com.hkm.pozix.data.model.Question

/** Navigation changes presentation only. Answer locks and score are preserved. */
object QuizReviewNavigation {
    fun show(state: QuizState.Playing, index: Int, explanations: Boolean): QuizState.Playing {
        if (index !in state.questions.indices) return state
        val question = state.questions[index]
        val selected = state.selectedAnswers[index]
        val answered = index in state.answeredQuestions
        val correct = when (question) {
            is Question.SingleChoice -> selected == question.correctIndex
            is Question.TrueFalse -> selected != null && (selected == 0) == question.correctAnswer
        }
        return state.copy(currentQuestionIndex = index, selectedAnswerIndex = selected,
            isAnswered = answered, isCorrect = answered && correct,
            showExplanation = answered && explanations && !question.explanation.isNullOrBlank())
    }
}
