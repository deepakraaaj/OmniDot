package com.omnidot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omnidot.automation.AutomationEngine
import com.omnidot.automation.AutomationRepository
import com.omnidot.automation.AutomationRule
import com.omnidot.core.AdaptiveMatrixEngine
import com.omnidot.core.MatrixPreset
import com.omnidot.core.MatrixSyncWorker
import com.omnidot.data.OmniDotPreferences
import com.omnidot.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

class OmniDotViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)
    private val automationRepository = AutomationRepository(application)
    val preferences: StateFlow<OmniDotPreferences> = preferencesManager.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        OmniDotPreferences(),
    )
    private val _applyStatus = MutableStateFlow<ApplyStatus>(ApplyStatus.Idle)
    val applyStatus = _applyStatus.asStateFlow()
    val automations = automationRepository.rules.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val _testStatus = MutableStateFlow<String?>(null)
    val testStatus = _testStatus.asStateFlow()

    init {
        viewModelScope.launch {
            automationRepository.ensureDefaultRules()
            if (preferencesManager.current().automaticUpdates) {
                MatrixSyncWorker.scheduleNext(application)
            }
        }
    }

    fun selectPreset(preset: MatrixPreset) = viewModelScope.launch {
        preferencesManager.setPreset(preset)
        _applyStatus.value = ApplyStatus.Idle
    }

    fun setBirthDate(date: LocalDate) = viewModelScope.launch {
        preferencesManager.setBirthDate(date)
    }

    fun restartSprint() = viewModelScope.launch {
        preferencesManager.setSprintStart(LocalDate.now())
    }

    fun setAutomaticUpdates(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setAutomaticUpdates(enabled)
        if (enabled) MatrixSyncWorker.scheduleNext(getApplication())
        else MatrixSyncWorker.cancel(getApplication())
    }

    fun setVerticalOffset(offset: Float) = viewModelScope.launch {
        preferencesManager.setVerticalOffset(offset)
        _applyStatus.value = ApplyStatus.Idle
    }

    fun setMatrixScale(scale: Float) = viewModelScope.launch {
        preferencesManager.setMatrixScale(scale)
        _applyStatus.value = ApplyStatus.Idle
    }

    fun setAccentColor(color: Int) = viewModelScope.launch {
        preferencesManager.setAccentColor(color)
        _applyStatus.value = ApplyStatus.Idle
    }

    fun applyWallpaper() = viewModelScope.launch {
        _applyStatus.value = ApplyStatus.Applying
        _applyStatus.value = try {
            AdaptiveMatrixEngine().applyToLockscreen(getApplication(), preferences.value)
            if (preferences.value.automaticUpdates) MatrixSyncWorker.scheduleNext(getApplication())
            ApplyStatus.Applied
        } catch (error: Exception) {
            ApplyStatus.Error(error.message ?: "Could not set the lockscreen wallpaper")
        }
    }

    fun saveAutomation(rule: AutomationRule) = viewModelScope.launch {
        automationRepository.save(rule)
    }

    fun setAutomationEnabled(id: String, enabled: Boolean) = viewModelScope.launch {
        automationRepository.setEnabled(id, enabled)
    }

    fun deleteAutomation(id: String) = viewModelScope.launch {
        automationRepository.delete(id)
    }

    fun testAutomation(rule: AutomationRule, onResult: ((String) -> Unit)? = null) = viewModelScope.launch {
        _testStatus.value = "Executing '${rule.name}'..."
        val result = AutomationEngine.execute(getApplication(), rule)
        val msg = if (result.isSuccess) {
            "✓ Success: ${result.getOrNull()}"
        } else {
            "✕ Failed: ${result.exceptionOrNull()?.message}"
        }
        _testStatus.value = msg
        automationRepository.recordRun(rule.id, msg)
        onResult?.invoke(msg)
    }

    fun clearTestStatus() {
        _testStatus.value = null
    }
}
