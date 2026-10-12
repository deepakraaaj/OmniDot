package com.omnidot.goals

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.goalDataStore by preferencesDataStore(name = "omnidot_goals")

class GoalRepository(private val context: Context) {
    private val key = stringPreferencesKey("goals_json")

    val goals: Flow<List<Goal>> = context.goalDataStore.data.map { GoalCodec.decode(it[key].orEmpty()) }

    suspend fun current(): List<Goal> = goals.first()
    suspend fun save(goal: Goal) = update { GoalListOps.upsert(it, goal) }
    suspend fun delete(id: String) = update { GoalListOps.remove(it, id) }
    suspend fun toggle(id: String, date: LocalDate, today: LocalDate = LocalDate.now()) =
        update { GoalListOps.toggle(it, id, date, today) }
    suspend fun setCheckIn(id: String, date: LocalDate, done: Boolean, today: LocalDate = LocalDate.now()) =
        update { GoalListOps.setCheckIn(it, id, date, done, today) }

    private suspend fun update(transform: (List<Goal>) -> List<Goal>) {
        context.goalDataStore.edit { it[key] = GoalCodec.encode(transform(GoalCodec.decode(it[key].orEmpty()))) }
    }
}
