package com.hkm.pozix

import com.hkm.pozix.data.cloud.CloudBackupApi
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudBackupApiThreadingTest {
    @Test
    fun networkWorkRunsOffTheCallingThread() {
        val callerDispatcher = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "pozix-ui-test")
        }.asCoroutineDispatcher()
        val networkDispatcher = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "pozix-network-test")
        }.asCoroutineDispatcher()
        val observedThread = AtomicReference<String>()

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                observedThread.set(Thread.currentThread().name)
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"salt":"test-salt"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        try {
            val api = CloudBackupApi(
                baseUrl = "https://pozix.test",
                client = client,
                ioDispatcher = networkDispatcher
            )

            val salt = runBlocking(callerDispatcher) {
                api.getSalt("hkm-${"a".repeat(36)}").getOrThrow()
            }

            assertEquals("test-salt", salt)
            assertTrue(observedThread.get().startsWith("pozix-network-test"))
            assertFalse(observedThread.get().startsWith("pozix-ui-test"))
        } finally {
            callerDispatcher.close()
            networkDispatcher.close()
        }
    }
}
