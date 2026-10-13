package com.omnidot.automation

object LinkDetector {
    private val link = Regex(
        """(https?://[^\s]+)|(www\.[^\s]+)|(\b[a-z0-9-]+\.(?:com|in|io|org|net|co|me|ly|gl|app|dev|link|us|to|be|tv|ai|gg|xyz|info|edu|gov|uk|fm|so|sh|page|site|news)(?:/[^\s]*)?)""",
        RegexOption.IGNORE_CASE,
    )

    // WhatsApp notifications hide the URL behind a preview: "🔗 <page title> (<domain>)".
    private val whatsAppPreview = Regex("""🔗.*\(([a-z0-9-]+(?:\.[a-z0-9-]+)+)\)\s*$""", RegexOption.IGNORE_CASE)

    /** The first web link in [text] (or the domain of a WhatsApp link preview), without trailing punctuation, or null. */
    fun firstLink(text: String): String? =
        whatsAppPreview.find(text.trim())?.groupValues?.get(1)
            ?: link.find(text)?.value?.trimEnd('.', ',', ')', '!', '?', ';', ':', '"', '\'')
}
