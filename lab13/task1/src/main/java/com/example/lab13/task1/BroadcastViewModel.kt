package com.example.lab13.task1

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BroadcastViewModel(application: Application) : AndroidViewModel(application) {

    // applicationContext: не тримаємо Activity → немає витоку пам'яті
    private val repository = BroadcastRepository(application.applicationContext)

    private val _events = MutableStateFlow<List<BroadcastEvent>>(emptyList())
    val events: StateFlow<List<BroadcastEvent>> = _events.asStateFlow()

    init {
        // Підписка живе у viewModelScope → переживає поворот екрана;
        // приймач відписується (awaitClose) лише при onCleared
        viewModelScope.launch {
            repository.events().collect { event ->
                _events.update { listOf(event) + it }   // нові — згори
            }
        }
    }

    fun deleteEvent(event: BroadcastEvent) {
        _events.update { list -> list.filterNot { it.id == event.id } }
    }

    fun clearAll() {
        _events.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(BroadcastRepository.TAG, "onCleared")
    }
}