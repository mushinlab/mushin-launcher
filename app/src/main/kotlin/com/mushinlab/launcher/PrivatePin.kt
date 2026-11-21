package com.mushinlab.launcher

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal object PrivatePin {
    fun create(pin: String): Pair<String, String> {
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        return Base64.getEncoder().encodeToString(salt) to hash(pin, salt)
    }

    fun matches(pin: String, salt: String, hash: String): Boolean =
        MessageDigest.isEqual(Base64.getDecoder().decode(hash), derive(pin, Base64.getDecoder().decode(salt)))

    private fun hash(pin: String, salt: ByteArray): String =
        Base64.getEncoder().encodeToString(derive(pin, salt))

    private fun derive(pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance(ALGORITHM)
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, BITS))
            .encoded

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val BITS = 256
    private const val SALT_BYTES = 16
}
