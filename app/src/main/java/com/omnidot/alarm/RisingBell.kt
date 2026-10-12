package com.omnidot.alarm

/** How a calm alarm grows: very quiet at first, full volume after 90 s, vibration after 3 min. */
object RisingBell {
    private const val START = 0.05f
    private const val RAMP_SECONDS = 90f
    private const val VIBRATE_AFTER_SECONDS = 180

    fun volumeAt(secondsElapsed: Int): Float =
        (START + (1f - START) * (secondsElapsed / RAMP_SECONDS)).coerceIn(START, 1f)

    fun shouldVibrate(secondsElapsed: Int): Boolean = secondsElapsed >= VIBRATE_AFTER_SECONDS
}
