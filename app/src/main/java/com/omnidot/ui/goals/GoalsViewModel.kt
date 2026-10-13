package com.omnidot.ui.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omnidot.core.DotShape
import com.omnidot.core.LabelPosition
import com.omnidot.core.LockPeriod
import com.omnidot.core.MatrixPreset
import com.omnidot.core.MatrixSyncWorker
import com.omnidot.core.WallpaperRefresher
import com.omnidot.data.OmniDotPreferences
import com.omnidot.data.PreferencesManager
import com.omnidot.goals.Goal
import com.omnidot.goals.GoalRepository
import com.omnidot.goals.reminder.CheckInReminderWorker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface ApplyStatus {
    data object Idle : ApplyStatus
    data object Applying : ApplyStatus
    data object Applied : ApplyStatus
    data class Error(val message: String) : ApplyStatus
}

class GoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GoalRepository(application)
    private val prefs = PreferencesManager(application)

    val goals: StateFlow<List<Goal>> = repository.goals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val preferences: StateFlow<OmniDotPreferences> = prefs.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OmniDotPreferences())
    private val _applyStatus = MutableStateFlow<ApplyStatus>(ApplyStatus.Idle)
    val applyStatus = _applyStatus.asStateFlow()
    private val _firstGoalCreated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val firstGoalCreated = _firstGoalCreated.asSharedFlow()

    fun save(goal: Goal) = viewModelScope.launch {
        val wasEmpty = repository.current().none { !it.archived }
        repository.save(goal)
        val settings = prefs.current()
        if (wasEmpty && !settings.reminderConfigured) {
            prefs.setReminder(true)
            CheckInReminderWorker.schedule(getApplication(), settings.reminderMinuteOfDay)
            _firstGoalCreated.tryEmit(Unit)
        }
        refresh()
    }

    fun delete(goal: Goal) = viewModelScope.launch { repository.delete(goal.id); refresh() }
    fun restore(goal: Goal) = viewModelScope.launch { repository.save(goal); refresh() }
    fun archive(goal: Goal) = viewModelScope.launch { repository.save(goal.copy(archived = true)); refresh() }
    fun toggle(goalId: String, date: LocalDate) = viewModelScope.launch { repository.toggle(goalId, date); refresh() }

    fun setPreset(preset: MatrixPreset) = edit { prefs.setPreset(preset) }
    fun setPeriod(period: LockPeriod) = edit { prefs.setLockPeriod(period) }
    fun setBirthDate(date: LocalDate) = edit { prefs.setBirthDate(date) }
    fun setOffset(offset: Float) = edit { prefs.setVerticalOffset(offset) }
    fun setScale(scale: Float) = edit { prefs.setMatrixScale(scale) }
    fun setDotShape(shape: DotShape) = edit { prefs.setDotShape(shape) }
    fun setLabelPosition(position: LabelPosition) = edit { prefs.setLabelPosition(position) }
    fun setDotSpacing(spacing: Float) = edit { prefs.setDotSpacing(spacing) }
    fun setPastColor(color: Int) = edit { prefs.setAccentColor(color) }
    fun setTodayColor(color: Int) = edit { prefs.setTodayColor(color) }
    fun setFutureColor(color: Int) = edit { prefs.setFutureColor(color) }
    fun setAutomaticUpdates(enabled: Boolean) = viewModelScope.launch {
        prefs.setAutomaticUpdates(enabled)
        if (enabled) MatrixSyncWorker.scheduleNext(getApplication()) else MatrixSyncWorker.cancel(getApplication())
    }

    fun apply() = viewModelScope.launch {
        _applyStatus.value = ApplyStatus.Applying
        _applyStatus.value = try {
            WallpaperRefresher.apply(getApplication())
            if (prefs.current().automaticUpdates) MatrixSyncWorker.scheduleNext(getApplication())
            ApplyStatus.Applied
        } catch (error: Exception) {
            ApplyStatus.Error(error.message ?: "Could not set the lock screen wallpaper")
        }
    }

    private fun edit(block: suspend () -> Unit) = viewModelScope.launch { block(); _applyStatus.value = ApplyStatus.Idle }

    private suspend fun refresh() { runCatching { WallpaperRefresher.refreshIfEnabled(getApplication()) } }
}
