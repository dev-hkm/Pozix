package com.hkm.pozix

import com.hkm.pozix.ui.components.richcontent.LatexMathParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LatexMathParserTest {

    @Test
    fun testDfracAndFracFormatting() {
        val math1 = LatexMathParser.formatMathString("\\dfrac{1}{x}")
        assertEquals("1/x", math1)

        val math2 = LatexMathParser.formatMathString("f'(x) = \\dfrac{1}{x}")
        assertEquals("f'(x) = 1/x", math2)

        val math3 = LatexMathParser.formatMathString("\\frac{1}{2}")
        assertEquals("½", math3)

        val math4 = LatexMathParser.formatMathString("\\dfrac{x + 1}{x - 1}")
        assertEquals("(x + 1)/(x - 1)", math4)
    }

    @Test
    fun testUserScreenshotCase() {
        val input = "Áp dụng quy tắc tích: (uv)' = u'v + uv'. Với u = x, v = \\ln x: f'(x) = 1 \\cdot \\ln x + x \\cdot \\dfrac{1}{x} = \\ln x + 1."
        val annotated = LatexMathParser.parseToAnnotatedString(input)
        val text = annotated.text
        assertTrue("Text should contain '1/x' and not 'dfrac1x'", text.contains("1/x"))
        assertTrue("Text should not contain 'dfrac1x'", !text.contains("dfrac1x"))
        assertTrue("Text should contain '·'", text.contains("·"))
    }

    @Test
    fun testRootsAndGreekLetters() {
        val sqrt = LatexMathParser.formatMathString("\\sqrt{\\Delta}")
        assertEquals("√Δ", sqrt)

        val quadratic = LatexMathParser.formatMathString("x = \\dfrac{-b \\pm \\sqrt{\\Delta}}{2a}")
        assertEquals("x = (-b ± √Δ)/2a", quadratic)
    }

    @Test
    fun testSuperscriptsAndSubscripts() {
        val exp = LatexMathParser.formatMathString("x^2 + y^2 = z^2")
        assertEquals("x² + y² = z²", exp)

        val sub = LatexMathParser.formatMathString("x_1 + x_2 = -\\dfrac{b}{a}")
        assertEquals("x₁ + x₂ = -b/a", sub)

        val fracExp1 = LatexMathParser.formatMathString("x^{2/3}")
        assertEquals("x²ᐟ³", fracExp1)

        val fracExp2 = LatexMathParser.formatMathString("x^{-1/3}")
        assertEquals("x⁻¹ᐟ³", fracExp2)

        val fracExp3 = LatexMathParser.formatMathString("x^{\\frac{1}{3}}")
        assertEquals("x¹ᐟ³", fracExp3)
    }
}
