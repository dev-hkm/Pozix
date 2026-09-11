package com.hkm.pozix.ui.components.richcontent

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.collection.LruCache
import androidx.compose.ui.viewinterop.AndroidView

private val katexHeightCache = LruCache<String, Dp>(300)

/**
 * Renders complex LaTeX mathematical display blocks using offline bundled KaTeX.
 * Supports fractions, roots, integrals, limits, matrices, systems of equations, and chemical equations.
 * Adapts dynamically to Material 3 light/dark theme colors with zero layout flickering.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KaTeXMathView(
    latex: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSizeSp: Float = 17f,
    displayMode: Boolean = true
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val hexColor = remember(textColor) {
        String.format("#%06X", 0xFFFFFF and textColor.toArgb())
    }

    val cachedHeight = remember(latex) { katexHeightCache[latex] }
    var contentHeightDp by remember(latex) { mutableStateOf(cachedHeight ?: if (displayMode) 52.dp else 40.dp) }
    var hasError by remember(latex) { mutableStateOf(false) }

    if (hasError) {
        // Fallback to high-performance Unicode math parser if WebView fails
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = if (displayMode) Alignment.Center else Alignment.CenterStart
        ) {
            Text(
                text = LatexMathParser.parseToAnnotatedString(latex),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSizeSp.sp),
                color = textColor
            )
        }
        return
    }

    val htmlData = remember(latex, hexColor, fontSizeSp, displayMode) {
        buildKaTeXHtml(latex, hexColor, fontSizeSp, displayMode)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                isNestedScrollingEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER
                setOnTouchListener { v, _ ->
                    // Disallow parent from intercepting touch if the math expression overflows horizontally
                    if (v.canScrollHorizontally(1) || v.canScrollHorizontally(-1)) {
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    false
                }
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = false
                    allowFileAccess = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    @Suppress("DEPRECATION")
                    setRenderPriority(WebSettings.RenderPriority.HIGH)
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onHeightCalculated(heightPx: Float) {
                        post {
                            if (heightPx > 0) {
                                val calculatedDp = heightPx.dp.coerceIn(36.dp, 500.dp)
                                contentHeightDp = calculatedDp
                                katexHeightCache.put(latex, calculatedDp)
                            }
                        }
                    }

                    @JavascriptInterface
                    fun onRenderError() {
                        post { hasError = true }
                    }
                }, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                        hasError = true
                    }
                }

                loadDataWithBaseURL("file:///android_asset/katex/", htmlData, "text/html", "UTF-8", null)
                tag = htmlData
            }
        },
        update = { webView ->
            if (webView.tag != htmlData) {
                webView.tag = htmlData
                webView.loadDataWithBaseURL("file:///android_asset/katex/", htmlData, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(contentHeightDp)
            .padding(vertical = 2.dp)
    )
}

private fun buildKaTeXHtml(
    latex: String,
    hexColor: String,
    fontSize: Float,
    displayMode: Boolean
): String {
    // Escape special characters for JavaScript string
    val escapedLatex = latex
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", " ")
        .replace("\r", "")

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <link rel="stylesheet" href="file:///android_asset/katex/katex.min.css">
            <script src="file:///android_asset/katex/katex.min.js"></script>
            <style>
                html, body {
                    background: transparent;
                    width: 100%;
                    margin: 0;
                    padding: 0;
                }
                body {
                    color: $hexColor;
                    font-size: ${fontSize}px;
                    display: flex;
                    align-items: center;
                    justify-content: flex-start;
                    overflow-x: auto;
                    overflow-y: hidden;
                    -webkit-overflow-scrolling: touch;
                    padding: 4px 8px;
                    box-sizing: border-box;
                }
                #math-output {
                    margin: ${if (displayMode) "0 auto" else "0"};
                    flex-shrink: 0;
                    display: inline-block;
                    padding: 2px 4px;
                    overflow: visible !important;
                }
                .katex-display {
                    margin: 0 !important;
                    overflow: visible !important;
                }
                .katex, .katex-html {
                    overflow: visible !important;
                    padding-top: 4px !important;
                    padding-bottom: 8px !important;
                }
                .base, .strut, .mop, .msupsub, .vlist-t, .vlist-r, .vlist {
                    overflow: visible !important;
                }
            </style>
        </head>
        <body>
            <div id="math-output"></div>
            <script>
                (function() {
                    try {
                        var target = document.getElementById("math-output");
                        if (!target) return;
                        katex.render("$escapedLatex", target, {
                            displayMode: $displayMode,
                            throwOnError: false
                        });
                        
                        function reportHeight() {
                            var target = document.getElementById("math-output");
                            if (!target) return;
                            
                            var targetRect = target.getBoundingClientRect();
                            var minTop = targetRect.top;
                            var maxBottom = targetRect.bottom;
                            
                            var all = target.querySelectorAll("*");
                            for (var i = 0; i < all.length; i++) {
                                var r = all[i].getBoundingClientRect();
                                if (r.height > 0 || r.width > 0) {
                                    if (r.top < minTop) minTop = r.top;
                                    if (r.bottom > maxBottom) maxBottom = r.bottom;
                                }
                            }
                            
                            var renderedH = Math.ceil(maxBottom - minTop);
                            var targetH = target ? Math.ceil(target.scrollHeight) : 0;
                            var contentH = Math.max(renderedH, targetH);
                            // +14px buffer accommodates 4px top + 8px bottom padding on .katex-html plus baseline
                            var finalH = Math.max(contentH + 14, 38);
                            if (window.AndroidBridge && window.AndroidBridge.onHeightCalculated) {
                                window.AndroidBridge.onHeightCalculated(finalH);
                            }
                        }
                        
                        reportHeight();
                        if (document.fonts && document.fonts.ready) {
                            document.fonts.ready.then(reportHeight);
                        }
                        setTimeout(reportHeight, 50);
                        setTimeout(reportHeight, 150);
                        setTimeout(reportHeight, 300);
                    } catch (e) {
                        if (window.AndroidBridge && window.AndroidBridge.onRenderError) {
                            window.AndroidBridge.onRenderError();
                        }
                    }
                })();
            </script>
        </body>
        </html>
    """.trimIndent()
}
