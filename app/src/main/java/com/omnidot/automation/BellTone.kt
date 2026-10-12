package com.omnidot.automation

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** A low, slowly fading singing-bowl style tone (inharmonic partials, long exponential decay). */
object BellTone {
    const val DURATION_SEC = 2.4
    private const val FUNDAMENTAL = 392.0 // G4
    private val partials = listOf(1.0 to 1.0, 2.0 to 0.35, 2.76 to 0.15)
    private const val PEAK = 0.5

    fun generate(sampleRate: Int = 44_100): ShortArray {
        val total = (DURATION_SEC * sampleRate).toInt()
        val attackSamples = (0.015 * sampleRate).toInt()
        val norm = partials.sumOf { it.second }
        return ShortArray(total) { i ->
            val t = i.toDouble() / sampleRate
            val attack = if (i < attackSamples) i.toDouble() / attackSamples else 1.0
            val envelope = attack * exp(-t * 2.2)
            val wave = partials.sumOf { (ratio, amp) -> amp * sin(2 * PI * FUNDAMENTAL * ratio * t) } / norm
            (wave * envelope * PEAK * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
