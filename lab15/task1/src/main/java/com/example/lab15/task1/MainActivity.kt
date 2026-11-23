package com.example.lab15.task1

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val workManager by lazy { WorkManager.getInstance(applicationContext) }

    // Статуси для Compose
    private val oneTimeStatus = MutableStateFlow("Задачу ще не запускали")
    private val periodicStatus = MutableStateFlow("Нічна синхронізація вимкнена")

    // id останньої одноразової задачі — зберігаємо при повороті екрана
    private var lastRequestId: UUID? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Після повороту екрана знову підписуємось на ту саму задачу
        savedInstanceState?.getString(KEY_LAST_ID)?.let { observeOneTimeWork(UUID.fromString(it)) }

        // Стан періодичної задачі (живе в WorkManager, а не в Activity)
        workManager.getWorkInfosForUniqueWorkLiveData(PERIODIC_WORK_NAME)
            .observe(this) { infos ->
                val info = infos.firstOrNull()
                periodicStatus.value = if (info == null) {
                    "Нічна синхронізація вимкнена"
                } else {
                    "Нічна синхронізація: ${info.state.name} (${info.state.toUkrainian()})"
                }
            }

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    SyncScreen(
                        oneTimeStatus = oneTimeStatus,
                        periodicStatus = periodicStatus,
                        onSyncNow = ::startOneTimeSync,
                        onEnableNightSync = ::enableNightSync,
                        onDisableNightSync = ::disableNightSync
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        lastRequestId?.let { outState.putString(KEY_LAST_ID, it.toString()) }
    }

    // ========== Одноразова задача ==========

    private fun startOneTimeSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)   // будь-який інтернет
            .build()

        val request = OneTimeWorkRequestBuilder<NewsSyncWorker>()
            .setConstraints(constraints)
            .setInputData(
                Data.Builder().putString(NewsSyncWorker.KEY_USER_TOKEN, "abc123").build()
            )
            .addTag("manual_sync")
            .build()

        workManager.enqueue(request)
        Log.d(NewsSyncWorker.TAG, "enqueue: одноразова задача ${request.id}")
        observeOneTimeWork(request.id)
    }

    private fun observeOneTimeWork(id: UUID) {
        lastRequestId = id
        workManager.getWorkInfoByIdLiveData(id).observe(this) { workInfo ->
            if (workInfo != null) {
                val status = workInfo.state.name
                oneTimeStatus.value = if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    val message = workInfo.outputData.getString(NewsSyncWorker.KEY_RESULT)
                    "$status\n$message"
                } else {
                    "$status (${workInfo.state.toUkrainian()})"
                }
            }
        }
    }

    // ========== Періодична задача ==========

    private fun enableNightSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)   // лише Wi-Fi
            .setRequiresCharging(true)                       // лише на зарядці
            .build()

        val request = PeriodicWorkRequestBuilder<NewsSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(
                Data.Builder().putString(NewsSyncWorker.KEY_USER_TOKEN, "abc123").build()
            )
            .addTag("night_sync")
            .build()

        // KEEP: якщо задача з таким ім'ям уже є — нову не створюємо (без дублікатів)
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Log.d(NewsSyncWorker.TAG, "enqueueUniquePeriodicWork: $PERIODIC_WORK_NAME (KEEP)")
    }

    private fun disableNightSync() {
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
        Log.d(NewsSyncWorker.TAG, "cancelUniqueWork: $PERIODIC_WORK_NAME")
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "night_news_sync"
        private const val KEY_LAST_ID = "last_request_id"
    }
}

private fun WorkInfo.State.toUkrainian(): String = when (this) {
    WorkInfo.State.ENQUEUED -> "очікує"
    WorkInfo.State.RUNNING -> "виконується"
    WorkInfo.State.SUCCEEDED -> "успіх"
    WorkInfo.State.FAILED -> "помилка"
    WorkInfo.State.BLOCKED -> "заблоковано"
    WorkInfo.State.CANCELLED -> "скасовано"
}

@Composable
fun SyncScreen(
    oneTimeStatus: StateFlow<String>,
    periodicStatus: StateFlow<String>,
    onSyncNow: () -> Unit,
    onEnableNightSync: () -> Unit,
    onDisableNightSync: () -> Unit
) {
    val oneTime by oneTimeStatus.collectAsStateWithLifecycle()
    val periodic by periodicStatus.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Синхронізація новин", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        Card(Modifier.fillMaxWidth()) {
            Text(
                text = oneTime,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSyncNow, modifier = Modifier.fillMaxWidth()) {
            Text("Синхронізувати зараз")
        }

        Spacer(Modifier.height(32.dp))
        Text(periodic, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onEnableNightSync, modifier = Modifier.fillMaxWidth()) {
            Text("Увімкнути нічну синхронізацію")
        }
        OutlinedButton(onClick = onDisableNightSync, modifier = Modifier.fillMaxWidth()) {
            Text("Вимкнути нічну синхронізацію")
        }
    }
}
