package com.example.lab18.task1

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.lab18.task1.ExchangeRatesSyncWorker.Companion.KEY_MESSAGE
import com.example.lab18.task1.data.ExchangeDatabase
import com.example.lab18.task1.data.ExchangeRateEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Стан синхронізації обраної дати (окремо від списку з Room) */
sealed interface SyncStatus {
    data object None : SyncStatus
    data object Waiting : SyncStatus
    data object Running : SyncStatus
    data class Retrying(val attempt: Int) : SyncStatus
    data class Done(val message: String, val isEmptyResponse: Boolean) : SyncStatus
    data class Failed(val message: String) : SyncStatus
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = ExchangeDatabase.getDatabase(application).exchangeRateDao()
    private val workManager = WorkManager.getInstance(application)

    // Обрана дата живе у ViewModel → переживає поворот екрана
    private val _selectedDate = MutableStateFlow(DateUtils.today())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    /** Список ЗАВЖДИ з Room. null — ще читаємо базу */
    val rates: StateFlow<List<ExchangeRateEntity>?> = _selectedDate
        .flatMapLatest { date -> dao.observeRates(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** ⭐ Збережені дати */
    val savedDates: StateFlow<List<String>> = dao.observeSavedDates()
        .map { dates -> dates.sortedByDescending { DateUtils.formatter().parse(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Стан одноразового оновлення саме для обраної дати */
    val syncStatus: StateFlow<SyncStatus> = _selectedDate
        .flatMapLatest { date ->
            workManager.getWorkInfosForUniqueWorkLiveData(workName(date)).asFlow()
        }
        .map { infos -> infos.firstOrNull().toSyncStatus() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncStatus.None)

    fun onDateSelected(date: String) {
        _selectedDate.value = date
    }

    /** «Оновити» лише ставить задачу у WorkManager; список оновиться через Flow з Room */
    fun refresh(): String? {
        val date = _selectedDate.value
        DateUtils.validate(date)?.let { return it }

        val request = OneTimeWorkRequestBuilder<ExchangeRatesSyncWorker>()
            .setConstraints(ExchangeApp.networkConstraints())
            .setInputData(workDataOf(ExchangeRatesSyncWorker.KEY_DATE to date))
            .build()

        // KEEP: повторні натискання не дублюють незавершений запит цієї дати
        workManager.enqueueUniqueWork(workName(date), ExistingWorkPolicy.KEEP, request)
        Log.d(ExchangeRatesSyncWorker.TAG, "enqueueUniqueWork: ${workName(date)}")
        return null
    }

    // onCleared() НЕ скасовує WorkManager: у фонової синхронізації свій життєвий цикл

    private fun workName(date: String) = "exchange_rates_$date"

    private fun WorkInfo?.toSyncStatus(): SyncStatus = when (this?.state) {
        null -> SyncStatus.None
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED ->
            if (runAttemptCount > 0) SyncStatus.Retrying(runAttemptCount) else SyncStatus.Waiting
        WorkInfo.State.RUNNING -> SyncStatus.Running
        WorkInfo.State.SUCCEEDED -> {
            val message = outputData.getString(KEY_MESSAGE) ?: "Оновлено"
            SyncStatus.Done(message, isEmptyResponse = message == "Сервер не повернув курсів")
        }
        WorkInfo.State.FAILED ->
            SyncStatus.Failed(outputData.getString(KEY_MESSAGE) ?: "Помилка оновлення")
        WorkInfo.State.CANCELLED -> SyncStatus.Failed("Оновлення скасовано")
    }
}
