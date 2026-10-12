package com.omnidot.automation

enum class VoiceStyle(val label: String, val pitch: Float, val rate: Float, val volume: Float) {
    CALM("Calm", pitch = 1.0f, rate = 0.9f, volume = 0.8f),
    NEUTRAL("Neutral", pitch = 1.0f, rate = 1.0f, volume = 0.9f),
    CHEERFUL("Cheerful", pitch = 1.15f, rate = 1.05f, volume = 1.0f);

    companion object {
        fun fromStored(value: String?): VoiceStyle =
            entries.firstOrNull { it.name == value } ?: CALM
    }
}
