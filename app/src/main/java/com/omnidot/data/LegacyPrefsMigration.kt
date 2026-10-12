package com.omnidot.data

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Upgrades preferences written by versions before Goals: those installs had no
 * wallpaper opt-in flag (the midnight worker always refreshed) and defaulted to Year in dots.
 */
object LegacyPrefsMigration : DataMigration<Preferences> {
    private const val VERSION = 2
    private val schemaVersion = intPreferencesKey("schema_version")
    private val preset = stringPreferencesKey("matrix_preset")
    private val wallpaperEnabled = booleanPreferencesKey("wallpaper_enabled")
    private val legacyKeys = listOf(
        preset,
        intPreferencesKey("accent_argb"),
        booleanPreferencesKey("automatic_updates"),
        longPreferencesKey("birth_date_epoch_day"),
        longPreferencesKey("sprint_start_epoch_day"),
        floatPreferencesKey("vertical_offset"),
        floatPreferencesKey("matrix_scale"),
    )

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        (currentData[schemaVersion] ?: 0) < VERSION

    override suspend fun migrate(currentData: Preferences): Preferences {
        val values = currentData.toMutablePreferences()
        if (legacyKeys.any { currentData.contains(it) } && currentData[wallpaperEnabled] == null) {
            if (currentData[preset] == null) values[preset] = "YEAR_PROGRESS"
            values[wallpaperEnabled] = true
        }
        values[schemaVersion] = VERSION
        return values.toPreferences()
    }

    override suspend fun cleanUp() = Unit
}
