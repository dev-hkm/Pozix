package com.hkm.pozix

import com.hkm.pozix.data.cloud.BackupCrypto
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun cloudTokenAndCiphertextRoundTrip() {
        val token = BackupCrypto.createToken()
        assertTrue(token.matches(Regex("hkm-[A-Za-z0-9]{36}")))
        val encrypted = BackupCrypto.encrypt("{\"version\":1}", "correct horse battery staple")
        assertEquals("{\"version\":1}", BackupCrypto.decrypt(encrypted, "correct horse battery staple"))
    }

    @Test
    fun cloudBackupRejectsWrongPasswordAndCanReuseSalt() {
        val first = BackupCrypto.encrypt("first", "password123")
        assertThrows(Exception::class.java) { BackupCrypto.decrypt(first, "wrong-password") }
        val replacement = BackupCrypto.encrypt("second", "password123", first.salt)
        assertEquals(first.verifier, replacement.verifier)
        assertEquals("second", BackupCrypto.decrypt(replacement, "password123"))
    }

    @Test
    fun compressedBackupRoundTripKeepsLegacyFormatReadable() {
        val value = "x".repeat(20_000)
        val encrypted = BackupCrypto.encrypt(value, "password123")
        assertEquals(value, BackupCrypto.decrypt(encrypted, "password123"))
    }

    @Test
    fun portablePbkdf2MatchesThePlatformImplementation() {
        val password = "portable-password"
        val salt = "pozix-fixed-salt".toByteArray()
        val saltEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(salt)
        val platformKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password.toCharArray(), salt, 100_000, 256))
            .encoded
        val expectedVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(platformKey)
        )

        assertEquals(expectedVerifier, BackupCrypto.verifierFor(password, saltEncoded))
    }
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}
