package com.example.lab18.task1

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.lab18.task1.data.ExchangeDatabase
import com.example.lab18.task1.data.ExchangeRateEntity
import com.example.lab18.task1.data.PrivatBankNetworkModule
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * Worker: API → Room. Працює без Activity та ViewModel.
 * Якщо дату не передано (періодичне оновлення) — береться поточна дата
 * на момент КОЖНОГО виконання.
 */
class ExchangeRatesSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val date = inputData.getString(KEY_DATE) ?: DateUtils.today()
        Log.d(TAG, "doWork: старт, date=$date, спроба=$runAttemptCount")

        DateUtils.validate(date)?.let { error ->
            Log.e(TAG, "Некоректні вхідні дані: $error")
            return Result.failure(workDataOf(KEY_MESSAGE to error))
        }

        val api = PrivatBankNetworkModule.api
        val dao = ExchangeDatabase.getDatabase(applicationContext).exchangeRateDao()

        return try {
            val response = api.getExchangeArchive(date)

            when {
                // 5xx — тимчасова проблема сервера → повторимо пізніше
                response.code() >= 500 -> {
                    Log.w(TAG, "HTTP ${response.code()} → retry")
                    Result.retry()
                }
                !response.isSuccessful -> {
                    Log.e(TAG, "HTTP ${response.code()} → failure")
                    Result.failure(workDataOf(KEY_MESSAGE to "Помилка сервера: HTTP ${response.code()}"))
                }
                else -> {
                    val body = response.body()
                        ?: return Result.failure(workDataOf(KEY_MESSAGE to "Сервер повернув порожню відповідь"))

                    // Дата відповіді має збігатися із запитаною
                    if (body.date != date) {
                        Log.e(TAG, "Дата відповіді ${body.date} ≠ $date")
                        return Result.failure(workDataOf(KEY_MESSAGE to "Сервер повернув курси за іншу дату"))
                    }

                    val entities = body.exchangeRate.orEmpty()
                        .filter { !it.currency.isNullOrBlank() }   // без коду валюти — пропускаємо
                        .map {
                            ExchangeRateEntity(
                                date = body.date,
                                currency = it.currency!!,
                                baseCurrency = it.baseCurrency ?: "UAH",
                                purchaseRate = it.purchaseRate,
                                saleRate = it.saleRate
                            )
                        }

                    if (entities.isEmpty()) {
                        // Порожня коректна відповідь: старі записи НЕ чіпаємо
                        Log.w(TAG, "Сервер не повернув курсів за $date")
                        Result.success(workDataOf(KEY_MESSAGE to "Сервер не повернув курсів"))
                    } else {
                        dao.upsertAll(entities)   // success — лише ПІСЛЯ запису в Room
                        Log.d(TAG, "Збережено в Room: ${entities.size} валют за $date")
                        Result.success(workDataOf(KEY_MESSAGE to "Оновлено: ${entities.size} валют"))
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e                      // скасування не перетворюємо на retry
        } catch (e: IOException) {
            Log.w(TAG, "Мережева помилка: ${e.message} → retry")
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Некоректна відповідь: ${e.message}")
            Result.failure(workDataOf(KEY_MESSAGE to "Некоректна відповідь сервера"))
        }
    }

    companion object {
        const val TAG = "ExchangeSync"
        const val KEY_DATE = "date"
        const val KEY_MESSAGE = "message"
    }
}
