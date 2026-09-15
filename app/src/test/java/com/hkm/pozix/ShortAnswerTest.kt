package com.hkm.pozix

import com.hkm.pozix.data.model.Question
import com.hkm.pozix.data.model.QuizReviewItem
import com.hkm.pozix.data.model.QuizValidationResult
import com.hkm.pozix.util.QuizJsonParser
import com.hkm.pozix.util.ShortAnswerMatcher
import org.junit.Assert.*
import org.junit.Test

class ShortAnswerTest {

    @Test
    fun testShortAnswerMatcherNumericEquivalence() {
        // Dot and comma decimals
        assertTrue(ShortAnswerMatcher.isMatch("1.5", "1.5"))
        assertTrue(ShortAnswerMatcher.isMatch("1,5", "1.5"))
        assertTrue(ShortAnswerMatcher.isMatch("1.50", "1.5"))
        assertTrue(ShortAnswerMatcher.isMatch(" 1,5 ", " 1.5 "))

        // Integer vs floating point
        assertTrue(ShortAnswerMatcher.isMatch("10", "10.0"))
        assertTrue(ShortAnswerMatcher.isMatch("10.0", "10"))
        assertTrue(ShortAnswerMatcher.isMatch("0", "0.0"))

        // Negative numbers
        assertTrue(ShortAnswerMatcher.isMatch("-4.2", "-4.2"))
        assertTrue(ShortAnswerMatcher.isMatch("-4,2", "-4.2"))
        assertFalse(ShortAnswerMatcher.isMatch("4.2", "-4.2"))

        // Fraction evaluation
        assertTrue(ShortAnswerMatcher.isMatch("1/2", "0.5"))
        assertTrue(ShortAnswerMatcher.isMatch("3/4", "0.75"))
        assertTrue(ShortAnswerMatcher.isMatch("0.5", "1/2"))
    }

    @Test
    fun testShortAnswerMatcherAcceptedAnswers() {
        val accepted = listOf("1.5", "1,5", "3/2", "một phẩy năm")
        assertTrue(ShortAnswerMatcher.isMatch("3/2", "1.5", accepted))
        assertTrue(ShortAnswerMatcher.isMatch("một phẩy năm", "1.5", accepted))
        assertTrue(ShortAnswerMatcher.isMatch("1,5", "1.5", accepted))
    }

    @Test
    fun testShortAnswerMatcherTextMatching() {
        assertTrue(ShortAnswerMatcher.isMatch("Hà Nội", "hà nội"))
        assertTrue(ShortAnswerMatcher.isMatch("  PARIS  ", "paris"))
        assertFalse(ShortAnswerMatcher.isMatch("Luân Đôn", "Hà Nội"))
        assertFalse(ShortAnswerMatcher.isMatch("", "Hà Nội"))
        assertFalse(ShortAnswerMatcher.isMatch("   ", "Hà Nội"))
    }

    @Test
    fun testQuizJsonParserWithAllThreeTypes() {
        val json = """{
            "title": "Đề thi thử THPT Quốc Gia",
            "description": "Đầy đủ 3 phần chuẩn GDPT 2018",
            "questions": [
                {
                    "type": "single_choice",
                    "question": "Phần I: Cho hàm số y = f(x)...",
                    "options": ["A", "B", "C", "D"],
                    "correctIndex": 2,
                    "explanation": "Chọn C"
                },
                {
                    "type": "true_false",
                    "question": "Phần II: Hàm số đồng biến trên R",
                    "correctAnswer": false,
                    "explanation": "Nghịch biến trên (-1, 1)"
                },
                {
                    "type": "short_answer",
                    "question": "Phần III: Giá trị nhỏ nhất của hàm số bằng bao nhiêu?",
                    "correctAnswer": "2.5",
                    "acceptedAnswers": ["2,5", "5/2"],
                    "explanation": "Min = 5/2 = 2.5"
                }
            ]
        }"""

        val result = QuizJsonParser.parseAndValidate(json)
        assertTrue(result is QuizValidationResult.Success)
        val success = result as QuizValidationResult.Success

        assertEquals("Đề thi thử THPT Quốc Gia", success.quiz.title)
        assertEquals(3, success.parsedQuestions.size)
        assertEquals(1, success.singleChoiceCount)
        assertEquals(1, success.trueFalseCount)
        assertEquals(1, success.shortAnswerCount)

        val q3 = success.parsedQuestions[2]
        assertTrue(q3 is Question.ShortAnswer)
        val shortAnswer = q3 as Question.ShortAnswer
        assertEquals("2.5", shortAnswer.correctAnswer)
        assertEquals(listOf("2,5", "5/2"), shortAnswer.acceptedAnswers)
    }

    @Test
    fun testQuizReviewItemShortAnswerGrading() {
        val correctItem = QuizReviewItem(
            question = "Tìm nghiệm",
            userTextAnswer = "1,5",
            correctTextAnswer = "1.5"
        )
        assertTrue(correctItem.isCorrect)

        val wrongItem = QuizReviewItem(
            question = "Tìm nghiệm",
            userTextAnswer = "2.0",
            correctTextAnswer = "1.5"
        )
        assertFalse(wrongItem.isCorrect)

        val unAnsweredItem = QuizReviewItem(
            question = "Tìm nghiệm",
            userTextAnswer = null,
            correctTextAnswer = "1.5"
        )
        assertFalse(unAnsweredItem.isCorrect)
    }
}
