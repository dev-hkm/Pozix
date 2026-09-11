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
import androidx.compose.ui.viewinterop.AndroidView

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

    var contentHeightDp by remember { mutableStateOf(48.dp) }
    var hasError by remember { mutableStateOf(false) }

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
                isHorizontalScrollBarEnabled = true
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onHeightCalculated(heightPx: Float) {
                        post {
                            if (heightPx > 0) {
                                // CSS pixels in mobile viewport match DP units. Add 14dp padding for fraction descenders/roots.
                                val calculatedDp = (heightPx + 14f).dp.coerceIn(36.dp, 600.dp)
                                contentHeightDp = calculatedDp
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
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                html, body {
                    background: transparent;
                    width: 100%;
                }
                body {
                    color: $hexColor;
                    font-size: ${fontSize}px;
                    display: flex;
                    align-items: center;
                    justify-content: ${if (displayMode) "center" else "flex-start"};
                    overflow-x: auto;
                    overflow-y: visible;
                    padding: 8px 6px 12px 6px;
                }
                #math-output {
                    display: inline-block;
                    padding: 2px 2px;
                }
                .katex-display {
                    margin: 2px 0 !important;
                }
                .katex {
                    line-height: 1.35 !important;
                }
            </style>
        </head>
        <body>
            <div id="math-output"></div>
            <script>
                document.addEventListener("DOMContentLoaded", function() {
                    try {
                        var target = document.getElementById("math-output");
                        katex.render("$escapedLatex", target, {
                            displayMode: $displayMode,
                            throwOnError: false
                        });
                        
                        function reportHeight() {
                            var rect = target.getBoundingClientRect();
                            var h = Math.ceil(Math.max(rect.height, target.offsetHeight, document.body.scrollHeight, document.documentElement.scrollHeight));
                            if (window.AndroidBridge && window.AndroidBridge.onHeightCalculated) {
                                window.AndroidBridge.onHeightCalculated(h);
                            }
                        }
                        
                        reportHeight();
                        setTimeout(reportHeight, 60);
                        setTimeout(reportHeight, 250);
                    } catch (e) {
                        if (window.AndroidBridge && window.AndroidBridge.onRenderError) {
                            window.AndroidBridge.onRenderError();
                        }
                    }
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}
