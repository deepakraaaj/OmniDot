package com.omnidot.ui.alarms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omnidot.alarm.AlarmRepository
import com.omnidot.alarm.AlarmScheduler
import com.omnidot.alarm.CalmAlarm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AlarmRepository(application)

    val alarms: StateFlow<List<CalmAlarm>> = repository.alarms.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(alarm: CalmAlarm) = viewModelScope.launch {
        repository.save(alarm)
        AlarmScheduler.scheduleNext(getApplication(), alarm)
    }

    fun setEnabled(alarm: CalmAlarm, enabled: Boolean) = save(alarm.copy(enabled = enabled))

    fun delete(alarm: CalmAlarm) = viewModelScope.launch {
        AlarmScheduler.cancel(getApplication(), alarm.id)
        repository.delete(alarm.id)
    }
}
