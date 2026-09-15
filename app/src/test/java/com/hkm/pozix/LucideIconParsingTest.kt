package com.hkm.pozix

import com.hkm.pozix.ui.components.richcontent.LatexMathParser
import com.hkm.pozix.ui.components.richcontent.LucideIconMap
import org.junit.Assert.*
import org.junit.Test

class LucideIconParsingTest {

    @Test
    fun verifyUserScreenshotIconsAreValid() {
        // Screenshot 1: x-circle
        assertTrue(LucideIconMap.isValidIcon("x-circle"))
        assertTrue(LucideIconMap.isValidIcon("circle-x"))
        assertTrue(LucideIconMap.isValidIcon("x_circle"))
        assertTrue(LucideIconMap.isValidIcon("lucide:x-circle"))

        // Screenshot 2: triangle-alert and scale
        assertTrue(LucideIconMap.isValidIcon("triangle-alert"))
        assertTrue(LucideIconMap.isValidIcon("alert-triangle"))
        assertTrue(LucideIconMap.isValidIcon("scale"))
        assertTrue(LucideIconMap.isValidIcon("balance"))
        assertTrue(LucideIconMap.isValidIcon("scales"))

        // Screenshot 3: globe-2
        assertTrue(LucideIconMap.isValidIcon("globe-2"))
        assertTrue(LucideIconMap.isValidIcon("globe"))
        assertTrue(LucideIconMap.isValidIcon("earth"))

        // Other crucial educational icons
        assertTrue(LucideIconMap.isValidIcon("school"))
        assertTrue(LucideIconMap.isValidIcon("graduation-cap"))
        assertTrue(LucideIconMap.isValidIcon("landmark"))
        assertTrue(LucideIconMap.isValidIcon("history"))
        assertTrue(LucideIconMap.isValidIcon("trending-up"))
        assertTrue(LucideIconMap.isValidIcon("trending-down"))
        assertTrue(LucideIconMap.isValidIcon("percent"))
        assertTrue(LucideIconMap.isValidIcon("calculator"))
        assertTrue(LucideIconMap.isValidIcon("quote"))
        assertTrue(LucideIconMap.isValidIcon("brain"))
        assertTrue(LucideIconMap.isValidIcon("psychology"))
    }

    @Test
    fun verifyDefaultColorsInferredHarmoniously() {
        assertEquals("rose", LucideIconMap.resolveDefaultColor("x-circle"))
        assertEquals("rose", LucideIconMap.resolveDefaultColor("circle-x"))
        assertEquals("amber", LucideIconMap.resolveDefaultColor("triangle-alert"))
        assertEquals("purple", LucideIconMap.resolveDefaultColor("scale"))
        assertEquals("blue", LucideIconMap.resolveDefaultColor("globe-2"))
        assertEquals("emerald", LucideIconMap.resolveDefaultColor("check-circle"))
        assertEquals("yellow", LucideIconMap.resolveDefaultColor("lightbulb"))
        assertEquals("teal", LucideIconMap.resolveDefaultColor("calculator"))
    }

    @Test
    fun extractIconAndTextCleansRawPrefixes() {
        val (icon1, text1) = LatexMathParser.extractIconAndText("x-circle:Bẫy 1: Cần chú ý")
        assertEquals("x-circle", icon1)
        assertEquals("Bẫy 1: Cần chú ý", text1)

        val (icon2, text2) = LatexMathParser.extractIconAndText("triangle-alert:Cẩn thận câu gài:")
        assertEquals("triangle-alert", icon2)
        assertEquals("Cẩn thận câu gài:", text2)

        val (icon3, text3) = LatexMathParser.extractIconAndText("scale:Khái niệm:")
        assertEquals("scale", icon3)
        assertEquals("Khái niệm:", text3)

        val (icon4, text4) = LatexMathParser.extractIconAndText("globe-2:Khái niệm:")
        assertEquals("globe-2", icon4)
        assertEquals("Khái niệm:", text4)
    }

    @Test
    fun fullBadgeWithColorAndIconParsesCleanlyWithoutRawText() {
        // ==pink:x-circle:Bẫy 1:==
        val parsed = LatexMathParser.parseToAnnotatedString("==pink:x-circle:Bẫy 1:== Đọc kỹ đề bài")
        // The text must NOT leak "x-circle:" into visible text
        assertFalse(parsed.text.contains("x-circle:"))
        assertTrue(parsed.text.contains("Bẫy 1:"))
        assertTrue(parsed.text.contains("Đọc kỹ đề bài"))

        val annotations = parsed.getStringAnnotations("androidx.compose.foundation.text.inlineContent", 0, parsed.length)
        assertTrue(annotations.any { it.item.startsWith("lucide:x-circle:") })
    }

    @Test
    fun bracketBadgeWithColorAndIconParsesCleanly() {
        val parsed = LatexMathParser.parseToAnnotatedString("[rose:x-circle:Bẫy 1:] Đọc kỹ đề")
        assertFalse(parsed.text.contains("x-circle:"))
        assertTrue(parsed.text.contains("Bẫy 1:"))
        val annotations = parsed.getStringAnnotations("androidx.compose.foundation.text.inlineContent", 0, parsed.length)
        assertTrue(annotations.any { it.item.startsWith("lucide:x-circle:") })
    }

