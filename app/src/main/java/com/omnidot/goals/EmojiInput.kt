package com.omnidot.goals

object EmojiInput {
    private const val MAX_UTF16 = 8
    private const val ZWJ = 0x200D

    /** Trims to a short icon without cutting a surrogate pair or leaving a dangling joiner. */
    fun sanitize(input: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < input.length) {
            val cp = input.codePointAt(i)
            val count = Character.charCount(cp)
            if (out.length + count > MAX_UTF16) break
            out.appendCodePoint(cp)
            i += count
        }
        while (out.isNotEmpty() && out.codePointBefore(out.length) == ZWJ) out.setLength(out.length - 1)
        return out.toString()
    }
}
