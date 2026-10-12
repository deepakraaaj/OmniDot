package com.omnidot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omnidot.automation.AutomationEngine
import com.omnidot.automation.AutomationRepository
import com.omnidot.automation.AutomationRule
import com.omnidot.core.MatrixSyncWorker
import com.omnidot.data.OmniDotPreferences
import com.omnidot.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OmniDotViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)
    private val automationRepository = AutomationRepository(application)
    val preferences: StateFlow<OmniDotPreferences> = preferencesManager.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        OmniDotPreferences(),
    )
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

    fun saveAutomation(rule: AutomationRule) = viewModelScope.launch {
        automationRepository.save(rule)
    }

    fun setAutomationEnabled(id: String, enabled: Boolean) = viewModelScope.launch {
        val rule = automations.value.firstOrNull { it.id == id } ?: return@launch
        if (enabled && !rule.trigger.available) return@launch
        automationRepository.setEnabled(id, enabled)
    }

    fun restoreAutomation(rule: AutomationRule) = saveAutomation(rule)

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
