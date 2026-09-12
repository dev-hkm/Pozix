package com.hkm.pozix

import com.hkm.pozix.util.DocumentTextReader
import com.hkm.pozix.util.QuizAiFollowUp
import com.hkm.pozix.data.model.Question
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test

class DocumentAndReviewTest {
    private fun archive(vararg parts: Pair<String, String>): File = File.createTempFile("pozix-document", ".bin").apply {
        ZipOutputStream(outputStream()).use { zip -> parts.forEach { (name, text) ->
            zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry()
        } }
    }
    @Test fun docxDetectedWithoutExtensionAndKeepsParagraphsAndTables() {
        val file = archive("word/document.xml" to """<w:document xmlns:w="urn:word"><w:p><w:r><w:t>Liên Hợp Quốc</w:t></w:r></w:p><w:tr><w:tc><w:p><w:r><w:t>1945</w:t></w:r></w:p></w:tc></w:tr></w:document>""")
        try { val text = DocumentTextReader.read(file, "unknown.blob")!!; assertTrue(text.contains("Liên Hợp Quốc")); assertTrue(text.contains("\n1945")) } finally { file.delete() }
    }
    @Test fun xlsxResolvesSharedStringsNotJustNumericIndexes() {
        val file = archive("xl/sharedStrings.xml" to "<sst><si><t>Photosynthesis</t></si></sst>",
            "xl/worksheets/sheet1.xml" to "<worksheet><row><c t=\"s\"><v>0</v></c><c><v>42</v></c></row></worksheet>")
        try { assertTrue(DocumentTextReader.read(file, "lesson.xlsx")!!.contains("Photosynthesis\t42")) } finally { file.delete() }
    }
    @Test fun pptxAndOpenDocumentExtractActualText() {
        for (part in listOf("ppt/slides/slide1.xml", "content.xml")) {
            val file = archive(part to "<document><p>My lesson</p></document>")
            try { assertEquals("My lesson", DocumentTextReader.read(file, "unknown")) } finally { file.delete() }
        }
    }
    @Test fun unknownTextAcceptedButBinaryNeverSentAsText() {
        val file = File.createTempFile("pozix-text", ".custom")
        try {
            file.writeText("Đây là bài học")
            assertEquals("Đây là bài học", DocumentTextReader.read(file, file.name))
            file.writeBytes(byteArrayOf(0, 1, 2, 3))
            assertNull(DocumentTextReader.read(file, file.name))
        } finally { file.delete() }
    }
    @Test fun reviewContainsExactSelectionsAndDistinguishesUnanswered() {
        val text = QuizAiFollowUp.report("Test", listOf(Question.TrueFalse("Sky is blue", true),
            Question.SingleChoice("Pick", listOf("A", "B"), 1)), mapOf(0 to 1), 0, 1234)
        val payload = QuizAiFollowUp.decode(text)!!
        assertEquals("Test", payload.title)
        assertEquals(0, payload.score)
        assertEquals(1, payload.items[0].selectedIndex)
        assertEquals(0, payload.items[0].correctIndex)
        assertNull(payload.items[1].selectedIndex)
    }
}
