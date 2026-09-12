package com.hkm.pozix

import com.hkm.pozix.data.model.Question
import com.hkm.pozix.data.model.QuizValidationResult
import com.hkm.pozix.util.QuizJsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionMediaSchemaTest {

    @Test
    fun legacyQuizRemainsCompatibleWithoutMedia() {
        val result = QuizJsonParser.parseAndValidate(
            """
            {
              "title": "Legacy",
              "questions": [
                {
                  "type": "true_false",
                  "question": "Old JSON still works",
                  "correctAnswer": true
                }
              ]
            }
            """.trimIndent()
        )

        assertTrue(result is QuizValidationResult.Success)
        val question = (result as QuizValidationResult.Success).parsedQuestions.single()
        assertTrue(question is Question.TrueFalse)
        assertTrue(question.media.isEmpty())
    }

    @Test
    fun supportedMediaIsPreservedForPlayerRendering() {
        val result = QuizJsonParser.parseAndValidate(
            """
            {
              "title": "Visual STEM",
              "questions": [
                {
                  "type": "single_choice",
                  "question": "Which object is shown?",
                  "options": ["A", "B"],
                  "correctIndex": 0,
                  "media": [
                    {"type":"image","uri":"https://example.com/figure.png","altText":"A figure","caption":"Reference figure"},
                    {"type":"geometry","preset":"cube","showHiddenEdges":true,"highlightEdges":["AB"]},
                    {"type":"diagram","nodes":[{"id":"a","label":"Input","x":0.2,"y":0.5},{"id":"b","label":"Output","x":0.8,"y":0.5}],"edges":[{"from":"a","to":"b","label":"flow"}]},
                    {"type":"mind_map","nodes":[{"id":"root","label":"Topic","x":0.5,"y":0.5},{"id":"child","label":"Detail","x":0.82,"y":0.28}],"edges":[{"from":"root","to":"child"}]}
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        assertTrue(result is QuizValidationResult.Success)
        val question = (result as QuizValidationResult.Success).parsedQuestions.single()
        assertTrue(question is Question.SingleChoice)
        assertEquals(listOf("image", "geometry", "diagram", "mind_map"), question.media.map { it.type })
        assertEquals("cube", question.media[1].preset)
        assertEquals(2, question.media[2].nodes.size)
        assertEquals("flow", question.media[2].edges.single().label)
    }

    @Test
    fun invalidMediaIsIgnoredWithoutInvalidatingTheQuestion() {
        val result = QuizJsonParser.parseAndValidate(
            """
            {
              "title": "Graceful media",
              "questions": [
                {
                  "type": "true_false",
                  "question": "The text remains playable",
                  "correctAnswer": true,
                  "media": [
                    {"type":"unknown","uri":"https://example.com/nope"},
                    {"type":"image","uri":"file:///private/not-allowed.png"},
                    {"type":"geometry","preset":"not-a-real-solid"},
                    {"type":"diagram","nodes":[{"id":"","label":"","x":4.0,"y":-2.0}],"edges":[{"from":"missing","to":"also-missing"}]}
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        assertTrue(result is QuizValidationResult.Success)
        val question = (result as QuizValidationResult.Success).parsedQuestions.single()
        assertTrue(question.media.isEmpty())
    }
}
