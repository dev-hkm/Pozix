package com.hkm.pozix

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.hkm.pozix.ui.components.richcontent.LatexMathParser
import com.hkm.pozix.ui.components.richcontent.ContentBlock
import com.hkm.pozix.ui.components.richcontent.parseContentBlocks
import com.hkm.pozix.ui.components.richcontent.LucideIconMap
import com.hkm.pozix.util.RichContentContract
import com.hkm.pozix.util.QuizJsonParser
import com.hkm.pozix.data.model.QuizValidationResult
import org.junit.Assert.*
import org.junit.Test

class MarkdownEnhancementsTest {
    @Test fun screenshotCombinedEmphasisAndStrike() {
        val result = LatexMathParser.parseToAnnotatedString("~~Nội dung bị hủy~~ và ***Nội dung cực kỳ quan trọng***")
        assertEquals("Nội dung bị hủy và Nội dung cực kỳ quan trọng", result.text)
        assertTrue(result.spanStyles.any { it.item.textDecoration == TextDecoration.LineThrough })
        assertTrue(result.spanStyles.any { it.item.fontWeight == FontWeight.Bold && it.item.fontStyle == FontStyle.Italic })
    }

    @Test fun inlineCodeRemainsLiteral() {
        val result = LatexMathParser.parseToAnnotatedString("`~~text~~` và `***text***`")
        assertTrue(result.text.contains("~~text~~"))
        assertTrue(result.text.contains("***text***"))
    }

    @Test fun highlightAndEscapedMarkers() {
        assertEquals("important", LatexMathParser.parseToAnnotatedString("==important==").text)
        assertEquals("*literal*", LatexMathParser.parseToAnnotatedString("\\*literal\\*").text)
    }

    @Test fun copiedSchemaIsImportableJson() {
        assertTrue(QuizJsonParser.parseAndValidate(RichContentContract.example) is QuizValidationResult.Success)
        val quote = parseContentBlocks("> ### Quy tắc\n> **Giữ hàm ngắn**\n> - [x] Early return").single() as ContentBlock.Quote
        val inner = parseContentBlocks(quote.text)
        assertTrue(inner.first() is ContentBlock.Heading)
        assertEquals("☑", (inner.last() as ContentBlock.ListItem).bullet)
    }

    @Test fun badgeNewColorsValidation() {
        assertTrue(LatexMathParser.isValidBadgeColor("teal"))
        assertTrue(LatexMathParser.isValidBadgeColor("indigo"))
        assertTrue(LatexMathParser.isValidBadgeColor("emerald"))
        assertTrue(LatexMathParser.isValidBadgeColor("pink"))
        assertTrue(LatexMathParser.isValidBadgeColor("yellow"))

        val tealColors = LatexMathParser.getBadgeColors("teal", isDark = false)
        assertNotEquals(Color.Transparent, tealColors.background)
        assertNotEquals(Color.Transparent, tealColors.text)
    }

    @Test fun lucideIconResolverValidation() {
        assertTrue(LucideIconMap.isValidIcon("star"))
        assertTrue(LucideIconMap.isValidIcon("check"))
        assertTrue(LucideIconMap.isValidIcon("alert-triangle"))
        assertTrue(LucideIconMap.isValidIcon("lightbulb"))
        assertTrue(LucideIconMap.isValidIcon("terminal"))
        assertNotNull(LucideIconMap.getIcon("star"))
        assertNotNull(LucideIconMap.getIcon("check-circle"))
    }

    @Test fun badgeIconExtractionValidation() {
        val (icon1, text1) = LatexMathParser.extractIconAndText("star:Quang hợp")
        assertEquals("star", icon1)
        assertEquals("Quang hợp", text1)

        val (icon2, text2) = LatexMathParser.extractIconAndText("check:Chính xác")
        assertEquals("check", icon2)
        assertEquals("Chính xác", text2)

        val (icon3, text3) = LatexMathParser.extractIconAndText("Không có icon")
        assertNull(icon3)
        assertEquals("Không có icon", text3)
    }

    @Test fun badgeWithLucideIconParsedWithoutEmojis() {
        val parsed = LatexMathParser.parseToAnnotatedString("Khái niệm ==pink:star:Quang hợp== trong sinh học")
        // Must contain label and NOT contain toy emoji
        assertTrue(parsed.text.contains("Quang hợp"))
        assertFalse(parsed.text.contains("⭐"))
        assertTrue(parsed.spanStyles.isNotEmpty())

        // Must attach inlineContent annotation for vector icon
        val inlineAnnotations = parsed.getStringAnnotations("androidx.compose.foundation.text.inlineContent", 0, parsed.length)
        assertTrue(inlineAnnotations.any { it.item.startsWith("lucide:star:") })

        val bracketParsed = LatexMathParser.parseToAnnotatedString("Xem [teal:check:Hoàn thành] ngay")
        assertTrue(bracketParsed.text.contains("Hoàn thành"))
        assertFalse(bracketParsed.text.contains("✅"))
        val bracketAnnotations = bracketParsed.getStringAnnotations("androidx.compose.foundation.text.inlineContent", 0, bracketParsed.length)
        assertTrue(bracketAnnotations.any { it.item.startsWith("lucide:check:") })
    }
}
