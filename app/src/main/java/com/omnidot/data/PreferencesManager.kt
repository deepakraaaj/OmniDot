package com.omnidot.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omnidot.core.MatrixPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.omniDotDataStore by preferencesDataStore(name = "omnidot_preferences")

data class OmniDotPreferences(
    val preset: MatrixPreset = MatrixPreset.YEAR_PROGRESS,
    val birthDate: LocalDate = LocalDate.of(1995, 1, 1),
    val sprintStart: LocalDate = LocalDate.now(),
    val accentColor: Int = 0xFFE65100.toInt(),
    val automaticUpdates: Boolean = true,
    val verticalOffset: Float = 0f,
    val matrixScale: Float = 1f,
)

class PreferencesManager(private val context: Context) {
    private object Keys {
        val preset = stringPreferencesKey("matrix_preset")
        val birthDate = longPreferencesKey("birth_date_epoch_day")
        val sprintStart = longPreferencesKey("sprint_start_epoch_day")
        val accent = intPreferencesKey("accent_argb")
        val automaticUpdates = booleanPreferencesKey("automatic_updates")
        val verticalOffset = floatPreferencesKey("vertical_offset")
        val matrixScale = floatPreferencesKey("matrix_scale")
    }

    val preferences: Flow<OmniDotPreferences> = context.omniDotDataStore.data.map { values ->
        OmniDotPreferences(
            preset = MatrixPreset.fromStored(values[Keys.preset]),
            birthDate = values[Keys.birthDate]?.let(LocalDate::ofEpochDay)
                ?: OmniDotPreferences().birthDate,
            sprintStart = values[Keys.sprintStart]?.let(LocalDate::ofEpochDay)
                ?: OmniDotPreferences().sprintStart,
            accentColor = values[Keys.accent] ?: OmniDotPreferences().accentColor,
            automaticUpdates = values[Keys.automaticUpdates] ?: true,
            verticalOffset = (values[Keys.verticalOffset] ?: 0f).coerceIn(-0.15f, 0.15f),
            matrixScale = (values[Keys.matrixScale] ?: 1f).coerceIn(0.6f, 1.6f),
        )
    }

    suspend fun current(): OmniDotPreferences = preferences.first()

    suspend fun setPreset(preset: MatrixPreset) {
        context.omniDotDataStore.edit { it[Keys.preset] = preset.name }
    }

    suspend fun setBirthDate(date: LocalDate) {
        context.omniDotDataStore.edit { it[Keys.birthDate] = date.toEpochDay() }
    }

    suspend fun setSprintStart(date: LocalDate) {
        context.omniDotDataStore.edit { it[Keys.sprintStart] = date.toEpochDay() }
    }

    suspend fun setAutomaticUpdates(enabled: Boolean) {
        context.omniDotDataStore.edit { it[Keys.automaticUpdates] = enabled }
    }

    suspend fun setVerticalOffset(offset: Float) {
        context.omniDotDataStore.edit {
            it[Keys.verticalOffset] = offset.coerceIn(-0.15f, 0.15f)
        }
    }

    suspend fun setMatrixScale(scale: Float) {
        context.omniDotDataStore.edit {
            it[Keys.matrixScale] = scale.coerceIn(0.6f, 1.6f)
        }
    }

    suspend fun setAccentColor(color: Int) {
        context.omniDotDataStore.edit { it[Keys.accent] = color }
    }
}

