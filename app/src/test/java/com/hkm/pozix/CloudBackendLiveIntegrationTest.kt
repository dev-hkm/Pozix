package com.hkm.pozix

import com.hkm.pozix.data.cloud.BackupCrypto
import com.hkm.pozix.data.cloud.CloudBackupApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class CloudBackendLiveIntegrationTest {
    @Test
    fun androidCryptoAndTransportRoundTripAgainstProduction() = runBlocking {
        assumeTrue(System.getenv("POZIX_LIVE_TEST") == "1")

        val api = CloudBackupApi()
        val token = BackupCrypto.createToken()
        val password = "Pozix-live-${System.nanoTime()}"
        val firstBackup = """{"version":2,"quizSets":[],"progress":[]}"""
        val encrypted = BackupCrypto.encrypt(firstBackup, password)

        api.upload(token, encrypted, replace = false).getOrThrow()
        val remoteSalt = api.getSalt(token).getOrThrow()
        assertEquals(encrypted.salt, remoteSalt)

        val verifier = BackupCrypto.verifierFor(password, remoteSalt)
        val restored = api.restore(token, verifier).getOrThrow()
        assertEquals(firstBackup, BackupCrypto.decrypt(restored, password))

        val wrongPasswordPayload = BackupCrypto.encrypt(
            plainText = """{"version":2,"tampered":true}""",
            password = "$password-wrong",
            existingSalt = remoteSalt
        )
        val rejectedUpdate = api.upload(token, wrongPasswordPayload, replace = true)
        assertTrue(rejectedUpdate.isFailure)
        val preservedAfterRejectedUpdate = api.restore(token, verifier).getOrThrow()
        assertEquals(firstBackup, BackupCrypto.decrypt(preservedAfterRejectedUpdate, password))

        val updatedBackup = """{"version":2,"quizSets":[],"progress":[],"currentQuizSetId":"live"}"""
        val replacement = BackupCrypto.encrypt(updatedBackup, password, remoteSalt)
        api.upload(token, replacement, replace = true).getOrThrow()
        val restoredUpdate = api.restore(token, verifier).getOrThrow()
        assertEquals(updatedBackup, BackupCrypto.decrypt(restoredUpdate, password))

        val quizJson = """
            {
              "title": "Pozix Android live integration",
              "questions": [
                {
                  "type": "true_false",
                  "question": "The Android client reached Cloudflare successfully",
                  "correctAnswer": true
                }
              ]
            }
        """.trimIndent()
        val shareUrl = api.createShare(quizJson).getOrThrow()
        assertTrue(shareUrl.startsWith(CloudBackupApi.BASE_URL))
        assertEquals(quizJson, api.loadShare(shareUrl).getOrThrow())
    }
}
