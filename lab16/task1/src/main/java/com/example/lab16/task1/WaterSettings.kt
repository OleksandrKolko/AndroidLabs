package com.example.lab16.task1

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Єдиний (Singleton) DataStore на весь застосунок
val Context.waterDataStore: DataStore<Preferences> by preferencesDataStore(name = "water_settings")

/**
 * Сховище налаштувань трекера води (Preferences DataStore).
 * Ним користуються і Worker (збільшує лічильник), і ViewModel (показує його в UI).
 */
class WaterSettings(private val context: Context) {

    private object Keys {
        val SENT_COUNT = intPreferencesKey("sent_count")
        val ENABLED = booleanPreferencesKey("enabled")
        val INTERVAL = longPreferencesKey("interval_minutes")
    }

    val sentCount: Flow<Int> = context.waterDataStore.data.map { it[Keys.SENT_COUNT] ?: 0 }
    val enabled: Flow<Boolean> = context.waterDataStore.data.map { it[Keys.ENABLED] ?: false }
    val interval: Flow<Long> = context.waterDataStore.data.map { it[Keys.INTERVAL] ?: 15L }

    suspend fun incrementSentCount(): Int {
        var newValue = 0
        context.waterDataStore.edit { prefs ->
            newValue = (prefs[Keys.SENT_COUNT] ?: 0) + 1
            prefs[Keys.SENT_COUNT] = newValue
        }
        return newValue
    }

    suspend fun resetSentCount() {
        context.waterDataStore.edit { it[Keys.SENT_COUNT] = 0 }
    }

    suspend fun setEnabled(value: Boolean, intervalMinutes: Long) {
        context.waterDataStore.edit {
            it[Keys.ENABLED] = value
            it[Keys.INTERVAL] = intervalMinutes
        }
    }
}
