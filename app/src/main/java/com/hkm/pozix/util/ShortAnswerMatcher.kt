package com.hkm.pozix.util

import kotlin.math.abs

/**
 * Intelligent grading matcher for Short Answer questions (Trắc nghiệm trả lời ngắn).
 * Complies with Vietnamese national exam (THPTQG) evaluation guidelines:
 * - Case-insensitive & trimmed matching.
 * - Vietnamese comma decimal separator handling ("1,5" matches "1.5").
 * - Numerical equivalence ("4.0" matches "4", "-0.25" matches "-0,25").
 * - Fraction recognition ("1/2" matches "0.5").
 * - Multi-variant matching via acceptedAnswers list.
 */
object ShortAnswerMatcher {

    fun isMatch(
        userAnswer: String,
        correctAnswer: String,
        acceptedAnswers: List<String> = emptyList()
    ): Boolean {
        val cleanUser = userAnswer.trim()
        if (cleanUser.isEmpty()) return false

        val candidates = (listOf(correctAnswer) + acceptedAnswers)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (candidates.isEmpty()) return false

        // 1. Direct or Case-Insensitive String Match
        if (candidates.any { it.equals(cleanUser, ignoreCase = true) }) {
            return true
        }

        // 2. Comma vs Period Normalization (e.g. "1,5" <-> "1.5", "-3,2" <-> "-3.2")
        val normalizedUser = cleanUser.replace(',', '.')
        val normalizedCandidates = candidates.map { it.replace(',', '.') }

        if (normalizedCandidates.any { it.equals(normalizedUser, ignoreCase = true) }) {
            return true
        }

        // 3. Floating Point / Numerical Equivalence
        val userNum = parseNumberOrFraction(normalizedUser)
        if (userNum != null) {
            for (cand in normalizedCandidates) {
                val candNum = parseNumberOrFraction(cand)
                if (candNum != null && abs(userNum - candNum) < 1e-6) {
                    return true
                }
            }
        }

        return false
    }

    private fun parseNumberOrFraction(raw: String): Double? {
        val s = raw.trim()
        val direct = s.toDoubleOrNull()
        if (direct != null) return direct

        // Check simple fraction "a/b"
        if (s.contains('/')) {
            val parts = s.split('/')
            if (parts.size == 2) {
                val num = parts[0].trim().toDoubleOrNull()
                val den = parts[1].trim().toDoubleOrNull()
                if (num != null && den != null && den != 0.0) {
                    return num / den
                }
            }
        }
        return null
    }
}
