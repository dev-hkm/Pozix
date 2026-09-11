package com.hkm.pozix

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.hkm.pozix.ui.components.richcontent.LatexMathParser
import com.hkm.pozix.ui.components.richcontent.ContentBlock
import com.hkm.pozix.ui.components.richcontent.parseContentBlocks
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
}
