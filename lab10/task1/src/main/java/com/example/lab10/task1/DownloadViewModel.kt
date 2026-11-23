package com.example.lab10.task1

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab10.task1.FileRepository.Companion.TAG
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Стан екрана завантаження */
data class DownloadUiState(
    val progress: Int = 0,
    val status: String = "Натисніть «Скачати файл»",
    val isDownloading: Boolean = false
)

/**
 * Шар ViewModel: запускає та скасовує завантаження, зберігає стан UI.
 * Живе довше за Activity, тому завантаження переживає поворот екрана.
 */
class DownloadViewModel : ViewModel() {

    private val fileRepository = FileRepository()

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    init {
        Log.d(TAG, "DownloadViewModel створено: $this")
    }

    fun startDownload() {
        if (downloadJob?.isActive == true) return   // уже завантажуємо

        downloadJob = viewModelScope.launch {
            fileRepository.downloadFile()
                .onStart {
                    Log.d(TAG, "Завантаження розпочато")
                    _uiState.update {
                        it.copy(progress = 0, status = "Завантаження: 0%", isDownloading = true)
                    }
                }
                .onCompletion { cause ->
                    // cause == null — потік завершився успішно;
                    // CancellationException — користувач натиснув «Скасувати»
                    when (cause) {
                        null -> {
                            Log.d(TAG, "onCompletion: файл успішно скачано")
                            _uiState.update {
                                it.copy(status = "Файл успішно скачано!", isDownloading = false)
                            }
                        }
                        is CancellationException -> {
                            Log.d(TAG, "onCompletion: завантаження скасовано")
                            _uiState.update {
                                it.copy(status = "Завантаження скасовано", isDownloading = false)
                            }
                        }
                        else -> {
                            Log.e(TAG, "onCompletion: помилка ${cause.message}")
                            _uiState.update {
                                it.copy(status = "Помилка: ${cause.message}", isDownloading = false)
                            }
                        }
                    }
                }
                .collect { progress ->
                    // collect виконується на Main (контекст viewModelScope)
                    _uiState.update {
                        it.copy(progress = progress, status = "Завантаження: $progress%")
                    }
                }
        }
    }

    fun cancelDownload() {
        Log.d(TAG, "cancelDownload: downloadJob?.cancel()")
        downloadJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: ViewModel знищено, viewModelScope скасовано")
    }
}
