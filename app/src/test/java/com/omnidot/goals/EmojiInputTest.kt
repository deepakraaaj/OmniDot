package com.omnidot.goals

import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiInputTest {
    @Test
    fun `never splits a surrogate pair`() {
        val nine = "🏃".repeat(9)
        assertEquals("🏃".repeat(4), EmojiInput.sanitize(nine))
    }

    @Test
    fun `drops a dangling zero width joiner`() {
        // 👨‍👩 + ZWJ + start of another person, cut at 8 UTF-16 units would leave a trailing joiner
        assertEquals("👨\u200D👩", EmojiInput.sanitize("👨\u200D👩\u200D"))
    }

    @Test
    fun `keeps short input unchanged`() {
        assertEquals("🌱", EmojiInput.sanitize("🌱"))
        assertEquals("", EmojiInput.sanitize(""))
    }
}
