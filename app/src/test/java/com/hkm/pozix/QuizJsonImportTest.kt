package com.hkm.pozix

import com.hkm.pozix.data.model.QuizValidationResult
import com.hkm.pozix.util.QuizJsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizJsonImportTest {

    @Test
    fun validQuizJsonParsesSuccessfully() {
        val json = """
            {
              "title": "Kotlin Basics",
              "description": "Test your Kotlin fundamentals",
              "questions": [
                {
                  "type": "single_choice",
                  "question": "What keyword is used for read-only variables in Kotlin?",
                  "options": ["var", "val", "const", "let"],
                  "correctIndex": 1,
                  "explanation": "val defines immutable/read-only variables."
                },
                {
                  "type": "true_false",
                  "question": "Is null-safety built into Kotlin?",
                  "correctAnswer": true,
                  "explanation": "Kotlin type system distinguishes between nullable and non-nullable types."
                }
              ]
            }
        """.trimIndent()

        val result = QuizJsonParser.parseAndValidate(json)
        assertTrue(result is QuizValidationResult.Success)
        val success = result as QuizValidationResult.Success
        assertEquals("Kotlin Basics", success.quiz.title)
        assertEquals(2, success.parsedQuestions.size)
        assertEquals(1, success.singleChoiceCount)
        assertEquals(1, success.trueFalseCount)
    }

    @Test
    fun jsonWithMissingTitleFailsValidation() {
        val json = """
            {
              "title": "  ",
              "questions": [
                {
                  "type": "true_false",
                  "question": "Simple Question?",
                  "correctAnswer": true
                }
              ]
            }
        """.trimIndent()

        val result = QuizJsonParser.parseAndValidate(json)
        assertTrue(result is QuizValidationResult.Error)
        val error = result as QuizValidationResult.Error
        assertTrue(error.message.contains("Quiz title is required"))
    }

    @Test
    fun emptyOptionsValidationDetection() {
        val json = """
            {
              "title": "Invalid Quiz",
              "questions": [
                {
                  "type": "single_choice",
                  "question": "No options question?",
                  "options": [],
                  "correctIndex": 0
                }
              ]
            }
        """.trimIndent()

        val result = QuizJsonParser.parseAndValidate(json)
        assertTrue(result is QuizValidationResult.Error)
    }
}
