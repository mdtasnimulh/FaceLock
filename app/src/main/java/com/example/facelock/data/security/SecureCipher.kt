package com.example.facelock.data.security

/** Authenticated encryption of small blobs. [context] is bound to the ciphertext (associated data). */
interface SecureCipher {
    fun encrypt(plain: ByteArray, context: ByteArray): ByteArray
    fun decrypt(encrypted: ByteArray, context: ByteArray): ByteArray
}
