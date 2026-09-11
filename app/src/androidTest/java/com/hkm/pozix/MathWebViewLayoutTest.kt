package com.hkm.pozix

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.hkm.pozix.ui.components.richcontent.KaTeXMathView
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/** Exercises the real JS -> Android bridge and Compose's assigned height, not just HTML geometry. */
class MathWebViewLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findWebView(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    @Test fun screenshotFormulasFitNativeViewAndShrinkAfterReuse() {
        val latex = mutableStateOf("x")
        compose.setContent {
            MaterialTheme {
                Column(Modifier.width(320.dp)) {
                    Text("Question before formula")
                    KaTeXMathView(latex.value)
                    Text("Content immediately after formula")
                }
            }
        }
        val formulas = listOf(
            "K_w=[H^+][OH^-]=1.0\\times10^{-14}",
            "E^2=(mc^2)^2+(pc)^2",
            "\\left(\\sum_{i=1}^n a_i b_i\\right)^2\\leq\\left(\\sum_{i=1}^n a_i^2\\right)\\left(\\sum_{i=1}^n b_i^2\\right)",
            "f'(x_0)=\\lim_{h\\to0}\\frac{f(x_0+h)-f(x_0)}{h}",
            "x+1"
        )
        for (formula in formulas) {
            compose.runOnIdle { latex.value = formula }
            val result = AtomicReference<Boolean?>(null)
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.runOnUiThread {
                    val view = findWebView(compose.activity.window.decorView)
                    if (view != null && view.alpha == 1f) {
                        view.evaluateJavascript("""JSON.stringify({width:innerWidth,height:document.getElementById('math').getBoundingClientRect().height+8,text:document.querySelector('annotation')?.textContent})""") { encoded ->
                            if (encoded != "null") {
                                val values = JSONObject(JSONArray("[$encoded]").getString(0))
                                val scale = view.width / values.getDouble("width")
                                result.set(values.optString("text") == formula &&
                                    abs(view.height - values.getDouble("height") * scale) <= 3.0 * scale)
                            }
                        }
                    }
                }
                result.get() == true
            }
        }
    }
}
