package com.hkm.pozix.data.cloud

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object BackupCrypto {
    private const val ITERATIONS = 100_000
    private const val KEY_BITS = 256
    private const val IV_BYTES = 12
    private const val SALT_BYTES = 16
    private val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    fun createToken(): String = buildString {
        append("hkm-")
        repeat(36) { append(alphabet[SecureRandom().nextInt(alphabet.length)]) }
    }

    fun encrypt(plainText: String, password: String, existingSalt: String? = null): EncryptedBackup {
        require(password.isNotBlank()) { "Password is required" }
        val salt = existingSalt?.let(::decode) ?: ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val iv = ByteArray(IV_BYTES).also(SecureRandom()::nextBytes)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        }
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return EncryptedBackup(
            verifier = encode(MessageDigest.getInstance("SHA-256").digest(key)),
            salt = encode(salt),
            ciphertext = encode(iv + encrypted)
        )
    }

    fun decrypt(backup: EncryptedBackup, password: String): String {
        val salt = decode(backup.salt)
        val key = deriveKey(password, salt)
        require(MessageDigest.isEqual(decode(backup.verifier), MessageDigest.getInstance("SHA-256").digest(key))) { "Incorrect password" }
        val packed = decode(backup.ciphertext)
        require(packed.size > IV_BYTES) { "Invalid encrypted backup" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, packed.copyOfRange(0, IV_BYTES)))
        }
        return cipher.doFinal(packed.copyOfRange(IV_BYTES, packed.size)).toString(Charsets.UTF_8)
    }

    fun verifierFor(password: String, saltEncoded: String): String =
        encode(MessageDigest.getInstance("SHA-256").digest(deriveKey(password, decode(saltEncoded))))

    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val passwordBytes = password.toByteArray(Charsets.UTF_8)
        return try {
            val mac = Mac.getInstance("HmacSHA256").apply {
                init(SecretKeySpec(passwordBytes, "HmacSHA256"))
            }
            val blockInput = ByteArray(salt.size + 4)
            salt.copyInto(blockInput)
            blockInput[blockInput.lastIndex] = 1

            var u = mac.doFinal(blockInput)
            val derived = u.copyOf()
            repeat(ITERATIONS - 1) {
                u = mac.doFinal(u)
                for (index in derived.indices) {
                    derived[index] = (derived[index].toInt() xor u[index].toInt()).toByte()
                }
            }
            derived.copyOf(KEY_BITS / 8)
        } finally {
            passwordBytes.fill(0)
        }
    }

    private fun encode(value: ByteArray): String =
        Base64.UrlSafe.encode(value).trimEnd('=')

    private fun decode(value: String): ByteArray {
        val padding = (4 - value.length % 4) % 4
        return Base64.UrlSafe.decode(value + "=".repeat(padding))
    }
}
