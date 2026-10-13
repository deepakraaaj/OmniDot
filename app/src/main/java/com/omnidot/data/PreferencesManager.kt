package com.omnidot.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omnidot.automation.RemoteTriggerGuard
import com.omnidot.automation.VoiceStyle
import com.omnidot.core.DotShape
import com.omnidot.core.LabelPosition
import com.omnidot.core.LockPeriod
import com.omnidot.core.MatrixPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.omniDotDataStore by preferencesDataStore(
    name = "omnidot_preferences",
    produceMigrations = { listOf(LegacyPrefsMigration) },
)

data class OmniDotPreferences(
    val preset: MatrixPreset = MatrixPreset.GOALS,
    val birthDate: LocalDate = LocalDate.of(1995, 1, 1),
    /** Colour of past dots on the time matrices (also what the "cycle accent" action changes). */
    val accentColor: Int = 0xFFFFFFFF.toInt(),
    val todayColor: Int = 0xFFFF6B3D.toInt(),
    val futureColor: Int = 0xFF3A3A3A.toInt(),
    val dotShape: DotShape = DotShape.CIRCLE,
    val labelPosition: LabelPosition = LabelPosition.BELOW,
    /** Gap between dots as a fraction of the dot size. */
    val dotSpacing: Float = .55f,
    val automaticUpdates: Boolean = true,
    val verticalOffset: Float = 0f,
    val matrixScale: Float = 1f,
    val lockPeriod: LockPeriod = LockPeriod.THIS_MONTH,
    val wallpaperEnabled: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderConfigured: Boolean = false,
    val reminderMinuteOfDay: Int = 21 * 60,
    val voiceStyle: VoiceStyle = VoiceStyle.CALM,
    val reminderLastNotifiedDay: Long? = null,
)

class PreferencesManager(private val context: Context) {
    private object Keys {
        val preset = stringPreferencesKey("matrix_preset")
        val birthDate = longPreferencesKey("birth_date_epoch_day")
        val accent = intPreferencesKey("accent_argb")
        val automaticUpdates = booleanPreferencesKey("automatic_updates")
        val verticalOffset = floatPreferencesKey("vertical_offset")
        val matrixScale = floatPreferencesKey("matrix_scale")
        val lockPeriod = stringPreferencesKey("lock_period")
        val wallpaperEnabled = booleanPreferencesKey("wallpaper_enabled")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderMinute = intPreferencesKey("reminder_minute_of_day")
        val voiceStyle = stringPreferencesKey("voice_style")
        val remoteKey = stringPreferencesKey("remote_trigger_key")
        val reminderLastNotified = longPreferencesKey("reminder_last_notified_day")
        val todayColor = intPreferencesKey("today_argb")
        val futureColor = intPreferencesKey("future_argb")
        val dotShape = stringPreferencesKey("dot_shape")
        val labelPosition = stringPreferencesKey("label_position")
        val dotSpacing = floatPreferencesKey("dot_spacing")
    }

    val preferences: Flow<OmniDotPreferences> = context.omniDotDataStore.data.map { values ->
        val defaults = OmniDotPreferences()
        OmniDotPreferences(
            preset = MatrixPreset.fromStored(values[Keys.preset]),
            birthDate = values[Keys.birthDate]?.let(LocalDate::ofEpochDay) ?: defaults.birthDate,
            accentColor = values[Keys.accent] ?: defaults.accentColor,
            todayColor = values[Keys.todayColor] ?: defaults.todayColor,
            futureColor = values[Keys.futureColor] ?: defaults.futureColor,
            dotShape = DotShape.fromStored(values[Keys.dotShape]),
            labelPosition = LabelPosition.fromStored(values[Keys.labelPosition]),
            dotSpacing = (values[Keys.dotSpacing] ?: defaults.dotSpacing).coerceIn(.1f, 1.2f),
            automaticUpdates = values[Keys.automaticUpdates] ?: true,
            verticalOffset = (values[Keys.verticalOffset] ?: 0f).coerceIn(-0.15f, 0.15f),
            matrixScale = (values[Keys.matrixScale] ?: 1f).coerceIn(0.6f, 1.6f),
            lockPeriod = LockPeriod.fromStored(values[Keys.lockPeriod]),
            wallpaperEnabled = values[Keys.wallpaperEnabled] ?: false,
            reminderEnabled = values[Keys.reminderEnabled] ?: false,
            reminderConfigured = values[Keys.reminderEnabled] != null,
            reminderMinuteOfDay = (values[Keys.reminderMinute] ?: defaults.reminderMinuteOfDay).coerceIn(0, 1439),
            voiceStyle = VoiceStyle.fromStored(values[Keys.voiceStyle]),
            reminderLastNotifiedDay = values[Keys.reminderLastNotified],
        )
    }

    suspend fun current(): OmniDotPreferences = preferences.first()

    suspend fun setPreset(preset: MatrixPreset) = edit { it[Keys.preset] = preset.name }
    suspend fun setBirthDate(date: LocalDate) = edit { it[Keys.birthDate] = date.toEpochDay() }
    suspend fun setAutomaticUpdates(enabled: Boolean) = edit { it[Keys.automaticUpdates] = enabled }
    suspend fun setVerticalOffset(offset: Float) = edit { it[Keys.verticalOffset] = offset.coerceIn(-0.15f, 0.15f) }
    suspend fun setMatrixScale(scale: Float) = edit { it[Keys.matrixScale] = scale.coerceIn(0.6f, 1.6f) }
    suspend fun setAccentColor(color: Int) = edit { it[Keys.accent] = color }
    suspend fun setTodayColor(color: Int) = edit { it[Keys.todayColor] = color }
    suspend fun setFutureColor(color: Int) = edit { it[Keys.futureColor] = color }
    suspend fun setDotShape(shape: DotShape) = edit { it[Keys.dotShape] = shape.name }
    suspend fun setLabelPosition(position: LabelPosition) = edit { it[Keys.labelPosition] = position.name }
    suspend fun setDotSpacing(spacing: Float) = edit { it[Keys.dotSpacing] = spacing.coerceIn(.1f, 1.2f) }
    suspend fun setLockPeriod(period: LockPeriod) = edit { it[Keys.lockPeriod] = period.name }
    suspend fun setWallpaperEnabled(enabled: Boolean) = edit { it[Keys.wallpaperEnabled] = enabled }
    suspend fun setReminder(enabled: Boolean) = edit { it[Keys.reminderEnabled] = enabled }
    suspend fun setReminderMinuteOfDay(minute: Int) = edit { it[Keys.reminderMinute] = minute.coerceIn(0, 1439) }
    suspend fun setVoiceStyle(style: VoiceStyle) = edit { it[Keys.voiceStyle] = style.name }
    suspend fun setReminderLastNotified(day: LocalDate) = edit { it[Keys.reminderLastNotified] = day.toEpochDay() }

    suspend fun remoteKey(): String {
        var key = ""
        context.omniDotDataStore.edit { values ->
            key = values[Keys.remoteKey] ?: RemoteTriggerGuard.newKey().also { values[Keys.remoteKey] = it }
        }
        return key
    }

    suspend fun regenerateRemoteKey(): String {
        val key = RemoteTriggerGuard.newKey()
        edit { it[Keys.remoteKey] = key }
        return key
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.omniDotDataStore.edit { block(it) }
    }
}
