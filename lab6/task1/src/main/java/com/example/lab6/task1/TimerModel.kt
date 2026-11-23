package com.example.lab6.task1

import android.os.CountDownTimer
import android.util.Log

class TimerModel {

    private var timer: CountDownTimer? = null

    fun start(seconds: Long, onTick: (Long) -> Unit, onFinish: () -> Unit) {
        // Якщо відлік уже триває, скасовуємо його: жодних паралельних «зомбі-таймерів»
        stop()

        Log.d(TAG, "TimerModel.start: $seconds с")
        timer = object : CountDownTimer(seconds * 1_000L, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                // Округлюємо вгору, щоб першим показувалося 30, а не 29
                val secondsLeft = (millisUntilFinished + 999) / 1_000L
                Log.d(TAG, "onTick: $secondsLeft")
                onTick(secondsLeft)
            }

            override fun onFinish() {
                Log.d(TAG, "onFinish: час вийшов")
                timer = null
                onFinish()
            }
        }.start()
    }

    fun stop() {
        timer?.let {
            it.cancel()
            Log.d(TAG, "TimerModel.stop: таймер скасовано")
        }
        timer = null
    }

    companion object {
        const val TAG = "TimerMVVM"
    }
}