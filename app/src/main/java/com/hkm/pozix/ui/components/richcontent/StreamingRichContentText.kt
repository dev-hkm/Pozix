package com.hkm.pozix.ui.components.richcontent

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.hkm.pozix.util.StreamingMarkdown

/** Only complete paragraphs may instantiate syntax highlighting or a math WebView. */
@Composable
fun StreamingRichContentText(text: String, streaming: Boolean, textColor: Color, style: TextStyle, modifier: Modifier) {
    if (!streaming) {
        RichContentText(text, modifier, textColor = textColor, style = style)
        return
    }
    val split = remember(text) { StreamingMarkdown.split(text) }
    Column(modifier) {
        if (split.first.isNotBlank()) RichContentText(split.first, textColor = textColor, style = style)
        if (split.second.isNotEmpty()) Text(split.second, color = textColor, style = style)
    }
}
