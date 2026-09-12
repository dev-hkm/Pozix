package com.hkm.pozix.ui.components.richcontent

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.hkm.pozix.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val blockCache = object : android.util.LruCache<String, List<ContentBlock>>(512_000) {
    override fun sizeOf(key: String, value: List<ContentBlock>) = key.length.coerceAtLeast(1)
}

internal fun cachedContentBlocks(text: String): List<ContentBlock> =
    blockCache.get(text) ?: parseContentBlocks(text).also { blockCache.put(text, it) }

internal data class ChatRenderEntry(
    val key: String, val message: ChatMessage, val section: String,
    val streaming: Boolean, val block: ContentBlock? = null
)

@Composable
internal fun rememberChatEntries(messages: List<ChatMessage>, loading: Boolean): List<ChatRenderEntry> {
    val entries by produceState(emptyList<ChatRenderEntry>(), messages, loading) {
        value = withContext(Dispatchers.Default) {
            buildList {
                messages.forEachIndexed { index, message ->
                    val key = "${message.timestamp}_${message.role}_$index"
                    val streaming = loading && index == messages.lastIndex && message.role == "model"
                    if (message.role == "user") {
                        add(ChatRenderEntry(key, message, "all", false))
                    } else {
                        add(ChatRenderEntry("$key/header", message, "header", streaming))
                        cachedContentBlocks(message.text).forEachIndexed { blockIndex, block ->
                            add(ChatRenderEntry("$key/block/$blockIndex", message, "block", streaming, block))
                        }
                        add(ChatRenderEntry("$key/footer", message, "footer", streaming))
                    }
                }
            }
        }
    }
    return entries
}

@Composable
fun StreamingRichContentText(text: String, streaming: Boolean, textColor: Color, style: TextStyle, modifier: Modifier) {
    val blocks by produceState(emptyList<ContentBlock>(), text) {
        value = withContext(Dispatchers.Default) { cachedContentBlocks(text) }
    }
    RichContentText(text, modifier, textColor = textColor, style = style, blocksOverride = blocks)
}