    @Test
    fun directIconBadgeWithoutColorInferColorAndRenderIcon() {
        // [x-circle:Bẫy 1] without explicit color name
        val parsed1 = LatexMathParser.parseToAnnotatedString("[x-circle:Bẫy 1] Nhớ kiểm tra điều kiện")
        assertFalse(parsed1.text.contains("x-circle:"))
        assertTrue(parsed1.text.contains("Bẫy 1"))
        val ann1 = parsed1.getStringAnnotations("androidx.compose.foundation.text.inlineContent", 0, parsed1.length)
        assertTrue(ann1.any { it.item.startsWith("lucide:x-circle:") })

        // ==scale:Khái niệm== without explicit color name
        val parsed2 = LatexMathParser.parseToAnnotatedString("==scale:Khái niệm:== Là một phản ứng thuận nghịch")
        assertFalse(parsed2.text.contains("scale:"))
        assertTrue(parsed2.text.contains("Khái niệm:"))
        val ann2 = parsed2.getStringAnnotations("androidx.compose.foundation.text.inlineContent", 0, parsed2.length)
        assertTrue(ann2.any { it.item.startsWith("lucide:scale:") })

        // [globe-2:Địa lý thế giới]
        val parsed3 = LatexMathParser.parseToAnnotatedString("[globe-2:Khái niệm:] Kinh tuyến gốc đi qua Greenwich")
        assertFalse(parsed3.text.contains("globe-2:"))
        assertTrue(parsed3.text.contains("Khái niệm:"))
        val ann3 = parsed3.getStringAnnotations("androidx.compose.foundation.text.inlineContent", 0, parsed3.length)
        assertTrue(ann3.any { it.item.startsWith("lucide:globe-2:") })

        // [triangle-alert:Cẩn thận câu gài]
        val parsed4 = LatexMathParser.parseToAnnotatedString("[triangle-alert:Cẩn thận câu gài:] Hãy chú ý từ 'không đúng'")
        assertFalse(parsed4.text.contains("triangle-alert:"))
        assertTrue(parsed4.text.contains("Cẩn thận câu gài:"))
        val ann4 = parsed4.getStringAnnotations("androidx.compose.foundation.text.inlineContent", 0, parsed4.length)
        assertTrue(ann4.any { it.item.startsWith("lucide:triangle-alert:") })
    }

    @Test
    fun verifyAnyLucideIconSupportedFromComprehensiveDictionary() {
        val testIcons = listOf(
            "cat", "dog", "anchor", "tree-pine", "wifi", "car", "droplets",
            "mountain", "sun", "moon", "cloud-rain", "umbrella", "coffee",
            "pizza", "hammer", "sword", "crown", "skull", "syringe",
            "microscope", "dna", "fish", "bird", "battery-charging"
        )

        testIcons.forEach { icon ->
            assertTrue("Icon $icon should be valid in LucideIconMap", LucideIconMap.isValidIcon(icon))
            assertTrue("Glyph for $icon should be present in LucideGlyphMap", com.hkm.pozix.ui.components.richcontent.LucideGlyphMap.hasGlyph(icon))
            assertNotNull("Glyph char for $icon must not be null", com.hkm.pozix.ui.components.richcontent.LucideGlyphMap.getGlyph(icon))
        }

        // Test parsing bracket and highlight badges with arbitrary Lucide icons
        val parsedCat = LatexMathParser.parseToAnnotatedString("[cat:Động vật nuôi] Thú cưng")
        assertFalse(parsedCat.text.contains("cat:"))
        assertTrue(parsedCat.text.contains("Động vật nuôi"))
        val catAnn = parsedCat.getStringAnnotations("androidx.compose.foundation.text.inlineContent", 0, parsedCat.length)
        assertTrue(catAnn.any { it.item.startsWith("lucide:cat:") })

        val parsedTree = LatexMathParser.parseToAnnotatedString("==tree-pine:Sinh thái học== Rừng nhiệt đới")
        assertFalse(parsedTree.text.contains("tree-pine:"))
        assertTrue(parsedTree.text.contains("Sinh thái học"))
        val treeAnn = parsedTree.getStringAnnotations("androidx.compose.foundation.text.inlineContent", 0, parsedTree.length)
        assertTrue(treeAnn.any { it.item.startsWith("lucide:tree-pine:") })

        val parsedSkull = LatexMathParser.parseToAnnotatedString("==skull:Cực độc== Không được uống")
        assertFalse(parsedSkull.text.contains("skull:"))
        assertTrue(parsedSkull.text.contains("Cực độc"))
        val skullAnn = parsedSkull.getStringAnnotations("androidx.compose.foundation.text.inlineContent", 0, parsedSkull.length)
        assertTrue(skullAnn.any { it.item.startsWith("lucide:skull:") })
    }
}
