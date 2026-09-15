package com.hkm.pozix.ui.components.richcontent

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.hkm.pozix.R
import com.hkm.pozix.util.HapticUtil
import com.hkm.pozix.util.findActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

enum class ViewportMode(val title: String, val widthDp: Int?) {
    MOBILE("Mobile (375px)", 375),
    TABLET("Tablet (768px)", 768),
    DESKTOP("Desktop (1024px)", 1024),
    FULLSCREEN("Toàn màn hình", null)
}

data class ConsoleLogItem(
    val level: ConsoleMessage.MessageLevel,
    val message: String,
    val sourceId: String,
    val lineNumber: Int,
    val timestamp: Long = System.currentTimeMillis()
)

private data class JsPromptData(
    val message: String,
    val defaultValue: String,
    val result: JsPromptResult
)

private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

/**
 * Super Pro Interactive Mini Browser for Pozix.
 * Full HTML5 Engine, Hardened Security, Chromium crash resilience,
 * Fullscreen Canvas/Video overlay, Navigation & Omnibox controls,
 * Find In Page, 4-mode Viewport switcher, Edge Swipe gestures,
 * Global JS runtime error interceptor, and Live Code Editor.
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

    // Document state
    var currentCode by remember(code) { mutableStateOf(code) }
    var currentFileName by remember { mutableStateOf<String?>(null) }

    // Navigation & Viewport states
    var currentViewport by remember { mutableStateOf(ViewportMode.FULLSCREEN) }
    var isDesktopUa by remember { mutableStateOf(false) }
    var showViewportDialog by remember { mutableStateOf(false) }

    var canvasDark by remember { mutableStateOf(isSystemDark) }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Live Browser, 1 = Source Code / Editor
    var showConsole by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isReloading by remember { mutableStateOf(false) }

    // Live Web metrics
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var pageProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var livePageTitle by remember { mutableStateOf<String?>(null) }
    var liveFavicon by remember { mutableStateOf<Bitmap?>(null) }

    // Video/Canvas Fullscreen CustomView state
    var customView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Find In Page state
    var isSearchingInPage by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var findMatchIndex by remember { mutableIntStateOf(0) }
    var findTotalMatches by remember { mutableIntStateOf(0) }

    // Edge Swipe states
    var edgeSwipeDirection by remember { mutableStateOf<Int?>(null) } // -1 = Back, 1 = Forward
    var edgeSwipeDragAmount by remember { mutableFloatStateOf(0f) }
    var edgeSwipeTriggered by remember { mutableStateOf(false) }

    // Console logs & filters
    val consoleLogs = remember { mutableStateListOf<ConsoleLogItem>() }
    var filterLevel by remember { mutableStateOf<ConsoleMessage.MessageLevel?>(null) }

    // Native JS Dialog States
    var jsAlertMessage by remember { mutableStateOf<String?>(null) }
    var jsConfirmMessage by remember { mutableStateOf<String?>(null) }
    var jsConfirmResult by remember { mutableStateOf<JsResult?>(null) }
    var jsPromptData by remember { mutableStateOf<JsPromptData?>(null) }
    var promptInputText by remember { mutableStateOf("") }

    // Export Dialog State
    var showExportDialog by remember { mutableStateOf(false) }

    // WebView reference
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // File Chooser Callback for <input type="file">
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val webViewFileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        val clipData = result.data?.clipData
        val uris = when {
            clipData != null -> Array(clipData.itemCount) { clipData.getItemAt(it).uri }
            uri != null -> arrayOf(uri)
            else -> null
        }
        fileChooserCallback?.onReceiveValue(uris)
        fileChooserCallback = null
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!content.isNullOrBlank()) {
                    val name = getFileNameFromUri(context, it) ?: "Tập tin HTML"
                    currentCode = content
                    currentFileName = name
                    refreshTrigger++
                    HapticUtil.actionConfirm(context)
                    Toast.makeText(context, "Đã mở: $name", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Không thể đọc file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(currentCode.toByteArray(Charsets.UTF_8))
                }
                HapticUtil.actionConfirm(context)
                Toast.makeText(context, "Đã lưu file HTML thành công!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi lưu file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            fileChooserCallback?.onReceiveValue(null)
            fileChooserCallback = null
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
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

    val isSvg = remember(language, currentCode) {
        language.equals("SVG", ignoreCase = true) || currentCode.trimStart().startsWith("<svg", ignoreCase = true)
    }

    val extractedTitle = remember(currentCode, currentFileName) {
        if (!currentFileName.isNullOrBlank()) {
            currentFileName!!
        } else {
            val titleMatch = Regex("<title>([^<]+)</title>", RegexOption.IGNORE_CASE).find(currentCode)
            titleMatch?.groupValues?.get(1)?.trim() ?: if (isSvg) "Vector Graphics (SVG)" else "HTML Document"
        }
    }

    // Global JS runtime error & unhandled rejection interceptor script
    val errorInterceptorScript = """
        <script id="pozix-error-interceptor">
        (function() {
            window.onerror = function(message, source, lineno, colno, error) {
                var src = source || 'inline';
                var msg = '[JS Runtime Error] ' + message + ' (' + src + ':' + lineno + (colno ? ':' + colno : '') + ')';
                if (error && error.stack) {
                    msg += '\n' + error.stack;
                }
                console.error(msg);
                return false;
            };
            window.addEventListener('unhandledrejection', function(event) {
                var reason = event.reason;
                var msg = reason ? (reason.stack || reason.message || reason) : 'Unhandled Promise Rejection';
                console.error('[Unhandled Promise] ' + msg);
            });
        })();
        </script>
    """.trimIndent()

    val finalHtml = remember(currentCode, canvasDark, isSvg, currentViewport) {
        val viewportMeta = when (currentViewport) {
            ViewportMode.MOBILE -> "<meta name=\"viewport\" content=\"width=375, initial-scale=1.0, maximum-scale=3.0\">"
            ViewportMode.TABLET -> "<meta name=\"viewport\" content=\"width=768, initial-scale=0.75, maximum-scale=3.0\">"
            ViewportMode.DESKTOP -> "<meta name=\"viewport\" content=\"width=1024, initial-scale=0.38, minimum-scale=0.25, maximum-scale=3.0\">"
            ViewportMode.FULLSCREEN -> "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, minimum-scale=0.5, maximum-scale=3.0, user-scalable=yes\">"
        }

        val hasCanvas = currentCode.contains("<canvas", ignoreCase = true)

        val proEngineCss = buildString {
            append("""
                <meta name="color-scheme" content="${if (canvasDark) "dark" else "light"}">
                <style id="pozix-pro-engine">
                    :root {
                        color-scheme: ${if (canvasDark) "dark" else "light"};
                    }
                    html {
                        min-height: 100%;
                        height: 100%;
                        width: 100%;
                        background-color: transparent;
                    }
                    body {
                        min-height: 100%;
                        width: 100%;
                        margin: 0;
                        box-sizing: border-box;
                        -webkit-tap-highlight-color: transparent;
                    }
            """.trimIndent())

            if (hasCanvas) {
                append("""
                    
                    /* Auto-center Canvas games perfectly both horizontally and vertically */
                    body {
                        display: flex;
                        flex-direction: column;
                        justify-content: center;
                        align-items: center;
                    }
                    canvas {
                        margin: auto !important;
                        display: block !important;
                        max-width: 100% !important;
                        max-height: 88vh !important;
                        touch-action: none !important;
                        user-select: none !important;
                        -webkit-user-select: none !important;
                    }
                """.trimIndent())
            }

            append("\n</style>")
        }

        if (isSvg) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                $viewportMeta
                $errorInterceptorScript
                $proEngineCss
                <style>
                    html, body {
                        margin: 0;
                        padding: 16px;
                        background: transparent;
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
                $currentCode
            </body>
            </html>
            """.trimIndent()
        } else {
            val hasHtml = currentCode.contains("<html", ignoreCase = true) && currentCode.contains("</html>", ignoreCase = true)
            if (hasHtml) {
                var modified = currentCode
                val headOpenRegex = Regex("<head[^>]*>", RegexOption.IGNORE_CASE)
                val headCloseRegex = Regex("</head>", RegexOption.IGNORE_CASE)
                val htmlOpenRegex = Regex("<html[^>]*>", RegexOption.IGNORE_CASE)
                val bodyOpenRegex = Regex("<body[^>]*>", RegexOption.IGNORE_CASE)

                // 1. Inject error interceptor and viewport right after the first <head> tag
                val headMatch = headOpenRegex.find(modified)
                if (headMatch != null) {
                    val insertion = "${headMatch.value}\n$errorInterceptorScript\n$viewportMeta"
                    modified = modified.replaceRange(headMatch.range, insertion)
                } else {
                    val htmlMatch = htmlOpenRegex.find(modified)
                    if (htmlMatch != null) {
                        modified = modified.replaceRange(htmlMatch.range, "${htmlMatch.value}\n<head>\n$errorInterceptorScript\n$viewportMeta\n</head>")
                    } else {
                        modified = "<head>\n$errorInterceptorScript\n$viewportMeta\n</head>\n$modified"
                    }
                }

                // 2. Inject proEngineCss right before </head> or <body>
                val headCloseMatch = headCloseRegex.find(modified)
                if (headCloseMatch != null) {
                    modified = modified.replaceRange(headCloseMatch.range, "$proEngineCss\n${headCloseMatch.value}")
                } else {
                    val bodyMatch = bodyOpenRegex.find(modified)
                    if (bodyMatch != null) {
                        modified = modified.replaceRange(bodyMatch.range, "<head>\n$proEngineCss\n</head>\n${bodyMatch.value}")
                    }
                }
                modified
            } else {
                val textHex = if (canvasDark) "#CDD6F4" else "#1E293B"
                """
                <!DOCTYPE html>
                <html>
                <head>
                    $viewportMeta
                    $errorInterceptorScript
                    $proEngineCss
                    <style>
                        body {
                            margin: 0;
                            padding: 12px;
                            color: $textHex;
                            font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                            font-size: 15px;
                            line-height: 1.5;
                        }
                        *, *:before, *:after {
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
                    $currentCode
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
    val infoCount = remember(consoleLogs.size, errorCount, warnCount) {
        (consoleLogs.size - errorCount - warnCount).coerceAtLeast(0)
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    dialogWindow.isStatusBarContrastEnforced = false
                    dialogWindow.isNavigationBarContrastEnforced = false
                }
                dialogWindow.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
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

        // BackHandler with customView priority
        BackHandler {
            if (customView != null) {
                customViewCallback?.onCustomViewHidden()
                customView = null
                customViewCallback = null
            } else if (isSearchingInPage) {
                isSearchingInPage = false
                webViewRef?.clearMatches()
                searchQuery = ""
            } else if (canGoBack && webViewRef?.canGoBack() == true) {
                webViewRef?.goBack()
            } else {
                onDismiss()
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (canvasDark) Color(0xFF181825) else MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Two-Row Pro Browser Header with live favicon, title & reload/stop
                    BrowserHeaderPro(
                        title = livePageTitle?.takeIf { it.isNotBlank() } ?: extractedTitle,
                        favicon = liveFavicon,
                        isLoading = isLoading,
                        isReloading = isReloading,
                        refreshRotation = refreshRotation,
                        viewportMode = currentViewport,
                        canvasDark = canvasDark,
                        activeTab = activeTab,
                        onClose = onDismiss,
                        onReloadOrStop = {
                            if (isLoading) {
                                webViewRef?.stopLoading()
                                isLoading = false
                                HapticUtil.lightTap(context)
                            } else {
                                isReloading = true
                                refreshTrigger++
                                webViewRef?.reload()
                                HapticUtil.lightTap(context)
                            }
                        },
                        onHardRefresh = {
                            webViewRef?.clearCache(true)
                            refreshTrigger++
                            webViewRef?.reload()
                            HapticUtil.actionConfirm(context)
                            Toast.makeText(context, "Hard Refresh: Đã làm mới & xóa sạch bộ nhớ đệm!", Toast.LENGTH_SHORT).show()
                        },
                        onOpenFile = {
                            openFileLauncher.launch("*/*")
                            HapticUtil.lightTap(context)
                        },
                        onExportFile = {
                            showExportDialog = true
                            HapticUtil.lightTap(context)
                        },
                        onToggleViewport = {
                            showViewportDialog = true
                            HapticUtil.selectionTick(context)
                        },
                        onToggleCanvasDark = {
                            canvasDark = !canvasDark
                            HapticUtil.selectionTick(context)
                            Toast.makeText(
                                context,
                                if (canvasDark) "Đã bật nền Tối (Dark Canvas)" else "Đã bật nền Sáng (Light Canvas)",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onToggleTab = {
                            activeTab = if (activeTab == 0) 1 else 0
                            HapticUtil.selectionTick(context)
                        },
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(currentCode))
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

                    // 3. Find In Page Bar
                    AnimatedVisibility(
                        visible = isSearchingInPage,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        FindInPageBar(
                            query = searchQuery,
                            matchIndex = findMatchIndex,
                            totalMatches = findTotalMatches,
                            onQueryChange = { newQuery ->
                                searchQuery = newQuery
                                if (newQuery.isNotBlank()) {
                                    webViewRef?.findAllAsync(newQuery)
                                } else {
                                    webViewRef?.clearMatches()
                                    findMatchIndex = 0
                                    findTotalMatches = 0
                                }
                            },
                            onFindNext = {
                                webViewRef?.findNext(true)
                                HapticUtil.lightTap(context)
                            },
                            onFindPrev = {
                                webViewRef?.findNext(false)
                                HapticUtil.lightTap(context)
                            },
                            onClose = {
                                isSearchingInPage = false
                                webViewRef?.clearMatches()
                                searchQuery = ""
                                HapticUtil.lightTap(context)
                            }
                        )
                    }

                    // 4. Viewport Body (Live Browser with Edge Swipe vs Source Code Editor)
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (activeTab == 0) {
                            val desktopScrollState = rememberScrollState()
                            val density = LocalDensity.current
                            val triggerThresholdPx = with(density) { 64.dp.toPx() }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(if (canvasDark) Color(0xFF181825) else Color.White)
                                    .then(
                                        if (currentViewport.widthDp != null) {
                                            Modifier.horizontalScroll(desktopScrollState)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = if (currentViewport.widthDp != null) Alignment.TopCenter else Alignment.Center
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

                                            // Setup Find Listener
                                            setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                                                findMatchIndex = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0
                                                findTotalMatches = numberOfMatches
                                            }

                                            // 1. WebSettings & Hardened Security
                                            settings.apply {
                                                javaScriptEnabled = true
                                                domStorageEnabled = true
                                                @Suppress("DEPRECATION")
                                                databaseEnabled = true
                                                mediaPlaybackRequiresUserGesture = false
                                                allowFileAccess = false
                                                allowContentAccess = false
                                                @Suppress("DEPRECATION")
                                                allowFileAccessFromFileURLs = false
                                                @Suppress("DEPRECATION")
                                                allowUniversalAccessFromFileURLs = false
                                                cacheMode = WebSettings.LOAD_DEFAULT
                                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                                loadsImagesAutomatically = true
                                                setSupportZoom(true)
                                                builtInZoomControls = true
                                                displayZoomControls = false
                                                useWideViewPort = true
                                                loadWithOverviewMode = true

                                                userAgentString = if (isDesktopUa) {
                                                    DESKTOP_USER_AGENT
                                                } else {
                                                    WebSettings.getDefaultUserAgent(ctx)
                                                }

                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    isAlgorithmicDarkeningAllowed = canvasDark
                                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    @Suppress("DEPRECATION")
                                                    forceDark = if (canvasDark) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
                                                }
                                            }

                                            // 2. WebChromeClient with customView & audio/video
                                            webChromeClient = object : WebChromeClient() {
                                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                    pageProgress = newProgress
                                                    isLoading = newProgress < 100
                                                    canGoBack = view?.canGoBack() == true
                                                    canGoForward = view?.canGoForward() == true
                                                }

                                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                                    if (!title.isNullOrBlank() && !title.startsWith("https://") && !title.startsWith("data:")) {
                                                        livePageTitle = title
                                                    }
                                                }

                                                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                                                    liveFavicon = icon
                                                }

                                                override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                                                    customView = view
                                                    customViewCallback = callback
                                                }

                                                override fun onHideCustomView() {
                                                    customViewCallback?.onCustomViewHidden()
                                                    customView = null
                                                    customViewCallback = null
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

                                                override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
                                                    if (result != null) {
                                                        promptInputText = defaultValue.orEmpty()
                                                        jsPromptData = JsPromptData(message.orEmpty(), defaultValue.orEmpty(), result)
                                                        return true
                                                    }
                                                    return false
                                                }

                                                override fun onPermissionRequest(request: PermissionRequest?) {
                                                    request?.grant(request.resources)
                                                }

                                                override fun onShowFileChooser(
                                                    webView: WebView?,
                                                    filePathCallback: ValueCallback<Array<Uri>>?,
                                                    fileChooserParams: FileChooserParams?
                                                ): Boolean {
                                                    fileChooserCallback?.onReceiveValue(null)
                                                    fileChooserCallback = filePathCallback
                                                    return try {
                                                        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                                            type = "*/*"
                                                        }
                                                        webViewFileChooserLauncher.launch(intent)
                                                        true
                                                    } catch (e: Exception) {
                                                        fileChooserCallback = null
                                                        false
                                                    }
                                                }
                                            }

                                            // 3. WebViewClient with Chromium crash resilience
                                            webViewClient = object : WebViewClient() {
                                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                    isLoading = true
                                                    canGoBack = view?.canGoBack() == true
                                                    canGoForward = view?.canGoForward() == true
                                                    if (favicon != null) liveFavicon = favicon
                                                }

                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    isLoading = false
                                                    canGoBack = view?.canGoBack() == true
                                                    canGoForward = view?.canGoForward() == true
                                                    if (livePageTitle.isNullOrBlank()) {
                                                        val t = view?.title
                                                        if (!t.isNullOrBlank() && !t.startsWith("https://") && !t.startsWith("data:")) {
                                                            livePageTitle = t
                                                        }
                                                    }
                                                }

                                                override fun onRenderProcessGone(
                                                    view: WebView?,
                                                    detail: android.webkit.RenderProcessGoneDetail?
                                                ): Boolean {
                                                    val didCrash = detail?.didCrash() == true
                                                    consoleLogs.add(
                                                        ConsoleLogItem(
                                                            level = ConsoleMessage.MessageLevel.ERROR,
                                                            message = "Tiến trình Chromium bị dừng (${if (didCrash) "Sập GPU/WebGL" else "Hệ thống dừng"}). Đã bật cơ chế tự bảo vệ Pozix.",
                                                            sourceId = "Chromium",
                                                            lineNumber = 0
                                                        )
                                                    )
                                                    view?.let {
                                                        try {
                                                            (it.parent as? android.view.ViewGroup)?.removeView(it)
                                                            it.destroy()
                                                        } catch (_: Exception) {}
                                                    }
                                                    return true
                                                }

                                                override fun onReceivedError(
                                                    view: WebView?,
                                                    request: WebResourceRequest?,
                                                    error: WebResourceError?
                                                ) {
                                                    super.onReceivedError(view, request, error)
                                                    val desc = error?.description?.toString().orEmpty()
                                                    val code = error?.errorCode ?: 0
                                                    val url = request?.url?.toString().orEmpty()
                                                    if (url.isNotEmpty() && !url.startsWith("data:")) {
                                                        consoleLogs.add(
                                                            ConsoleLogItem(
                                                                level = ConsoleMessage.MessageLevel.ERROR,
                                                                message = "Lỗi nạp CDN/mạng [$code]: $desc ($url)",
                                                                sourceId = "Network",
                                                                lineNumber = 0
                                                            )
                                                        )
                                                    }
                                                }

                                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                                    val url = request?.url?.toString().orEmpty()
                                                    return !url.startsWith("#") && !url.startsWith("javascript:")
                                                }

                                                @Suppress("DEPRECATION")
                                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                                    val u = url.orEmpty()
                                                    return !u.startsWith("#") && !u.startsWith("javascript:")
                                                }
                                            }
                                        }
                                    },
                                    update = { webView ->
                                        val loadKey = "$refreshTrigger:$canvasDark:${currentViewport.name}:$isDesktopUa:${currentCode.hashCode()}"
                                        if (webView.tag != loadKey) {
                                            webView.tag = loadKey

                                            webView.settings.userAgentString = if (isDesktopUa) {
                                                DESKTOP_USER_AGENT
                                            } else {
                                                WebSettings.getDefaultUserAgent(context)
                                            }

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                webView.settings.isAlgorithmicDarkeningAllowed = canvasDark
                                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                @Suppress("DEPRECATION")
                                                webView.settings.forceDark = if (canvasDark) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
                                            }
                                            webView.loadDataWithBaseURL("https://sandbox.local/", finalHtml, "text/html", "UTF-8", null)
                                        }
                                    },
                                    modifier = if (currentViewport.widthDp != null) {
                                        Modifier.width(currentViewport.widthDp!!.dp).fillMaxHeight()
                                    } else {
                                        Modifier.fillMaxSize()
                                    }
                                )
                            }

                            // 4.1 Edge Swipe Interception Strips
                            // Left Edge Strip (Swipe Right for Back)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .width(26.dp)
                                    .fillMaxHeight()
                                    .pointerInput(canGoBack) {
                                        detectHorizontalDragGestures(
                                            onDragStart = {
                                                edgeSwipeDirection = -1
                                                edgeSwipeDragAmount = 0f
                                                edgeSwipeTriggered = false
                                            },
                                            onDragEnd = {
                                                if (edgeSwipeTriggered && canGoBack) {
                                                    webViewRef?.goBack()
                                                    HapticUtil.actionConfirm(context)
                                                }
                                                edgeSwipeDirection = null
                                                edgeSwipeDragAmount = 0f
                                                edgeSwipeTriggered = false
                                            },
                                            onDragCancel = {
                                                edgeSwipeDirection = null
                                                edgeSwipeDragAmount = 0f
                                                edgeSwipeTriggered = false
                                            },
                                            onHorizontalDrag = { _, dragAmount ->
                                                edgeSwipeDragAmount = (edgeSwipeDragAmount + dragAmount).coerceAtLeast(0f)
                                                if (edgeSwipeDragAmount >= triggerThresholdPx && !edgeSwipeTriggered) {
                                                    edgeSwipeTriggered = true
                                                    HapticUtil.selectionTick(context)
                                                } else if (edgeSwipeDragAmount < triggerThresholdPx && edgeSwipeTriggered) {
                                                    edgeSwipeTriggered = false
                                                }
                                            }
                                        )
                                    }
                            )

                            // Right Edge Strip (Swipe Left for Forward)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(26.dp)
                                    .fillMaxHeight()
                                    .pointerInput(canGoForward) {
                                        detectHorizontalDragGestures(
                                            onDragStart = {
                                                edgeSwipeDirection = 1
                                                edgeSwipeDragAmount = 0f
                                                edgeSwipeTriggered = false
                                            },
                                            onDragEnd = {
                                                if (edgeSwipeTriggered && canGoForward) {
                                                    webViewRef?.goForward()
                                                    HapticUtil.actionConfirm(context)
                                                }
                                                edgeSwipeDirection = null
                                                edgeSwipeDragAmount = 0f
                                                edgeSwipeTriggered = false
                                            },
                                            onDragCancel = {
                                                edgeSwipeDirection = null
                                                edgeSwipeDragAmount = 0f
                                                edgeSwipeTriggered = false
                                            },
                                            onHorizontalDrag = { _, dragAmount ->
                                                edgeSwipeDragAmount = (edgeSwipeDragAmount - dragAmount).coerceAtLeast(0f)
                                                if (edgeSwipeDragAmount >= triggerThresholdPx && !edgeSwipeTriggered) {
                                                    edgeSwipeTriggered = true
                                                    HapticUtil.selectionTick(context)
                                                } else if (edgeSwipeDragAmount < triggerThresholdPx && edgeSwipeTriggered) {
                                                    edgeSwipeTriggered = false
                                                }
                                            }
                                        )
                                    }
                            )

                            // 4.2 Floating Indicator Bubble for Edge Swipe
                            if (edgeSwipeDirection != null) {
                                val isBack = edgeSwipeDirection == -1
                                val canPerform = if (isBack) canGoBack else canGoForward
                                val isTriggered = edgeSwipeTriggered

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(100f),
                                    contentAlignment = if (isBack) Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isTriggered && canPerform) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f)
                                        },
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (isTriggered && canPerform) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        shadowElevation = 8.dp,
                                        modifier = Modifier.padding(
                                            start = if (isBack) (16 + (edgeSwipeDragAmount * 0.15f).coerceIn(0f, 28f)).dp else 0.dp,
                                            end = if (!isBack) (16 + (edgeSwipeDragAmount * 0.15f).coerceIn(0f, 28f)).dp else 0.dp
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isBack) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = null,
                                                    tint = if (canPerform) MaterialTheme.colorScheme.primary else Color.Gray,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (canPerform) "Quay lại" else "Hết trang",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (canPerform) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray
                                                )
                                            } else {
                                                Text(
                                                    text = if (canPerform) "Tiếp theo" else "Hết trang",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (canPerform) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    tint = if (canPerform) MaterialTheme.colorScheme.primary else Color.Gray,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // 4.3 CodeViewerAndEditor (Syntax Highlight + Quick Live Editor)
                            CodeViewerAndEditor(
                                originalCode = code,
                                initialCode = currentCode,
                                isDark = isDark,
                                onApply = { newCode ->
                                    currentCode = newCode
                                    refreshTrigger++
                                    activeTab = 0
                                    HapticUtil.actionConfirm(context)
                                    Toast.makeText(context, "Đã áp dụng thay đổi mã HTML!", Toast.LENGTH_SHORT).show()
                                },
                                onReset = {
                                    currentCode = code
                                    refreshTrigger++
                                    HapticUtil.actionConfirm(context)
                                    Toast.makeText(context, "Đã khôi phục về mã gốc!", Toast.LENGTH_SHORT).show()
                                },
                                onExport = {
                                    showExportDialog = true
                                    HapticUtil.lightTap(context)
                                }
                            )
                        }
                    }

                    // 5. Expandable Console Logs Drawer with Filters & Quick Copy
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
                            infoCount = infoCount,
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

                    // 6. Bottom Browser Status Bar (Back, Forward, Zoom, Find, Viewport, Console)
                    BrowserStatusBar(
                        canGoBack = canGoBack,
                        canGoForward = canGoForward,
                        viewportMode = currentViewport,
                        consoleCount = consoleLogs.size,
                        errorCount = errorCount,
                        showConsole = showConsole,
                        onBack = {
                            webViewRef?.goBack()
                            HapticUtil.lightTap(context)
                        },
                        onForward = {
                            webViewRef?.goForward()
                            HapticUtil.lightTap(context)
                        },
                        onToggleFind = {
                            isSearchingInPage = !isSearchingInPage
                            if (!isSearchingInPage) {
                                webViewRef?.clearMatches()
                                searchQuery = ""
                            }
                            HapticUtil.lightTap(context)
                        },
                        onToggleViewport = {
                            showViewportDialog = true
                            HapticUtil.lightTap(context)
                        },
                        onToggleConsole = {
                            showConsole = !showConsole
                            HapticUtil.lightTap(context)
                        },
                        onZoomIn = {
                            webViewRef?.zoomIn()
                            HapticUtil.lightTap(context)
                        },
                        onZoomOut = {
                            webViewRef?.zoomOut()
                            HapticUtil.lightTap(context)
                        }
                    )
                }

                // 7. Video / Canvas Fullscreen Overlay (zIndex 999f)
                if (customView != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(999f)
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            factory = { customView!! },
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = {
                                customViewCallback?.onCustomViewHidden()
                                customView = null
                                customViewCallback = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Thoát toàn màn hình",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Viewport Switcher Modal Dialog
        if (showViewportDialog) {
            AlertDialog(
                onDismissRequest = { showViewportDialog = false },
                title = { Text("Chế độ hiển thị & Viewport", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ViewportMode.entries.forEach { mode ->
                            Surface(
                                onClick = {
                                    currentViewport = mode
                                    HapticUtil.selectionTick(context)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentViewport == mode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = currentViewport == mode,
                                        onClick = { currentViewport = mode }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mode.title,
                                        fontWeight = if (currentViewport == mode) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Desktop User-Agent", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Gửi header Chrome Desktop tới CDN & script", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isDesktopUa,
                                onCheckedChange = {
                                    isDesktopUa = it
                                    HapticUtil.selectionTick(context)
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showViewportDialog = false }) {
                        Text("Xong")
                    }
                }
            )
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

        // Native JS Prompt Dialog
        if (jsPromptData != null) {
            AlertDialog(
                onDismissRequest = {
                    jsPromptData?.result?.cancel()
                    jsPromptData = null
                },
                title = { Text("JavaScript Prompt", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(jsPromptData!!.message)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = promptInputText,
                            onValueChange = { promptInputText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        jsPromptData?.result?.confirm(promptInputText)
                        jsPromptData = null
                    }) {
                        Text("Xác nhận")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        jsPromptData?.result?.cancel()
                        jsPromptData = null
                    }) {
                        Text("Hủy")
                    }
                }
            )
        }

        // Export / Save Dialog Modal
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Xuất & Lưu Mã HTML", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Bạn muốn lưu file HTML vào bộ nhớ thiết bị hay chia sẻ trực tiếp qua ứng dụng khác?")
                },
                confirmButton = {
                    TextButton(onClick = {
                        showExportDialog = false
                        saveFileLauncher.launch(currentFileName ?: "pozix_document.html")
                    }) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lưu file .html")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showExportDialog = false
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/html"
                                putExtra(Intent.EXTRA_SUBJECT, currentFileName ?: "pozix_document.html")
                                putExtra(Intent.EXTRA_TEXT, currentCode)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ mã HTML"))
                            HapticUtil.lightTap(context)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Không thể chia sẻ: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chia sẻ")
                    }
                }
            )
        }
    }
}

/**
 * Two-Row Pro Browser Header:
 * Row 1: Close button + Smart Omnibox (Favicon, Dynamic Title, Reload/Stop with Long-Press Hard Refresh) + Export
 * Row 2: Tool Action Chips (Open file, Viewport Switcher, Dark mode, View code, Copy)
 */
@Composable
private fun BrowserHeaderPro(
    title: String,
    favicon: Bitmap?,
    isLoading: Boolean,
    isReloading: Boolean,
    refreshRotation: Float,
    viewportMode: ViewportMode,
    canvasDark: Boolean,
    activeTab: Int,
    onClose: () -> Unit,
    onReloadOrStop: () -> Unit,
    onHardRefresh: () -> Unit,
    onOpenFile: () -> Unit,
    onExportFile: () -> Unit,
    onToggleViewport: () -> Unit,
    onToggleCanvasDark: () -> Unit,
    onToggleTab: () -> Unit,
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
            // Row 1: Back + Smart Omnibox + Export
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Smart Omnibox with Favicon + Dynamic Title + Reload/Stop (Long press for Hard Refresh)
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
                        if (favicon != null) {
                            Image(
                                bitmap = favicon.asImageBitmap(),
                                contentDescription = "Favicon",
                                modifier = Modifier.size(16.dp).clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Safe Local Sandbox",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                        }
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

                        // Reload / Stop button with Long-Press for Hard Refresh
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .pointerInput(isLoading) {
                                    detectTapGestures(
                                        onTap = { onReloadOrStop() },
                                        onLongPress = {
                                            if (!isLoading) {
                                                onHardRefresh()
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dừng tải",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Tải lại (Nhấn giữ: Hard Refresh)",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp).rotate(refreshRotation)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Export / Save file button
                IconButton(onClick = onExportFile, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Lưu / Xuất file",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Pro Tool Action Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderToolChip(
                    icon = Icons.Default.FileOpen,
                    label = "Mở file",
                    isActive = false,
                    onClick = onOpenFile
                )

                HeaderToolChip(
                    icon = when (viewportMode) {
                        ViewportMode.MOBILE -> Icons.Default.PhoneAndroid
                        ViewportMode.TABLET -> Icons.Default.Tablet
                        ViewportMode.DESKTOP -> Icons.Default.DesktopWindows
                        ViewportMode.FULLSCREEN -> Icons.Default.Fullscreen
                    },
                    label = viewportMode.title.substringBefore(" "),
                    isActive = viewportMode != ViewportMode.FULLSCREEN,
                    onClick = onToggleViewport
                )

                HeaderToolChip(
                    icon = if (canvasDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                    label = if (canvasDark) "Nền Tối" else "Nền Sáng",
                    isActive = canvasDark,
                    onClick = onToggleCanvasDark
                )

                HeaderToolChip(
                    icon = if (activeTab == 0) Icons.Default.Code else Icons.Default.Visibility,
                    label = if (activeTab == 0) "Mã nguồn" else "Xem web",
                    isActive = activeTab == 1,
                    onClick = onToggleTab
                )

                HeaderToolChip(
                    icon = Icons.Default.ContentCopy,
                    label = "Sao chép",
                    isActive = false,
                    onClick = onCopy
                )
            }
        }
    }
}

@Composable
private fun HeaderToolChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(
            1.dp,
            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * In-Page Search Bar (FindInPageBar)
 */
@Composable
private fun FindInPageBar(
    query: String,
    matchIndex: Int,
    totalMatches: Int,
    onQueryChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrev: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Tìm kiếm",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Tìm trong trang...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            )
            if (query.isNotEmpty()) {
                Text(
                    text = if (totalMatches > 0) "$matchIndex/$totalMatches" else "0/0",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (totalMatches > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
            IconButton(onClick = onFindPrev, enabled = totalMatches > 0, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Trước", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onFindNext, enabled = totalMatches > 0, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Sau", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Đóng", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * CodeViewerAndEditor:
 * - Line numbers gutter
 * - Syntax highlighting via highlightCodeLine()
 * - Quick Live Edit mode via BasicTextField
 * - "Áp dụng" (Apply changes & reload browser), "Khôi phục" (Reset code), "Lưu/Chia sẻ"
 */
@Composable
private fun CodeViewerAndEditor(
    originalCode: String,
    initialCode: String,
    isDark: Boolean,
    onApply: (String) -> Unit,
    onReset: () -> Unit,
    onExport: () -> Unit
) {
    var editableCode by remember(initialCode) { mutableStateOf(initialCode) }
    var isEditMode by remember { mutableStateOf(false) }

    val hasChanges = editableCode != initialCode
    val canReset = editableCode != originalCode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF1E1E2E) else Color(0xFFF8FAFC))
    ) {
        // Toolbar for Code Viewer / Editor
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Switch between Syntax view and Edit mode
                Surface(
                    onClick = { isEditMode = !isEditMode },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isEditMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(1.dp, if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (isEditMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEditMode) "Chế độ xem cú pháp" else "Sửa nhanh mã HTML",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isEditMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Apply button
                Surface(
                    onClick = { onApply(editableCode) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasChanges) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(1.dp, if (hasChanges) Color(0xFF059669) else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (hasChanges) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Áp dụng",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasChanges) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Reset button
                if (canReset) {
                    Surface(
                        onClick = {
                            editableCode = originalCode
                            onReset()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Khôi phục gốc",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Export / Share
                Surface(
                    onClick = onExport,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lưu / Chia sẻ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Code Content Area
        val vScroll = rememberScrollState()
        val hScroll = rememberScrollState()
        val lines = remember(editableCode) { editableCode.lines() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            if (isEditMode) {
                // Live Edit Mode
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(vScroll)
                ) {
                    // Line numbers
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 12.dp, top = 2.dp)
                    ) {
                        for (i in 1..lines.size) {
                            Text(
                                text = "$i",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 19.sp,
                                color = if (isDark) Color(0xFF565F89) else Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Editable TextField
                    Box(modifier = Modifier.weight(1f).horizontalScroll(hScroll)) {
                        BasicTextField(
                            value = editableCode,
                            onValueChange = { editableCode = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 19.sp,
                                color = if (isDark) Color(0xFFC0CAF5) else Color(0xFF0F172A)
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // Read-only Syntax Highlighting View
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(vScroll)
                        .horizontalScroll(hScroll)
                ) {
                    // Line numbers gutter
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        lines.forEachIndexed { idx, _ ->
                            Text(
                                text = "${idx + 1}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 19.sp,
                                color = if (isDark) Color(0xFF565F89) else Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Highlighted code lines
                    Column {
                        lines.forEach { line ->
                            Text(
                                text = highlightCodeLine(line, "HTML", true, isDark),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 19.sp,
                                color = if (isDark) Color(0xFFC0CAF5) else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * DevTools Console Drawer with All, Info, Warn, Error filter chips and counts
 */
@Composable
private fun ConsoleDrawer(
    logs: List<ConsoleLogItem>,
    filterLevel: ConsoleMessage.MessageLevel?,
    errorCount: Int,
    warnCount: Int,
    infoCount: Int,
    onFilterChange: (ConsoleMessage.MessageLevel?) -> Unit,
    onClear: () -> Unit,
    onCopyAll: () -> Unit,
    onClose: () -> Unit
) {
    val filteredLogs = remember(logs.size, filterLevel) {
        when (filterLevel) {
            null -> logs
            ConsoleMessage.MessageLevel.ERROR -> logs.filter { it.level == ConsoleMessage.MessageLevel.ERROR }
            ConsoleMessage.MessageLevel.WARNING -> logs.filter { it.level == ConsoleMessage.MessageLevel.WARNING }
            else -> logs.filter { it.level != ConsoleMessage.MessageLevel.ERROR && it.level != ConsoleMessage.MessageLevel.WARNING }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(230.dp),
        color = Color(0xFF1E1E2E),
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
                        text = "DevTools Console (${logs.size})",
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

            // Filter Chips Bar (All, Info, Warn, Error)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E2E))
                    .horizontalScroll(rememberScrollState())
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
                if (infoCount > 0) {
                    ConsoleFilterChip(
                        text = "Thông tin ($infoCount)",
                        isSelected = filterLevel == ConsoleMessage.MessageLevel.LOG,
                        color = Color(0xFFA6E3A1),
                        onClick = { onFilterChange(ConsoleMessage.MessageLevel.LOG) }
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
                        text = if (logs.isEmpty()) "Chưa có log console. Hãy chạy JavaScript để xem log!" else "Không có log nào khớp bộ lọc",
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

/**
 * Bottom Browser Status Bar:
 * - Back & Forward buttons (hooked to canGoBack / canGoForward)
 * - In-Page Search button
 * - Zoom controls
 * - Viewport badge button
 * - DevTools Console button with error badges
 */
@Composable
private fun BrowserStatusBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    viewportMode: ViewportMode,
    consoleCount: Int,
    errorCount: Int,
    showConsole: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onToggleFind: () -> Unit,
    onToggleViewport: () -> Unit,
    onToggleConsole: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Navigation buttons & Zoom
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onBack, enabled = canGoBack, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onForward, enabled = canGoForward, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Tiếp theo",
                            tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Find in page button
                    IconButton(onClick = onToggleFind, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Tìm trong trang",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Zoom Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        IconButton(onClick = onZoomOut, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Thu nhỏ", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        }
                        IconButton(onClick = onZoomIn, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Phóng to", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Right Side: Viewport & Console Drawer toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Viewport Switcher Chip
                    Surface(
                        onClick = onToggleViewport,
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = when (viewportMode) {
                                    ViewportMode.MOBILE -> Icons.Default.PhoneAndroid
                                    ViewportMode.TABLET -> Icons.Default.Tablet
                                    ViewportMode.DESKTOP -> Icons.Default.DesktopWindows
                                    ViewportMode.FULLSCREEN -> Icons.Default.Fullscreen
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = viewportMode.title.substringBefore(" "),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Console Toggle Button
                    Surface(
                        onClick = onToggleConsole,
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            showConsole -> MaterialTheme.colorScheme.primaryContainer
                            errorCount > 0 -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (errorCount > 0) Icons.Default.ErrorOutline else Icons.Default.BugReport,
                                contentDescription = null,
                                tint = when {
                                    showConsole -> MaterialTheme.colorScheme.onPrimaryContainer
                                    errorCount > 0 -> MaterialTheme.colorScheme.onErrorContainer
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
                                    errorCount > 0 -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var name: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
    }
    if (name == null) {
        name = uri.path?.substringAfterLast('/')
    }
    return name
}
