package com.omnidot.automation

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.sin

object SoundSynthesizer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Plays an energetic rising power-up arpeggio with bright harmonic overtones.
     * C5 (523Hz) -> E5 (659Hz) -> G5 (784Hz) -> C6 (1046Hz)
     */
    fun playPowerConnectedChime() {
        scope.launch {
            val sampleRate = 44100
            val notes = listOf(
                Note(523.25, 0.06), // C5
                Note(659.25, 0.06), // E5
                Note(783.99, 0.06), // G5
                Note(1046.50, 0.18), // C6 (sustained finale)
            )
            val audioData = generateArpeggio(sampleRate, notes, isEnergetic = true)
            playPcm(sampleRate, audioData)
        }
    }

    /**
     * Plays a comical/dramatic descending power-down sound.
     * D5 (587Hz) -> A4 (440Hz) -> D4 (293Hz)
     */
    fun playPowerDisconnectedChime() {
        scope.launch {
            val sampleRate = 44100
            val notes = listOf(
                Note(587.33, 0.08), // D5
                Note(440.00, 0.08), // A4
                Note(293.66, 0.20), // D4
            )
            val audioData = generateArpeggio(sampleRate, notes, isEnergetic = false)
            playPcm(sampleRate, audioData)
        }
    }

    private data class Note(val freq: Double, val durationSec: Double)

    private fun generateArpeggio(sampleRate: Int, notes: List<Note>, isEnergetic: Boolean): ShortArray {
        val totalSamples = notes.sumOf { (it.durationSec * sampleRate).toInt() }
        val output = ShortArray(totalSamples)
        var cursor = 0

        for (note in notes) {
            val noteSamples = (note.durationSec * sampleRate).toInt()
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / noteSamples

                // Envelope: quick attack to prevent click, then natural exponential decay
                val attack = if (i < 220) i.toDouble() / 220.0 else 1.0
                val decay = exp(-progress * (if (isEnergetic) 2.2 else 3.5))
                val envelope = attack * decay

                // Primary sine wave + subtle bright 2nd harmonic
                val fundamental = sin(2.0 * Math.PI * note.freq * t)
                val harmonic = if (isEnergetic) 0.3 * sin(2.0 * Math.PI * (note.freq * 2.0) * t) else 0.15 * sin(2.0 * Math.PI * (note.freq * 1.5) * t)
                val sampleValue = (fundamental + harmonic) * envelope * 0.75

                val clamped = (sampleValue * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                output[cursor++] = clamped.toShort()
            }
        }
        return output
    }

    private fun playPcm(sampleRate: Int, audioData: ShortArray) {
        var track: AudioTrack? = null
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val bufferSize = audioData.size * 2
            track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(audioData, 0, audioData.size)
            track.play()

            val playDurationMs = ((audioData.size.toDouble() / sampleRate) * 1000).toLong() + 50
            Thread.sleep(playDurationMs)
        } catch (_: Throwable) {
            // AudioTrack playback failure fallback
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Throwable) {}
        }
    }
}
