package com.omnidot.automation

object LinkDetector {
    private val link = Regex(
        """(https?://[^\s]+)|(www\.[^\s]+)|(\b[a-z0-9-]+\.(?:com|in|io|org|net|co|me|ly|gl|app|dev|link|us|to)(?:/[^\s]*)?)""",
        RegexOption.IGNORE_CASE,
    )

    /** The first web link in [text], without trailing punctuation, or null. */
    fun firstLink(text: String): String? =
        link.find(text)?.value?.trimEnd('.', ',', ')', '!', '?', ';', ':', '"', '\'')
}
