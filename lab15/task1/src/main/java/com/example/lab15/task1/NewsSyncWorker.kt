package com.example.lab15.task1

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Фонова задача синхронізації новин.
 * WorkManager гарантує її виконання навіть після закриття застосунку чи перезавантаження.
 */
class NewsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_USER_TOKEN)
        Log.d(TAG, "doWork: старт (token = $token, id = $id, спроба = $runAttemptCount)")

        if (token.isNullOrBlank()) {
            Log.e(TAG, "doWork: немає токена")
            return Result.failure()
        }

        delay(3000)   // імітація мережевого запиту

        val newsCount = Random.nextInt(5, 31)
        val out = Data.Builder()
            .putString(KEY_RESULT, "Успіх: завантажено $newsCount новин")
            .build()

        Log.d(TAG, "doWork: завершено, завантажено $newsCount новин")
        return Result.success(out)
    }

    companion object {
        const val TAG = "NewsSync"
        const val KEY_USER_TOKEN = "user_token"
        const val KEY_RESULT = "result"
    }
}
