package com.example.lab18.task1

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ExchangeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicSync()
    }

    /**
     * Раз на годину оновлюємо курси НА ПОТОЧНУ ДАТУ.
     * Дату НЕ передаємо — Worker визначає її сам під час кожного запуску.
     * Стале ім'я + KEEP → повторний запуск застосунку не створює дублікат.
     */
    private fun schedulePeriodicSync() {
        val periodicRequest =
            PeriodicWorkRequestBuilder<ExchangeRatesSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(networkConstraints())
                .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
        Log.d(ExchangeRatesSyncWorker.TAG, "Періодичне оновлення зареєстровано (KEEP)")
    }

    companion object {
        const val PERIODIC_WORK_NAME = "exchange_rates_periodic"

        fun networkConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
