package com.hkm.pozix.util

import java.io.File
import java.util.zip.ZipFile
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object DocumentTextReader {
    private const val MAX_BYTES = 2 * 1024 * 1024
    fun read(file: File, name: String, mime: String = ""): String? {
        val signature = file.inputStream().use { input -> ByteArray(8).also { input.read(it) } }
        if (signature.take(4).toByteArray().toString(Charsets.US_ASCII) == "%PDF") {
            return com.tom_roush.pdfbox.pdmodel.PDDocument.load(file).use { pdf ->
                require(pdf.numberOfPages <= 300) { "PDF exceeds 300 pages" }
                val output = object : java.io.Writer() {
                    val text = StringBuilder()
                    override fun write(chars: CharArray, offset: Int, length: Int) {
                        val remaining = MAX_BYTES - text.length
                        if (remaining > 0) text.append(chars, offset, minOf(length, remaining))
                    }
                    override fun flush() {}
                    override fun close() {}
                }
                com.tom_roush.pdfbox.text.PDFTextStripper().writeText(pdf, output)
                output.text.toString().trim().takeIf { it.isNotBlank() }
            }
        }
        if (signature[0] == 0x50.toByte() && signature[1] == 0x4b.toByte()) return ZipFile(file).use { zip ->
            val names = zip.entries().asSequence().take(2001).map { it.name }.toList()
            require(names.size <= 2000) { "Too many document parts" }
            val parts = when {
                "word/document.xml" in names -> listOf("word/document.xml")
                names.any { it.startsWith("xl/worksheets/") } -> names.filter { it.matches(Regex("xl/worksheets/sheet\\d+\\.xml")) }.sortedBy { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
                names.any { it.startsWith("ppt/slides/") } -> names.filter { it.matches(Regex("ppt/slides/slide\\d+\\.xml")) }.sortedBy { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
                "content.xml" in names -> listOf("content.xml")
                else -> names.filter { it.endsWith(".xhtml", true) || it.endsWith(".html", true) }.sorted()
            }
            var expanded = 0
            fun bytes(part: String): ByteArray {
                val bytes = zip.getInputStream(zip.getEntry(part)).use { it.readBounded() }
                expanded += bytes.size
                require(expanded <= 8 * MAX_BYTES) { "Expanded document exceeds 16 MB" }
                require(!bytes.toString(Charsets.UTF_8).contains("<!DOCTYPE", true)) { "External declarations are not supported" }
                return bytes
            }
            val shared = mutableListOf<String>()
            if ("xl/sharedStrings.xml" in names) {
                val p = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
                p.setInput(bytes("xl/sharedStrings.xml").inputStream(), null)
                var item = StringBuilder()
                while (p.eventType != XmlPullParser.END_DOCUMENT) {
                    if (p.eventType == XmlPullParser.START_TAG && p.name == "si") item = StringBuilder()
                    if (p.eventType == XmlPullParser.TEXT) item.append(p.text)
                    if (p.eventType == XmlPullParser.END_TAG && p.name == "si") shared.add(item.toString())
                    p.nextToken()
                }
            }
            parts.joinToString("\n\n") { part ->
            val bytes = bytes(part)
            val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
            parser.setInput(bytes.inputStream(), null)
            buildString {
                var cellType: String? = null
                var value = false
                var inFormula = false
                while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                    when (parser.eventType) {
                        XmlPullParser.DOCDECL -> error("Document declarations are not supported")
                        XmlPullParser.START_TAG -> when (parser.name) {
                            "c" -> cellType = parser.getAttributeValue(null, "t")
                            "v" -> value = true
                            "f" -> inFormula = true
                            "tab" -> append('\t')
                            "br", "cr" -> append('\n')
                        }
                        XmlPullParser.TEXT -> if (!inFormula) {
                            if (value && cellType == "s") append(shared.getOrNull(parser.text.toIntOrNull() ?: -1) ?: "[Unreadable cell]")
                            else append(parser.text)
                        }
                        XmlPullParser.END_TAG -> when (parser.name) {
                            "v" -> value = false
                            "f" -> inFormula = false
                            "p", "tr", "row", "table-row", "h", "div" -> append('\n')
                            "tc", "c", "table-cell" -> append('\t')
                        }
                    }
                    if (length >= MAX_BYTES) break
                    parser.nextToken()
                }
            }.trim()
            }.takeIf { it.isNotBlank() }
        }
        val bytes = file.inputStream().use { it.readBounded() }
        val text = when {
            bytes.size >= 2 && bytes[0] == 0xff.toByte() && bytes[1] == 0xfe.toByte() -> bytes.toString(Charsets.UTF_16LE)
            bytes.size >= 2 && bytes[0] == 0xfe.toByte() && bytes[1] == 0xff.toByte() -> bytes.toString(Charsets.UTF_16BE)
            else -> bytes.toString(Charsets.UTF_8)
        }.removePrefix("\uFEFF")
        val invalidCount = text.count { it == '\uFFFD' || (it.code < 32 && it !in "\n\r\t") }
        if (invalidCount > 0 && (text.length < 64 || invalidCount.toDouble() / text.length > 0.05)) return null
        return (if (name.endsWith(".html", true) || mime.contains("html")) android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
            else text).trim().takeIf { it.isNotBlank() }
    }
    private fun java.io.InputStream.readBounded(): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            val remaining = MAX_BYTES - output.size()
            if (remaining <= 0) break
            output.write(buffer, 0, minOf(count, remaining))
            if (count > remaining) break
        }
        return output.toByteArray()
    }
}
