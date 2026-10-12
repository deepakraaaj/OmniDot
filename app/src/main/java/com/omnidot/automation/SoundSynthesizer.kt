package com.omnidot.automation

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object SoundSynthesizer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun playSoftBell() {
        scope.launch { playPcm(44_100, BellTone.generate(44_100)) }
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
