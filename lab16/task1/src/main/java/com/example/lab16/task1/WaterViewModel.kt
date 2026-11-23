package com.example.lab16.task1

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.lab16.task1.WaterReminderWorker.Companion.TAG
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)
    private val settings = WaterSettings(application)

    // Дані з DataStore → StateFlow для Compose (оновлюються, щойно Worker змінить лічильник)
    val sentCount: StateFlow<Int> =
        settings.sentCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val enabled: StateFlow<Boolean> =
        settings.enabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val interval: StateFlow<Long> =
        settings.interval.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15L)

    fun startReminder(intervalMinutes: Long) {
        // Системне обмеження: періодична задача не частіше ніж раз на 15 хвилин
        val safeInterval = if (intervalMinutes < 15) 15L else intervalMinutes

        val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            safeInterval, TimeUnit.MINUTES
        ).addTag(WORK_NAME).build()

        // UPDATE: якщо задача вже є — оновлюємо її інтервал, а не створюємо дублікат
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        viewModelScope.launch { settings.setEnabled(true, safeInterval) }
        Log.d(TAG, "startReminder: кожні $safeInterval хв")
    }

    fun stopReminder() {
        workManager.cancelUniqueWork(WORK_NAME)
        viewModelScope.launch { settings.setEnabled(false, interval.value) }
        Log.d(TAG, "stopReminder: задачу скасовано")
    }

    /** Для зручної перевірки: одноразовий запуск того самого Worker'а прямо зараз */
    fun testNow() {
        workManager.enqueue(OneTimeWorkRequestBuilder<WaterReminderWorker>().build())
        Log.d(TAG, "testNow: одноразовий запуск")
    }

    fun resetCounter() {
        viewModelScope.launch { settings.resetSentCount() }
    }

    companion object {
        const val WORK_NAME = "water_reminder"
    }
}
