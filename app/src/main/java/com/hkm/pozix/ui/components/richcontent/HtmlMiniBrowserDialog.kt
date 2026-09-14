package com.hkm.pozix.ui.components.richcontent

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hkm.pozix.R
import com.hkm.pozix.util.HapticUtil
import com.hkm.pozix.util.findActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConsoleLogItem(
    val level: ConsoleMessage.MessageLevel,
    val message: String,
    val sourceId: String,
    val lineNumber: Int,
    val timestamp: Long = System.currentTimeMillis()
)

private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

/**
 * Fullscreen Interactive Mini Browser Dialog for Pozix (v1.8.1 Production Polish).
 * Features:
 * - Isolated secure sandbox (https://sandbox.local/) with local storage & database support
 * - Recomposition reload guard prevents canvas / game frame resets during state changes
 * - Fluid touch & gesture responsiveness with CSS touch-action and hardware acceleration
 * - Horizontal scrolling for 1024px desktop view simulation on mobile displays
 * - Full lifecycle cleanup on dismiss prevents WebView memory leaks and background battery drain
 * - Real-time JavaScript console logs with error level filter and copy support
 * - Native JavaScript alert & confirm modal dialogs
 * - Dynamic color and dark/light canvas toggle
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlMiniBrowserDialog(
    code: String,
    language: String = "HTML",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isSystemDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var isDesktopView by remember { mutableStateOf(false) }
    var canvasDark by remember { mutableStateOf(isSystemDark) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Browser Live, 1 = Source Code
    var showConsole by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isReloading by remember { mutableStateOf(false) }
    var pageProgress by remember { mutableIntStateOf(0) }

    val consoleLogs = remember { mutableStateListOf<ConsoleLogItem>() }
    var filterLevel by remember { mutableStateOf<ConsoleMessage.MessageLevel?>(null) }

    // Native JS Dialog States
    var jsAlertMessage by remember { mutableStateOf<String?>(null) }
    var jsConfirmMessage by remember { mutableStateOf<String?>(null) }
    var jsConfirmResult by remember { mutableStateOf<JsResult?>(null) }

    // WebView reference for explicit lifecycle cleanup
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                loadUrl("about:blank")
                onPause()
                removeAllViews()
                destroy()
            }
            webViewRef = null
        }
    }

    val refreshRotation by animateFloatAsState(
        targetValue = if (isReloading) 360f else 0f,
        animationSpec = tween(600),
        label = "refreshAnim",
        finishedListener = { isReloading = false }
    )

    val isSvg = remember(language, code) {
        language.equals("SVG", ignoreCase = true) || code.trimStart().startsWith("<svg", ignoreCase = true)
    }

    val extractedTitle = remember(code) {
        val titleMatch = Regex("<title>([^<]+)</title>", RegexOption.IGNORE_CASE).find(code)
        titleMatch?.groupValues?.get(1)?.trim() ?: if (isSvg) "Vector Graphics (SVG)" else "HTML Document"
    }

    val finalHtml = remember(code, canvasDark, isSvg, isDesktopView) {
        val bgHex = if (canvasDark) "#181825" else "#FFFFFF"
        val textHex = if (canvasDark) "#CDD6F4" else "#1E293B"
        val viewportMeta = if (isDesktopView) {
            "<meta name=\"viewport\" content=\"width=1024, initial-scale=0.38, minimum-scale=0.25, maximum-scale=3.0\">"
        } else {
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, minimum-scale=0.5, maximum-scale=3.0, user-scalable=yes\">"
        }

        val canvasTouchCss = """
            <style id="pozix-touch-fix">
                canvas {
                    touch-action: none !important;
                    -webkit-touch-callout: none !important;
                    -webkit-user-select: none !important;
                    user-select: none !important;
                    display: block;
                    margin: 0 auto;
                    max-width: 100%;
                }
                * {
                    -webkit-tap-highlight-color: transparent;
                }
            </style>
        """.trimIndent()

        if (isSvg) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                $viewportMeta
                <style>
                    html, body {
                        margin: 0;
                        padding: 16px;
                        background: $bgHex;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        box-sizing: border-box;
                    }
                    svg {
                        max-width: 100%;
                        height: auto;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.15);
                        border-radius: 8px;
                    }
                </style>
            </head>
            <body>
                $code
            </body>
            </html>
            """.trimIndent()
        } else {
            val hasHtml = code.contains("<html", ignoreCase = true) && code.contains("</html>", ignoreCase = true)
            if (hasHtml) {
                var modified = code
                if (!modified.contains("<meta name=\"viewport\"", ignoreCase = true)) {
                    if (modified.contains("<head>", ignoreCase = true)) {
                        modified = modified.replaceFirst("<head>", "<head>$viewportMeta", ignoreCase = true)
                    } else if (modified.contains("<html>", ignoreCase = true)) {
                        modified = modified.replaceFirst("<html>", "<html><head>$viewportMeta</head>", ignoreCase = true)
                    }
                }
                if (modified.contains("</head>", ignoreCase = true)) {
                    modified = modified.replaceFirst("</head>", "$canvasTouchCss</head>", ignoreCase = true)
                } else if (modified.contains("<body>", ignoreCase = true)) {
                    modified = modified.replaceFirst("<body>", "<head>$canvasTouchCss</head><body>", ignoreCase = true)
                }
                modified
            } else {
                """
                <!DOCTYPE html>
                <html>
                <head>
                    $viewportMeta
                    $canvasTouchCss
                    <style>
                        html, body {
                            margin: 0;
                            padding: 16px;
                            background-color: $bgHex;
                            color: $textHex;
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                            font-size: 15px;
                            line-height: 1.6;
                            word-break: break-word;
                            box-sizing: border-box;
                        }
                        * {
                            box-sizing: border-box;
                        }
                        img, svg, video, iframe {
                            max-width: 100%;
                            height: auto;
                        }
                        table {
                            border-collapse: collapse;
                            width: 100%;
                            margin: 12px 0;
                        }
                        th, td {
                            border: 1px solid ${if (canvasDark) "#45475A" else "#CBD5E1"};
                            padding: 8px 12px;
                            text-align: left;
                        }
                        th {
                            background-color: ${if (canvasDark) "#313244" else "#F1F5F9"};
                            font-weight: 600;
                        }
                        button, input, select, textarea {
                            font-family: inherit;
                            font-size: 14px;
                            padding: 6px 12px;
                            border-radius: 8px;
                            border: 1px solid ${if (canvasDark) "#585B70" else "#94A3B8"};
                            background: ${if (canvasDark) "#313244" else "#F8FAFC"};
                            color: $textHex;
                        }
                        button:active {
                            opacity: 0.8;
                        }
                    </style>
                </head>
                <body>
                    $code
                </body>
                </html>
                """.trimIndent()
            }
        }
    }

    val errorCount = remember(consoleLogs.size) {
        consoleLogs.count { it.level == ConsoleMessage.MessageLevel.ERROR }
    }
    val warnCount = remember(consoleLogs.size) {
        consoleLogs.count { it.level == ConsoleMessage.MessageLevel.WARNING }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

        DisposableEffect(dialogView, isDark) {
            val dialogWindow = generateSequence(dialogView.parent) { it.parent }
                .filterIsInstance<androidx.compose.ui.window.DialogWindowProvider>()
                .firstOrNull()?.window
                ?: dialogView.context.findActivity()?.window

            if (dialogWindow != null) {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
                @Suppress("DEPRECATION")
                dialogWindow.statusBarColor = android.graphics.Color.TRANSPARENT
                @Suppress("DEPRECATION")
                dialogWindow.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    dialogWindow.isStatusBarContrastEnforced = false
                    dialogWindow.isNavigationBarContrastEnforced = false
                }
            }

            dialogView.context.findActivity()?.window?.let { actWindow ->
                val actController = androidx.core.view.WindowCompat.getInsetsController(actWindow, actWindow.decorView)
                actController.isAppearanceLightStatusBars = !isDark
                actController.isAppearanceLightNavigationBars = !isDark
            }

            onDispose {
                dialogView.context.findActivity()?.window?.let { actWindow ->
                    val actController = androidx.core.view.WindowCompat.getInsetsController(actWindow, actWindow.decorView)
                    actController.isAppearanceLightStatusBars = !isDark
                    actController.isAppearanceLightNavigationBars = !isDark
                }
            }
        }

        BackHandler {
            onDismiss()
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Browser Navigation Header
                BrowserHeader(
                    title = extractedTitle,
                    isReloading = isReloading,
                    refreshRotation = refreshRotation,
                    isDesktopView = isDesktopView,
                    canvasDark = canvasDark,
                    activeTab = activeTab,
                    consoleCount = consoleLogs.size,
                    hasError = errorCount > 0,
                    showConsole = showConsole,
                    onClose = onDismiss,
                    onReload = {
                        isReloading = true
                        refreshTrigger++
                        HapticUtil.lightTap(context)
                    },
                    onToggleDesktop = {
                        isDesktopView = !isDesktopView
                        HapticUtil.selectionTick(context)
                    },
                    onToggleCanvasDark = {
                        canvasDark = !canvasDark
                        HapticUtil.selectionTick(context)
                    },
                    onToggleTab = {
                        activeTab = if (activeTab == 0) 1 else 0
                        HapticUtil.selectionTick(context)
                    },
                    onToggleConsole = {
                        showConsole = !showConsole
                        HapticUtil.selectionTick(context)
                    },
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(code))
                        HapticUtil.actionConfirm(context)
                        Toast.makeText(context, context.getString(R.string.code_copied), Toast.LENGTH_SHORT).show()
                    }
                )

                // 2. Loading Progress Indicator
                if (pageProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { pageProgress / 100f },
                        modifier = Modifier.fillMaxWidth().height(2.5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                } else {
                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
                }

                // 3. Viewport Body (Browser Canvas vs Source Code)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (activeTab == 0) {
                        // Interactive Live Browser Viewport with Desktop Horizontal Pan Support
                        val desktopScrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (canvasDark) Color(0xFF181825) else Color.White)
                                .then(if (isDesktopView) Modifier.horizontalScroll(desktopScrollState) else Modifier),
                            contentAlignment = if (isDesktopView) Alignment.TopStart else Alignment.TopCenter
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        webViewRef = this
                                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                                        isFocusable = true
                                        isFocusableInTouchMode = true
                                        requestFocus()
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        isVerticalScrollBarEnabled = true
                                        isHorizontalScrollBarEnabled = true
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            databaseEnabled = true
                                            mediaPlaybackRequiresUserGesture = false
                                            setSupportZoom(true)
                                            builtInZoomControls = true
                                            displayZoomControls = false
                                            useWideViewPort = true
                                            loadWithOverviewMode = true
                                            allowFileAccess = false
                                            allowContentAccess = false
                                        }
                                        webChromeClient = object : WebChromeClient() {
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                pageProgress = newProgress
                                            }
                                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                                consoleMessage?.let {
                                                    consoleLogs.add(
                                                        ConsoleLogItem(
                                                            level = it.messageLevel(),
                                                            message = it.message().orEmpty(),
                                                            sourceId = it.sourceId().orEmpty(),
                                                            lineNumber = it.lineNumber()
                                                        )
                                                    )
                                                }
                                                return true
                                            }
                                            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                                                jsAlertMessage = message ?: ""
                                                result?.confirm()
                                                return true
                                            }
                                            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                                                jsConfirmMessage = message ?: ""
                                                jsConfirmResult = result
                                                return true
                                            }
                                        }
                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                                val url = request?.url?.toString().orEmpty()
                                                if (url.startsWith("#") || url.startsWith("javascript:")) {
                                                    return false
                                                }
                                                return true
                                            }
                                            @Suppress("DEPRECATION")
                                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                                val u = url.orEmpty()
                                                if (u.startsWith("#") || u.startsWith("javascript:")) {
                                                    return false
                                                }
                                                return true
                                            }
                                        }
                                    }
                                },
                                update = { webView ->
                                    val loadKey = "$refreshTrigger:$canvasDark:$isDesktopView:${code.hashCode()}"
                                    if (webView.tag != loadKey) {
                                        webView.tag = loadKey
                                        webView.loadDataWithBaseURL("https://sandbox.local/", finalHtml, "text/html", "UTF-8", null)
                                    }
                                },
                                modifier = if (isDesktopView) Modifier.width(1024.dp).fillMaxHeight() else Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        // Raw Source Code Tab
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(14.dp)
                        ) {
                            val vScroll = rememberScrollState()
                            val hScroll = rememberScrollState()
                            Text(
                                text = code,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(vScroll)
                                    .horizontalScroll(hScroll)
                            )
                        }
                    }
                }

                // 4. Expandable Console Logs Drawer with Filters & Quick Copy
                AnimatedVisibility(
                    visible = showConsole,
                    enter = slideInVertically { it } + expandVertically(),
                    exit = slideOutVertically { it } + shrinkVertically()
                ) {
                    ConsoleDrawer(
                        logs = consoleLogs,
                        filterLevel = filterLevel,
                        errorCount = errorCount,
                        warnCount = warnCount,
                        onFilterChange = { filterLevel = it },
                        onClear = {
                            consoleLogs.clear()
                            HapticUtil.lightTap(context)
                        },
                        onCopyAll = {
                            if (consoleLogs.isNotEmpty()) {
                                val logText = consoleLogs.joinToString("\n") { item ->
                                    val tag = when (item.level) {
                                        ConsoleMessage.MessageLevel.ERROR -> "[ERR]"
                                        ConsoleMessage.MessageLevel.WARNING -> "[WARN]"
                                        else -> "[LOG]"
                                    }
                                    val time = timeFormatter.format(Date(item.timestamp))
                                    "$time $tag ${item.message}${if (item.lineNumber > 0) " (line ${item.lineNumber})" else ""}"
                                }
                                clipboardManager.setText(AnnotatedString(logText))
                                HapticUtil.actionConfirm(context)
                                Toast.makeText(context, "Đã sao chép ${consoleLogs.size} dòng log", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onClose = { showConsole = false }
                    )
                }

                // 5. Bottom Browser Status Bar with Error Badge
                BrowserStatusBar(
                    isDesktopView = isDesktopView,
                    canvasDark = canvasDark,
                    consoleCount = consoleLogs.size,
                    hasError = errorCount > 0,
                    showConsole = showConsole,
                    onToggleConsole = { showConsole = !showConsole }
                )
            }
        }

        // Native JS Alert Dialog
        if (jsAlertMessage != null) {
            AlertDialog(
                onDismissRequest = { jsAlertMessage = null },
                title = { Text("JavaScript Alert", fontWeight = FontWeight.Bold) },
                text = { Text(jsAlertMessage!!) },
                confirmButton = {
                    TextButton(onClick = { jsAlertMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }

        // Native JS Confirm Dialog
        if (jsConfirmMessage != null) {
            AlertDialog(
                onDismissRequest = {
                    jsConfirmResult?.cancel()
                    jsConfirmMessage = null
                },
                title = { Text("JavaScript Confirm", fontWeight = FontWeight.Bold) },
                text = { Text(jsConfirmMessage!!) },
                confirmButton = {
                    TextButton(onClick = {
                        jsConfirmResult?.confirm()
                        jsConfirmMessage = null
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        jsConfirmResult?.cancel()
                        jsConfirmMessage = null
                    }) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}

@Composable
private fun BrowserHeader(
    title: String,
    isReloading: Boolean,
    refreshRotation: Float,
    isDesktopView: Boolean,
    canvasDark: Boolean,
    activeTab: Int,
    consoleCount: Int,
    hasError: Boolean,
    showConsole: Boolean,
    onClose: () -> Unit,
    onReload: () -> Unit,
    onToggleDesktop: () -> Unit,
    onToggleCanvasDark: () -> Unit,
    onToggleTab: () -> Unit,
    onToggleConsole: () -> Unit,
    onCopy: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Row 1: Back + Smart Omnibox + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Smart Omnibox
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Safe Local Sandbox",
                            tint = Color(0xFF10B981), // Emerald green lock
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onReload,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Tải lại",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp).rotate(refreshRotation)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Desktop / Mobile Viewport
                IconButton(onClick = onToggleDesktop, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = if (isDesktopView) Icons.Default.DesktopWindows else Icons.Default.PhoneAndroid,
                        contentDescription = if (isDesktopView) "Chế độ máy tính" else "Chế độ di động",
                        tint = if (isDesktopView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Light / Dark Canvas Toggle
                IconButton(onClick = onToggleCanvasDark, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = if (canvasDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Đổi nền Canvas",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Code vs Preview toggle
                IconButton(onClick = onToggleTab, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = if (activeTab == 0) Icons.Default.Code else Icons.Default.Visibility,
                        contentDescription = if (activeTab == 0) "Xem mã nguồn" else "Xem giao diện",
                        tint = if (activeTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Copy source
                IconButton(onClick = onCopy, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.code_copy_content_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsoleDrawer(
    logs: List<ConsoleLogItem>,
    filterLevel: ConsoleMessage.MessageLevel?,
    errorCount: Int,
    warnCount: Int,
    onFilterChange: (ConsoleMessage.MessageLevel?) -> Unit,
    onClear: () -> Unit,
    onCopyAll: () -> Unit,
    onClose: () -> Unit
) {
    val filteredLogs = remember(logs.size, filterLevel) {
        if (filterLevel == null) logs else logs.filter { it.level == filterLevel }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        color = Color(0xFF1E1E2E), // Catppuccin Base dark
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFF313244))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181825))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = Color(0xFF89B4FA),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Console (${logs.size})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCDD6F4)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (logs.isNotEmpty()) {
                        TextButton(onClick = onCopyAll) {
                            Text("Sao chép", color = Color(0xFF89B4FA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(onClick = onClear) {
                            Text("Xóa", color = Color(0xFFF38BA8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Đóng", tint = Color(0xFFA6ADC8), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Filter Chips Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E2E))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ConsoleFilterChip(
                    text = "Tất cả (${logs.size})",
                    isSelected = filterLevel == null,
                    color = Color(0xFF89B4FA),
                    onClick = { onFilterChange(null) }
                )
                if (errorCount > 0) {
                    ConsoleFilterChip(
                        text = "Lỗi ($errorCount)",
                        isSelected = filterLevel == ConsoleMessage.MessageLevel.ERROR,
                        color = Color(0xFFF38BA8),
                        onClick = { onFilterChange(ConsoleMessage.MessageLevel.ERROR) }
                    )
                }
                if (warnCount > 0) {
                    ConsoleFilterChip(
                        text = "Cảnh báo ($warnCount)",
                        isSelected = filterLevel == ConsoleMessage.MessageLevel.WARNING,
                        color = Color(0xFFF9E2AF),
                        onClick = { onFilterChange(ConsoleMessage.MessageLevel.WARNING) }
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF313244), thickness = 0.5.dp)

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (logs.isEmpty()) "Chưa có console log. Thử console.log() trong mã HTML!" else "Không có log nào khớp bộ lọc",
                        color = Color(0xFF6C7086),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredLogs) { log ->
                        val (tagColor, tagText) = when (log.level) {
                            ConsoleMessage.MessageLevel.ERROR -> Color(0xFFF38BA8) to "[ERR]"
                            ConsoleMessage.MessageLevel.WARNING -> Color(0xFFF9E2AF) to "[WARN]"
                            else -> Color(0xFF89DCEB) to "[LOG]"
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Text(
                                text = timeFormatter.format(Date(log.timestamp)),
                                color = Color(0xFF6C7086),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tagText,
                                color = tagColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = log.message,
                                color = Color(0xFFCDD6F4),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            if (log.lineNumber > 0) {
                                Text(
                                    text = ":${log.lineNumber}",
                                    color = Color(0xFF6C7086),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleFilterChip(
    text: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) color else Color(0xFF45475A))
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else Color(0xFFA6ADC8),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun BrowserStatusBar(
    isDesktopView: Boolean,
    canvasDark: Boolean,
    consoleCount: Int,
    hasError: Boolean,
    showConsole: Boolean,
    onToggleConsole: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = if (isDesktopView) "1024px Desktop" else "Responsive Mobile",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = if (canvasDark) "Dark Canvas" else "Light Canvas",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Surface(
                onClick = onToggleConsole,
                shape = RoundedCornerShape(6.dp),
                color = when {
                    showConsole -> MaterialTheme.colorScheme.primaryContainer
                    hasError -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (hasError) Icons.Default.ErrorOutline else Icons.Default.BugReport,
                        contentDescription = null,
                        tint = when {
                            showConsole -> MaterialTheme.colorScheme.onPrimaryContainer
                            hasError -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Console ($consoleCount)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            showConsole -> MaterialTheme.colorScheme.onPrimaryContainer
                            hasError -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
