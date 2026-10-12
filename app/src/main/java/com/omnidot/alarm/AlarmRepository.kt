package com.omnidot.alarm

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.alarmDataStore by preferencesDataStore(name = "omnidot_alarms")

class AlarmRepository(private val context: Context) {
    private val key = stringPreferencesKey("alarms_json")

    val alarms: Flow<List<CalmAlarm>> = context.alarmDataStore.data.map { AlarmCodec.decode(it[key].orEmpty()) }

    suspend fun current(): List<CalmAlarm> = alarms.first()
    suspend fun find(id: String): CalmAlarm? = current().firstOrNull { it.id == id }

    suspend fun save(alarm: CalmAlarm) = update { list ->
        if (list.any { it.id == alarm.id }) list.map { if (it.id == alarm.id) alarm else it } else list + alarm
    }

    suspend fun delete(id: String) = update { list -> list.filterNot { it.id == id } }

    private suspend fun update(transform: (List<CalmAlarm>) -> List<CalmAlarm>) {
        context.alarmDataStore.edit { it[key] = AlarmCodec.encode(transform(AlarmCodec.decode(it[key].orEmpty())).sortedBy { a -> a.minuteOfDay }) }
    }
}
