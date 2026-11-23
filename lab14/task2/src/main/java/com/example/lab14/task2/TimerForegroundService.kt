package com.example.lab14.task2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Гібридна служба: Foreground (живе у фоні) + Bound (Activity читає секунди через Binder).
 */
class TimerForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    // Поточні секунди: їх читає Activity через Binder
    private val _timerSeconds = MutableStateFlow(0)
    fun getTimerFlow(): StateFlow<Int> = _timerSeconds.asStateFlow()

    // Bound Service: доступ Activity до екземпляра служби
    inner class LocalBinder : Binder() {
        fun getService(): TimerForegroundService = this@TimerForegroundService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification("Таймер запущено"),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )

        if (timerJob?.isActive != true) {
            timerJob = scope.launch {
                while (isActive) {
                    delay(1000)
                    _timerSeconds.value++
                    Log.d(TAG, "Минуло ${_timerSeconds.value} секунд")
                    updateNotification("Минуло ${_timerSeconds.value} с")
                }
            }
        }
        return START_STICKY
    }

    /**
     * Зупинка таймера, яку Activity викликає через Binder.
     * Потрібна, бо поки до служби хтось прив'язаний, stopService() її не знищить.
     */
    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerSeconds.value = 0
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "stopTimer: таймер зупинено, сповіщення прибрано")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        Log.d(TAG, "onDestroy")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Таймер", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Фоновий таймер")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun updateNotification(text: String) {
        if (timerJob?.isActive == true) {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(text))
        }
    }

    companion object {
        const val TAG = "Timer"
        private const val CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 1
    }
}
