package com.omnidot.automation

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object RemoteTriggerGuard {
    fun newKey(random: java.util.Random = SecureRandom()): String {
        val bytes = ByteArray(18).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun isAuthorized(provided: String?, expected: String): Boolean {
        if (provided.isNullOrBlank() || expected.isBlank()) return false
        return MessageDigest.isEqual(provided.toByteArray(), expected.toByteArray())
    }
}
