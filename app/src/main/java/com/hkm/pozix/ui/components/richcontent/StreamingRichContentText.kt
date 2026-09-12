package com.hkm.pozix.ui.components.richcontent

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Lightweight presentation path for an active stream.
 *
 * The network already delivers realtime deltas. Re-parsing the complete growing
 * document and creating KaTeX WebViews on every delta makes long answers visibly
 * jank. Keep the stream as native text until EOF; the settled message then takes
 * the full Markdown/KaTeX path exactly once.
 */
@Composable
fun StreamingRichContentText(text: String, streaming: Boolean, textColor: Color, style: TextStyle, modifier: Modifier) {
    if (streaming) {
        Text(text = text, modifier = modifier, color = textColor, style = style)
    } else {
        RichContentText(text, modifier, textColor = textColor, style = style)
    }
}
