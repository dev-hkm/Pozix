package com.hkm.pozix.data.model

sealed class QuizValidationResult {
    data class Success(
        val quiz: Quiz,
        val parsedQuestions: List<Question>,
        val singleChoiceCount: Int,
        val trueFalseCount: Int
    ) : QuizValidationResult()
    
    data class Error(val message: String) : QuizValidationResult()
}
