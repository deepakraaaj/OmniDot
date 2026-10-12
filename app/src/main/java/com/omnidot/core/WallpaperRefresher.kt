package com.omnidot.core

import android.content.Context
import com.omnidot.data.OmniDotPreferences
import com.omnidot.data.PreferencesManager
import com.omnidot.goals.GoalRepository

object WallpaperRefresher {
    suspend fun apply(context: Context, settings: OmniDotPreferences? = null) {
        val preferences = PreferencesManager(context)
        val resolved = settings ?: preferences.current()
        AdaptiveMatrixEngine().applyToLockscreen(context, resolved, GoalRepository(context).current())
        preferences.setWallpaperEnabled(true)
    }

    suspend fun refreshIfEnabled(context: Context): Boolean {
        val settings = PreferencesManager(context).current()
        if (!settings.wallpaperEnabled) return false
        apply(context, settings)
        return true
    }
}
