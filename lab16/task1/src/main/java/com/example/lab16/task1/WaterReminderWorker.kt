package com.example.lab16.task1

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Періодична фонова задача: збільшує лічильник у DataStore та показує сповіщення.
 */
class WaterReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // 1. Лічильник: Worker не має доступу до ViewModel, тому пишемо в DataStore
        val count = WaterSettings(applicationContext).incrementSentCount()
        Log.d(TAG, "doWork: нагадування №$count (id = $id)")

        // 2. Сповіщення (на Android 13+ лише за наявності дозволу)
        if (hasNotificationPermission()) {
            showNotification(count)
        } else {
            Log.w(TAG, "Немає дозволу POST_NOTIFICATIONS — сповіщення не показано")
        }
        return Result.success()
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    private fun showNotification(count: Int) {
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, "Нагадування про воду", NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Трекер води")
            .setContentText("Час випити склянку води! 💧")
            .setSubText("Нагадування №$count")
            .setSmallIcon(R.drawable.ic_water_drop)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val TAG = "WaterReminder"
        const val CHANNEL_ID = "water_channel"
    }
}
