package com.example.lab6.task1

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.lab6.task1.TimerModel.Companion.TAG

class TimerViewModel : ViewModel() {

    private val timerModel = TimerModel()

    // Залишок секунд: змінювати може лише ViewModel, Activity тільки читає
    private val _timerLiveData = MutableLiveData(DEFAULT_SECONDS)
    val timerLiveData: LiveData<Long>
        get() = _timerLiveData

    // Чи працює таймер зараз
    private val _isTimerRunning = MutableLiveData(false)
    val isTimerRunning: LiveData<Boolean>
        get() = _isTimerRunning

    init {
        Log.d(TAG, "TimerViewModel створено: $this")
    }

    fun startTimer(seconds: Long = DEFAULT_SECONDS) {
        _timerLiveData.value = seconds
        _isTimerRunning.value = true

        timerModel.start(
            seconds = seconds,
            onTick = { secondsLeft -> _timerLiveData.value = secondsLeft },
            onFinish = {
                _timerLiveData.value = 0L
                _isTimerRunning.value = false
            }
        )
    }

    fun resetTimer() {
        timerModel.stop()
        _timerLiveData.value = DEFAULT_SECONDS
        _isTimerRunning.value = false
        Log.d(TAG, "resetTimer: скинуто до $DEFAULT_SECONDS с")
    }

    // Викликається лише при остаточному закритті екрана (не при повороті)
    override fun onCleared() {
        super.onCleared()
        timerModel.stop()
        Log.d(TAG, "onCleared: ViewModel знищено, таймер зупинено")
    }

    companion object {
        const val DEFAULT_SECONDS = 30L
    }
}