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
import com.omnidot.automation.VoiceStyle
import com.omnidot.core.LockPeriod
import com.omnidot.core.MatrixPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.omniDotDataStore by preferencesDataStore(name = "omnidot_preferences")

data class OmniDotPreferences(
    val preset: MatrixPreset = MatrixPreset.GOALS,
    val birthDate: LocalDate = LocalDate.of(1995, 1, 1),
    val accentColor: Int = 0xFF5FB3A6.toInt(),
    val automaticUpdates: Boolean = true,
    val verticalOffset: Float = 0f,
    val matrixScale: Float = 1f,
    val lockPeriod: LockPeriod = LockPeriod.THIS_MONTH,
    val wallpaperEnabled: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderConfigured: Boolean = false,
    val reminderMinuteOfDay: Int = 21 * 60,
    val voiceStyle: VoiceStyle = VoiceStyle.CALM,
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
    }

    val preferences: Flow<OmniDotPreferences> = context.omniDotDataStore.data.map { values ->
        val defaults = OmniDotPreferences()
        OmniDotPreferences(
            preset = MatrixPreset.fromStored(values[Keys.preset]),
            birthDate = values[Keys.birthDate]?.let(LocalDate::ofEpochDay) ?: defaults.birthDate,
            accentColor = values[Keys.accent] ?: defaults.accentColor,
            automaticUpdates = values[Keys.automaticUpdates] ?: true,
            verticalOffset = (values[Keys.verticalOffset] ?: 0f).coerceIn(-0.15f, 0.15f),
            matrixScale = (values[Keys.matrixScale] ?: 1f).coerceIn(0.6f, 1.6f),
            lockPeriod = LockPeriod.fromStored(values[Keys.lockPeriod]),
            wallpaperEnabled = values[Keys.wallpaperEnabled] ?: false,
            reminderEnabled = values[Keys.reminderEnabled] ?: false,
            reminderConfigured = values[Keys.reminderEnabled] != null,
            reminderMinuteOfDay = (values[Keys.reminderMinute] ?: defaults.reminderMinuteOfDay).coerceIn(0, 1439),
            voiceStyle = VoiceStyle.fromStored(values[Keys.voiceStyle]),
        )
    }

    suspend fun current(): OmniDotPreferences = preferences.first()

    suspend fun setPreset(preset: MatrixPreset) = edit { it[Keys.preset] = preset.name }
    suspend fun setBirthDate(date: LocalDate) = edit { it[Keys.birthDate] = date.toEpochDay() }
    suspend fun setAutomaticUpdates(enabled: Boolean) = edit { it[Keys.automaticUpdates] = enabled }
    suspend fun setVerticalOffset(offset: Float) = edit { it[Keys.verticalOffset] = offset.coerceIn(-0.15f, 0.15f) }
    suspend fun setMatrixScale(scale: Float) = edit { it[Keys.matrixScale] = scale.coerceIn(0.6f, 1.6f) }
    suspend fun setAccentColor(color: Int) = edit { it[Keys.accent] = color }
    suspend fun setLockPeriod(period: LockPeriod) = edit { it[Keys.lockPeriod] = period.name }
    suspend fun setWallpaperEnabled(enabled: Boolean) = edit { it[Keys.wallpaperEnabled] = enabled }
    suspend fun setReminder(enabled: Boolean) = edit { it[Keys.reminderEnabled] = enabled }
    suspend fun setReminderMinuteOfDay(minute: Int) = edit { it[Keys.reminderMinute] = minute.coerceIn(0, 1439) }
    suspend fun setVoiceStyle(style: VoiceStyle) = edit { it[Keys.voiceStyle] = style.name }

    suspend fun remoteKey(): String = context.omniDotDataStore.data.first()[Keys.remoteKey].orEmpty()

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.omniDotDataStore.edit { block(it) }
    }
}
