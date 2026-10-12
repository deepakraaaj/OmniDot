package com.omnidot.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyPrefsMigrationTest {
    private val preset = stringPreferencesKey("matrix_preset")
    private val wallpaper = booleanPreferencesKey("wallpaper_enabled")
    private val accent = intPreferencesKey("accent_argb")

    @Test
    fun `upgrading install keeps year view and midnight refresh`() = runBlocking {
        val old = mutablePreferencesOf(accent to 0xFFE65100.toInt())
        assertTrue(LegacyPrefsMigration.shouldMigrate(old))
        val migrated = LegacyPrefsMigration.migrate(old)
        assertEquals("YEAR_PROGRESS", migrated[preset])
        assertEquals(true, migrated[wallpaper])
        assertFalse(LegacyPrefsMigration.shouldMigrate(migrated))
    }

    @Test
    fun `upgrading install keeps an explicitly chosen preset`() = runBlocking {
        val migrated = LegacyPrefsMigration.migrate(mutablePreferencesOf(preset to "LIFE_IN_WEEKS"))
        assertEquals("LIFE_IN_WEEKS", migrated[preset])
        assertEquals(true, migrated[wallpaper])
    }

    @Test
    fun `fresh install is only stamped`() = runBlocking {
        val fresh = mutablePreferencesOf()
        assertTrue(LegacyPrefsMigration.shouldMigrate(fresh))
        val migrated = LegacyPrefsMigration.migrate(fresh)
        assertNull(migrated[preset])
        assertNull(migrated[wallpaper])
        assertFalse(LegacyPrefsMigration.shouldMigrate(migrated))
    }
}
