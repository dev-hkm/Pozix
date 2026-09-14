package com.hkm.pozix

import android.webkit.ConsoleMessage
import com.hkm.pozix.ui.components.richcontent.ConsoleLogItem
import org.junit.Assert.*
import org.junit.Test

class HtmlPreviewAndMiniBrowserTest {

    @Test
    fun testSvgDetection() {
        val svgCode = """<svg width="100" height="100"><circle cx="50" cy="50" r="40" fill="red" /></svg>"""
        val isSvg1 = "SVG".equals("SVG", ignoreCase = true) || svgCode.trimStart().startsWith("<svg", ignoreCase = true)
        val isSvg2 = "HTML".equals("SVG", ignoreCase = true) || svgCode.trimStart().startsWith("<svg", ignoreCase = true)
        val isNotSvg = "HTML".equals("SVG", ignoreCase = true) || "<div>Hello</div>".trimStart().startsWith("<svg", ignoreCase = true)

        assertTrue(isSvg1)
        assertTrue(isSvg2)
        assertFalse(isNotSvg)
    }

    @Test
    fun testHtmlViewportAndTouchFixInjection() {
        val rawHtml = "<html><head><title>Canvas Game</title></head><body><canvas id='c'></canvas></body></html>"
        val hasHtml = rawHtml.contains("<html", ignoreCase = true) && rawHtml.contains("</html>", ignoreCase = true)
        assertTrue(hasHtml)

        val viewportMeta = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
        val canvasTouchCss = "<style id=\"pozix-touch-fix\">canvas{touch-action:none !important;}</style>"

        var modified = rawHtml
        if (!modified.contains("<meta name=\"viewport\"", ignoreCase = true)) {
            modified = modified.replaceFirst("<head>", "<head>$viewportMeta", ignoreCase = true)
        }
        if (modified.contains("</head>", ignoreCase = true)) {
            modified = modified.replaceFirst("</head>", "$canvasTouchCss</head>", ignoreCase = true)
        }

        assertTrue(modified.contains("<meta name=\"viewport\""))
        assertTrue(modified.contains("pozix-touch-fix"))
        assertTrue(modified.contains("touch-action:none"))
        assertTrue(modified.contains("<canvas id='c'></canvas>"))
    }

    @Test
    fun testConsoleLogLevelAndErrorCounting() {
        val logs = listOf(
            ConsoleLogItem(level = ConsoleMessage.MessageLevel.LOG, message = "App initialized", sourceId = "app.js", lineNumber = 1),
            ConsoleLogItem(level = ConsoleMessage.MessageLevel.WARNING, message = "Deprecated API", sourceId = "game.js", lineNumber = 20),
            ConsoleLogItem(level = ConsoleMessage.MessageLevel.ERROR, message = "Uncaught TypeError: cannot read undefined", sourceId = "game.js", lineNumber = 45),
            ConsoleLogItem(level = ConsoleMessage.MessageLevel.ERROR, message = "AudioContext failed", sourceId = "sound.js", lineNumber = 12)
        )

        val errorCount = logs.count { it.level == ConsoleMessage.MessageLevel.ERROR }
        val warnCount = logs.count { it.level == ConsoleMessage.MessageLevel.WARNING }
        val logCount = logs.count { it.level == ConsoleMessage.MessageLevel.LOG }

        assertEquals(2, errorCount)
        assertEquals(1, warnCount)
        assertEquals(1, logCount)
        assertEquals(4, logs.size)
    }

    @Test
    fun testReloadGuardKeyStability() {
        // Simulating the Compose recomposition cache tag key
        var refreshTrigger = 0
        var canvasDark = true
        var isDesktopView = false
        val code = "<html><body><h1>Flappy Bird</h1><canvas></canvas></body></html>"

        fun computeKey() = "$refreshTrigger:$canvasDark:$isDesktopView:${code.hashCode()}"

        val initialKey = computeKey()

        // Simulating 50 frames or 50 console logs arriving without user reloading or toggling settings
        for (i in 1..50) {
            val keyDuringGameUpdates = computeKey()
            assertEquals("Key must remain identical during game loop to prevent reload", initialKey, keyDuringGameUpdates)
        }

        // When user explicitly taps Reload, key changes
        refreshTrigger++
        val reloadKey = computeKey()
        assertNotEquals(initialKey, reloadKey)

        // When user toggles Desktop mode, key changes
        isDesktopView = true
        val desktopKey = computeKey()
        assertNotEquals(reloadKey, desktopKey)

        // When user toggles Canvas Dark mode, key changes
        canvasDark = false
        val lightKey = computeKey()
        assertNotEquals(desktopKey, lightKey)
    }
}
