package com.hkm.pozix.data.cloud

data class EncryptedBackup(
    val verifier: String,
    val salt: String,
    val ciphertext: String
)
