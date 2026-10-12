package com.omnidot.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteTriggerGuardTest {
    @Test
    fun `keys are 24 url-safe characters and differ`() {
        val a = RemoteTriggerGuard.newKey()
        assertEquals(24, a.length)
        assertTrue(a.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertNotEquals(a, RemoteTriggerGuard.newKey())
    }

    @Test
    fun `authorization requires an exact non-blank match`() {
        assertTrue(RemoteTriggerGuard.isAuthorized("abc123", "abc123"))
        assertFalse(RemoteTriggerGuard.isAuthorized("abc124", "abc123"))
        assertFalse(RemoteTriggerGuard.isAuthorized(null, "abc123"))
        assertFalse(RemoteTriggerGuard.isAuthorized("", ""))
    }
}
