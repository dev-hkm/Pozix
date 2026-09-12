package com.hkm.pozix.ui.components.richcontent

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.collection.LruCache
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.ceil

private val katexHeightCache = LruCache<String, Float>(200)

@Composable
fun KaTeXMathView(latex: String, modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSizeSp: Float = 17f, displayMode: Boolean = true) {
    val density = LocalDensity.current
    var width by remember { mutableIntStateOf(0) }
    val fontSize = fontSizeSp * density.fontScale
    // Width controls WebView reflow, but it must not reset the measured height
    // cache. Doing so caused every question transition to flash at 48.dp.
    val measurementKey = "$latex|${density.density}|$fontSize|$displayMode"
    val renderKey = "$measurementKey|width=$width"
    var height by remember(measurementKey) {
        mutableFloatStateOf(katexHeightCache[measurementKey] ?: 48f)
    }
    var failed by remember(latex) { mutableStateOf(false) }
    val color = String.format("#%06X", textColor.toArgb() and 0xFFFFFF)
    if (failed) {
        Text(LatexMathParser.parseToAnnotatedString(latex), modifier = modifier,
            color = textColor, fontSize = fontSizeSp.sp)
        return
    }
    AndroidView(factory = { MathWebView(it) }, update = { view ->
        // A reused view must never write to the previous question's Compose state.
        view.onHeight = { measured ->
            if (measured.isFinite() && measured > 0 && abs(height - measured) >= 1f) {
                height = measured
                katexHeightCache.put(measurementKey, measured)
            }
        }
        view.onError = { failed = true }
        view.render(latex, color, fontSize, displayMode, renderKey)
    }, onReset = { it.resetForReuse() }, onRelease = { it.dispose() },
        modifier = modifier.fillMaxWidth().onSizeChanged { width = it.width }.height(ceil(height).toInt().dp))
}

/** Keep bundled JS and fonts alive across expression updates instead of reloading a document. */
@SuppressLint("SetJavaScriptEnabled")
private class MathWebView(context: Context) : WebView(context) {
    var onHeight: (Float) -> Unit = {}
    var onError: () -> Unit = {}
    private var loaded = false
    private var revision = 0
    private var modelKey = ""
    private var script = ""
    private var horizontalOverflow = false
    private var downX = 0f
    private var downY = 0f
    init {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = false
        settings.blockNetworkLoads = true
        settings.allowFileAccess = true
        settings.allowContentAccess = false
        settings.textZoom = 100
        addJavascriptInterface(object {
            @JavascriptInterface fun measured(token: Int, height: Float, overflow: Boolean) {
                post { if (token == revision) { alpha = 1f; horizontalOverflow = overflow; onHeight(height) } }
            }
            @JavascriptInterface fun failed(token: Int) { post { if (token == revision) onError() } }
        }, "MathBridge")
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                loaded = true
                if (script.isNotEmpty()) evaluateJavascript(script, null)
            }
            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = true
        }
        loadDataWithBaseURL("file:///android_asset/katex/", MATH_HTML, "text/html", "UTF-8", null)
    }
    fun render(latex: String, color: String, fontSize: Float, display: Boolean, key: String) {
        val newKey = "$key|$color"
        if (modelKey == newKey) return
        modelKey = newKey
        revision++
        alpha = 0f
        script = "renderMath(${JSONObject.quote(latex)},${JSONObject.quote(color)},$fontSize,$display,$revision)"
        if (loaded) evaluateJavascript(script, null)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y }
            MotionEvent.ACTION_MOVE -> parent?.requestDisallowInterceptTouchEvent(
                horizontalOverflow && abs(event.x - downX) > abs(event.y - downY))
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        return super.onTouchEvent(event)
    }
    fun dispose() {
        revision++
        onHeight = {}; onError = {}
        stopLoading()
        removeJavascriptInterface("MathBridge")
        destroy()
    }
    fun resetForReuse() {
        revision++
        modelKey = ""
        script = ""
        onHeight = {}; onError = {}
        alpha = 0f
    }
}

private val MATH_HTML = """
<!doctype html><html><head>
<meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
<link rel="stylesheet" href="katex.min.css"><script src="katex.min.js"></script>
<style>
html,body { margin:0; padding:0; background:transparent; width:100%; }
#viewport { overflow-x:auto; overflow-y:hidden; padding:4px; box-sizing:border-box; }
#math { display:table; width:max-content; margin:0 auto; }
.katex-display { margin:0; }
</style></head><body><div id="viewport"><div id="math"></div></div>
<script>
let revision=0;
const target=document.getElementById('math'), viewport=document.getElementById('viewport');
function measure() {
  const height=Math.ceil(target.getBoundingClientRect().height)+8;
  MathBridge.measured(revision,height,viewport.scrollWidth>viewport.clientWidth+1);
}
function renderMath(latex,color,size,display,token) {
  revision=token;
  try {
    target.style.color=color; target.style.fontSize=size+'px';
    target.style.margin=display?'0 auto':'0';
    katex.render(latex,target,{displayMode:display,throwOnError:false,trust:false});
    viewport.scrollLeft=0;
    document.fonts.ready.then(()=>requestAnimationFrame(()=>{if(revision===token) measure();}));
  } catch(error) {MathBridge.failed(token);}
}
new ResizeObserver(()=>requestAnimationFrame(measure)).observe(target);
</script></body></html>
""".trimIndent()
