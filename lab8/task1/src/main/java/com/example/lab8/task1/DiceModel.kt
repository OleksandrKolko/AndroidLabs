package com.example.lab8.task1

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Шар Model: логіка обертання кісток.
 * Нічого не знає про UI та LiveData. Усі функції — suspend,
 * усі паузи — неблокуючий delay() (жодних Thread.sleep / Handler).
 */
class DiceModel {

    /**
     * Обертає ОДНУ кістку протягом [durationMs]:
     * кожні 100 мс генерує нову грань і повідомляє її через [onTick].
     * Повертає підсумкову грань.
     */
    suspend fun rollDie(
        index: Int,
        durationMs: Long,
        onTick: (value: Int) -> Unit
    ): Int {
        var value = Random.nextInt(1, 7)
        val iterations = durationMs / TICK_MS
        repeat(iterations.toInt()) {
            value = Random.nextInt(1, 7)
            onTick(value)
            delay(TICK_MS)   // призупиняє корутину, а не блокує потік
        }
        Log.d(TAG, "Кістка ${index + 1}: зупинилась на $value через $durationMs мс")
        return value
    }

    /**
     * Частина 2: паралельне обертання [count] кісток.
     * Для кожної кістки — окрема корутина async зі своєю тривалістю 1.5–3 с.
     * awaitAll() чекає, поки завершаться ВСІ корутини.
     */
    suspend fun rollAll(
        count: Int,
        onTick: (index: Int, value: Int) -> Unit
    ): List<Int> = coroutineScope {
        (0 until count).map { index ->
            async {
                val durationMs = Random.nextLong(1_500L, 3_001L)
                rollDie(index, durationMs) { value -> onTick(index, value) }
            }
        }.awaitAll()
    }

    companion object {
        const val TAG = "DiceCoroutines"
        private const val TICK_MS = 100L
    }
}
