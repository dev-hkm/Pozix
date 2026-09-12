package com.hkm.pozix

import com.hkm.pozix.data.model.AiProvider
import com.hkm.pozix.network.OpenAiCompatClient
import com.hkm.pozix.util.StreamPresentation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.*
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class AiStreamingTest {
    @Test fun documentContentsRemainInFollowUpRequest() = runBlocking {
        var requestBody = ""
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val buffer = okio.Buffer()
            chain.request().body!!.writeTo(buffer)
            requestBody = buffer.readUtf8()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("data: {\"choices\":[{\"delta\":{\"content\":\"OK\"},\"finish_reason\":\"stop\"}]}\n\n".toResponseBody("text/event-stream".toMediaType())).build()
        }.build()
        val attachment = com.hkm.pozix.data.model.ChatAttachment(name = "lesson.docx",
            type = com.hkm.pozix.data.model.AttachmentType.DOCUMENT, localPath = "unused", textContent = "UN was founded in 1945")
        OpenAiCompatClient.chatCompletionStream("https://test.example/v1", "", "test", listOf(
            com.hkm.pozix.data.model.ChatMessage(role = "user", text = "Read this", attachments = listOf(attachment)),
            com.hkm.pozix.data.model.ChatMessage(role = "model", text = "Read"),
            com.hkm.pozix.data.model.ChatMessage(role = "user", text = "Explain more")), httpClient = client).toList()
        assertTrue(requestBody.contains("UN was founded in 1945"))
        assertTrue(requestBody.contains("Explain more"))
    }

    @Test fun finalEventWithoutBlankLineIsNotLost() = runBlocking {
        val events = "data: {\"choices\":[{\"delta\":{\"content\":\"Last answer\"},\"finish_reason\":\"stop\"}]}"
        val chunks = OpenAiCompatClient.chatCompletionStream("https://test.example/v1", "", "test",
            emptyList(), httpClient = clientFor(events)).toList()
        assertEquals("Last answer", chunks.joinToString("") { it.content })
    }

    @Test fun outputLimitPreservesTerminalDelta() = runBlocking {
        val received = StringBuilder()
        try {
            OpenAiCompatClient.chatCompletionStream("https://test.example/v1", "", "test", emptyList(),
                httpClient = clientFor("data: {\"choices\":[{\"delta\":{\"content\":\"Keep me\"},\"finish_reason\":\"length\"}]}\n\n"))
                .collect { received.append(it.content) }
            fail("Expected output limit")
        } catch (_: java.io.IOException) {
            assertEquals("Keep me", received.toString())
        }
    }

    private fun clientFor(events: String) = OkHttpClient.Builder().addInterceptor { chain ->
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
            .body(events.toResponseBody("text/event-stream".toMediaType())).build()
    }.build()

    @Test fun preservesReasoningTextAndMultilineEvents() = runBlocking {
        val events = ": heartbeat\n\ndata: {\"choices\":[\ndata: {\"delta\":{\"reasoning_content\":\"Think\"}}]}\n\n" +
            "data: {\"choices\":[{\"delta\":{\"content\":\"Xin chào 😀\"}}]}\n\n" +
            "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n"
        val chunks = OpenAiCompatClient.chatCompletionStream("https://test.example/v1", "", "test",
            emptyList(), httpClient = clientFor(events)).toList()
        assertEquals("Think", chunks.joinToString("") { it.reasoning })
        assertEquals("Xin chào 😀", chunks.joinToString("") { it.content })
    }

    @Test fun streamErrorsAreNotSilentlyAcceptedAsEmptyAnswers() = runBlocking {
        try {
            OpenAiCompatClient.chatCompletionStream("https://test.example/v1", "", "test", emptyList(),
                httpClient = clientFor("data: {\"error\":{\"message\":\"provider failed\"}}\n\n")).toList()
            fail("Expected a stream error")
        } catch (e: java.io.IOException) {
            assertEquals("provider failed", e.message)
        }
    }
    @Test fun splitThinkingTagsNeverFlashIntoTheAnswer() {
        assertEquals("" to "", StreamPresentation.splitThinking("<thi"))
        assertEquals("" to "reason", StreamPresentation.splitThinking("<think>reason</thi"))
        assertEquals("Answer" to "reason", StreamPresentation.splitThinking("<think>reason</think>Answer"))
        assertEquals("normal text" to "", StreamPresentation.splitThinking("normal text"))
        assertEquals("x <" to "", StreamPresentation.splitThinking("x <", complete = true))
    }
    @Test fun endpointSuffixIsRemovedForSavedProviders() {
        val provider = AiProvider(name = "Test", baseUrl = "https://test.example/v1/chat/completions/",
            apiKey = "", modelId = "test")
        assertEquals("https://test.example/v1", provider.normalizedBaseUrl())
    }

    @Test fun presentationDrainsWithoutDumpingOrSplittingUnicode() {
        val text = "Hello 😀 world ".repeat(100)
        var index = 0
        var frames = 0
        while (index < text.length) {
            val next = StreamPresentation.nextRevealIndex(text, index)
            assertTrue(next > index)
            assertFalse(text[next - 1].isHighSurrogate())
            index = next
            frames++
        }
        assertTrue(frames > 1)
        assertTrue(frames < 80)
        assertEquals(text.length, index)
    }

    @Test fun firstDeltaArrivesBeforeServerFinishesAndStopClosesSocket() = runBlocking {
        val server = ServerSocket(0)
        val socketClosed = CountDownLatch(1)
        val first = CompletableDeferred<String>()
        val worker = thread(isDaemon = true) {
            server.accept().use { socket ->
                socket.soTimeout = 3000
                val reader = socket.getInputStream().bufferedReader()
                var length = 0
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                    if (line.startsWith("Content-Length:", true)) length = line.substringAfter(':').trim().toInt()
                }
                repeat(length) { reader.read() }
                val output = socket.getOutputStream()
                output.write(("HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nConnection: close\r\n\r\n" +
                    "data: {\"choices\":[{\"delta\":{\"content\":\"First\"}}]}\n\n").toByteArray())
                output.flush()
                if (reader.read() == -1) socketClosed.countDown()
            }
        }
        try {
            val job = launch {
                OpenAiCompatClient.chatCompletionStream("http://127.0.0.1:${server.localPort}/v1",
                    "", "test", emptyList(), httpClient = OkHttpClient())
                    .collect { first.complete(it.content) }
            }
            assertEquals("First", withTimeout(2000) { first.await() })
            withTimeout(2000) { job.cancelAndJoin() }
            assertTrue(socketClosed.await(2, TimeUnit.SECONDS))
        } finally {
            server.close()
            worker.join(3500)
        }
    }
}
