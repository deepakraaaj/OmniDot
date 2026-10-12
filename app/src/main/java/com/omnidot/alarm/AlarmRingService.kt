package com.omnidot.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.omnidot.R
import com.omnidot.automation.BellTone
import com.omnidot.automation.VoiceFeedbackManager
import com.omnidot.automation.VoiceStyle
import com.omnidot.goals.GoalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

/** Rings a calm alarm: a looping soft bell on the alarm stream that rises slowly until dismissed. */
class AlarmRingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var track: AudioTrack? = null
    private var ramp: Job? = null
    private var alarmId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SNOOZE -> finish { alarm -> AlarmScheduler.ringAt(this, alarm.id, ZonedDateTime.now().plusMinutes(alarm.snoozeMinutes.toLong())) }
            ACTION_DISMISS -> finish { alarm -> onWokeUp(alarm) }
            else -> start(intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID))
        }
        return START_NOT_STICKY
    }

    private fun start(id: String?) {
        if (id == null) {
            stopSelf()
            return
        }
        alarmId = id
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification(id),
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        _ringing.value = id
        startSound()
    }

    private fun startSound() {
        stopSound()
        val sampleRate = 44_100
        val bell = BellTone.generate(sampleRate)
        val pcm = bell + ShortArray((0.6 * sampleRate).toInt())
        val audio = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            )
            .setAudioFormat(
                AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        audio.write(pcm, 0, pcm.size)
        audio.setLoopPoints(0, pcm.size, -1)
        audio.setVolume(RisingBell.volumeAt(0))
        audio.play()
        track = audio
        ramp = scope.launch {
            var seconds = 0
            while (isActive) {
                delay(1_000)
                seconds++
                track?.setVolume(RisingBell.volumeAt(seconds))
                if (RisingBell.shouldVibrate(seconds) && seconds % 4 == 0) vibrate()
            }
        }
    }

    private fun stopSound() {
        ramp?.cancel()
        ramp = null
        runCatching { track?.stop() }
        runCatching { track?.release() }
        track = null
    }

    private fun finish(then: suspend (CalmAlarm) -> Unit) {
        stopSound()
        _ringing.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        val id = alarmId
        scope.launch {
            try {
                id?.let { AlarmRepository(this@AlarmRingService).find(it) }?.let { then(it) }
            } finally {
                stopSelf()
            }
        }
    }

    private suspend fun onWokeUp(alarm: CalmAlarm) {
        val repository = AlarmRepository(this)
        if (alarm.days.isEmpty()) {
            repository.save(alarm.copy(enabled = false))
            AlarmScheduler.cancel(this, alarm.id)
        } else {
            AlarmScheduler.scheduleNext(this, alarm)
        }
        if (alarm.speakGreeting) {
            val text = MorningGreeting.text(GoalRepository(this).current(), LocalDate.now())
            val calm = VoiceStyle.CALM
            VoiceFeedbackManager.speak(this, text, calm.pitch, calm.rate, calm.volume, waitForCompletion = true)
        }
    }

    private fun vibrate() {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1)
        if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(effect)
        }
    }

    private fun notification(id: String): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Calm alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Gentle wake-up alarms"
                setSound(null, null)
            },
        )
        val fullScreen = PendingIntent.getActivity(
            this, 0, Intent(this, AlarmActivity::class.java).putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Good morning")
            .setContentText("Your calm alarm is ringing")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, "Snooze", serviceAction(ACTION_SNOOZE, 1))
            .addAction(0, "I'm up", serviceAction(ACTION_DISMISS, 2))
            .build()
    }

    private fun serviceAction(action: String, code: Int) = PendingIntent.getService(
        this, code, Intent(this, AlarmRingService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    override fun onDestroy() {
        stopSound()
        _ringing.value = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.omnidot.alarm.START"
        const val ACTION_SNOOZE = "com.omnidot.alarm.SNOOZE"
        const val ACTION_DISMISS = "com.omnidot.alarm.DISMISS"
        private const val CHANNEL_ID = "calm_alarm"
        private const val NOTIFICATION_ID = 4301

        private val _ringing = MutableStateFlow<String?>(null)
        /** Id of the alarm currently ringing, or null. */
        val ringing = _ringing.asStateFlow()

        fun send(context: Context, action: String) {
            context.startService(Intent(context, AlarmRingService::class.java).setAction(action))
        }
    }
}
