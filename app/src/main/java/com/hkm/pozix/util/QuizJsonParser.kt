package com.hkm.pozix.util

import com.hkm.pozix.data.model.Question
import com.hkm.pozix.data.model.Quiz
import com.hkm.pozix.data.model.QuizValidationResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

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
            var shortAnswerCount = 0
            
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
                                explanation = rawQuestion.explanation,
                                media = QuestionMediaSanitizer.sanitize(rawQuestion.media.orEmpty())
                            )
                        )
                        singleChoiceCount++
                    }
                    
                    "true_false" -> {
                        // Validate correctAnswer
                        val boolAnswer = try {
                            rawQuestion.correctAnswer?.jsonPrimitive?.booleanOrNull
                                ?: rawQuestion.correctAnswer?.jsonPrimitive?.content?.toBooleanStrictOrNull()
                        } catch (_: Exception) {
                            null
                        }
                        if (boolAnswer == null) {
                            return QuizValidationResult.Error("Question $questionNumber: correctAnswer (true or false) is required for true_false")
                        }
                        
                        parsedQuestions.add(
                            Question.TrueFalse(
                                question = rawQuestion.question,
                                correctAnswer = boolAnswer,
                                explanation = rawQuestion.explanation,
                                media = QuestionMediaSanitizer.sanitize(rawQuestion.media.orEmpty())
                            )
                        )
                        trueFalseCount++
                    }
                    
                    "short_answer", "shortAnswer", "fill_in", "text" -> {
                        val textAnswer = try {
                            rawQuestion.correctAnswer?.jsonPrimitive?.content?.trim()
                        } catch (_: Exception) {
                            null
                        }
                        if (textAnswer.isNullOrBlank()) {
                            return QuizValidationResult.Error("Question $questionNumber: correctAnswer is required for short_answer")
                        }

                        val accepted = rawQuestion.acceptedAnswers
                            ?.map { it.trim() }
                            ?.filter { it.isNotBlank() }
                            ?: emptyList()

                        parsedQuestions.add(
                            Question.ShortAnswer(
                                question = rawQuestion.question,
                                correctAnswer = textAnswer,
                                acceptedAnswers = accepted,
                                explanation = rawQuestion.explanation,
                                media = QuestionMediaSanitizer.sanitize(rawQuestion.media.orEmpty())
                            )
                        )
                        shortAnswerCount++
                    }
                    
                    else -> {
                        return QuizValidationResult.Error("Question $questionNumber: invalid question type '${rawQuestion.type}'. Supported types: 'single_choice', 'true_false', 'short_answer'")
                    }
                }
            }
            
            return QuizValidationResult.Success(
                quiz = quiz,
                parsedQuestions = parsedQuestions,
                singleChoiceCount = singleChoiceCount,
                trueFalseCount = trueFalseCount,
                shortAnswerCount = shortAnswerCount
            )
            
        } catch (e: Exception) {
            return QuizValidationResult.Error("Invalid JSON format: ${e.message}")
        }
    }
}
