package com.hkm.pozix.util

import com.hkm.pozix.data.model.Question
import com.hkm.pozix.data.model.Quiz
import com.hkm.pozix.data.model.QuizValidationResult
import kotlinx.serialization.json.Json

object QuizJsonParser {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    fun parseAndValidate(jsonString: String): QuizValidationResult {
        try {
            // Parse JSON
            val quiz = json.decodeFromString<Quiz>(jsonString)
            
            // Validate title
            if (quiz.title.isBlank()) {
                return QuizValidationResult.Error("Quiz title is required and cannot be empty")
            }
            
            // Validate questions array
            if (quiz.questions.isEmpty()) {
                return QuizValidationResult.Error("Quiz must contain at least one question")
            }
            
            // Parse and validate each question
            val parsedQuestions = mutableListOf<Question>()
            var singleChoiceCount = 0
            var trueFalseCount = 0
            
            quiz.questions.forEachIndexed { index, rawQuestion ->
                val questionNumber = index + 1
                
                // Validate question text
                if (rawQuestion.question.isBlank()) {
                    return QuizValidationResult.Error("Question $questionNumber: question text is required")
                }
                
                when (rawQuestion.type) {
                    "single_choice" -> {
                        // Validate options
                        val options = rawQuestion.options
                        if (options == null || options.isEmpty()) {
                            return QuizValidationResult.Error("Question $questionNumber: single_choice must have options")
                        }
                        
                        if (options.size < 2 || options.size > 6) {
                            return QuizValidationResult.Error("Question $questionNumber: single_choice must have 2-6 options, found ${options.size}")
                        }
                        
                        // Check for empty options
                        options.forEachIndexed { optIndex, opt ->
                            if (opt.isBlank()) {
                                return QuizValidationResult.Error("Question $questionNumber: option ${optIndex + 1} is empty")
                            }
                        }
                        
                        // Validate correctIndex
                        val correctIndex = rawQuestion.correctIndex
                        if (correctIndex == null) {
                            return QuizValidationResult.Error("Question $questionNumber: correctIndex is required for single_choice")
                        }
                        
                        if (correctIndex < 0 || correctIndex >= options.size) {
                            return QuizValidationResult.Error("Question $questionNumber: correctIndex $correctIndex is out of range (0-${options.size - 1})")
                        }
                        
                        parsedQuestions.add(
                            Question.SingleChoice(
                                question = rawQuestion.question,
                                options = options,
                                correctIndex = correctIndex,
                                explanation = rawQuestion.explanation
                            )
                        )
                        singleChoiceCount++
                    }
                    
                    "true_false" -> {
                        // Validate correctAnswer
                        val correctAnswer = rawQuestion.correctAnswer
                        if (correctAnswer == null) {
                            return QuizValidationResult.Error("Question $questionNumber: correctAnswer is required for true_false")
                        }
                        
                        parsedQuestions.add(
                            Question.TrueFalse(
                                question = rawQuestion.question,
                                correctAnswer = correctAnswer,
                                explanation = rawQuestion.explanation
                            )
                        )
                        trueFalseCount++
                    }
                    
                    else -> {
                        return QuizValidationResult.Error("Question $questionNumber: invalid question type '${rawQuestion.type}'. Only 'single_choice' and 'true_false' are supported")
                    }
                }
            }
            
            return QuizValidationResult.Success(
                quiz = quiz,
                parsedQuestions = parsedQuestions,
                singleChoiceCount = singleChoiceCount,
                trueFalseCount = trueFalseCount
            )
            
        } catch (e: Exception) {
            return QuizValidationResult.Error("Invalid JSON format: ${e.message}")
        }
    }
}
