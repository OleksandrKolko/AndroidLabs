package com.example.lab7.task1

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * ViewModel симулятора 5 гральних кісток.
 *
 * Уся логіка (фонові потоки, генерація випадкових чисел, таймінги) живе тут,
 * а не в Activity. ViewModel переживає поворот екрана, тому потоки
 * не перезапускаються і не дублюються ("зомбі-потоки").
 */
class DiceViewModel : ViewModel() {

    // Поточні значення 5 кісток (1..6). Змінювати може лише ViewModel.
    private val _diceValues = MutableLiveData(List(DICE_COUNT) { 6 })
    val diceValues: LiveData<List<Int>>
        get() = _diceValues

    // true — кістки обертаються, false — зупинилися
    private val _isRolling = MutableLiveData(false)
    val isRolling: LiveData<Boolean>
        get() = _isRolling

    // Усі запущені фонові потоки — щоб скасувати їх в onCleared()
    private val workers = mutableListOf<Thread>()

    init {
        Log.d(TAG, "DiceViewModel створено: $this")
    }

    /**
     * Частина 3 (основний режим): кожна кістка обертається у СВОЄМУ потоці
     * зі своєю випадковою тривалістю 1.5–3 с. Кнопка стає активною лише
     * після того, як завершаться ВСІ 5 потоків.
     */
    fun rollDice() {
        // Викликається з Main Thread, тому value (а не postValue) — кнопка вимикається негайно
        if (_isRolling.value == true) return
        _isRolling.value = true
        workers.clear()

        Log.d(TAG, "rollDice: старт паралельного обертання ($DICE_COUNT потоків)")

        // Спільний масив значень; доступ — лише під synchronized
        val values = IntArray(DICE_COUNT) { _diceValues.value?.getOrNull(it) ?: 6 }
        val unfinished = AtomicInteger(DICE_COUNT)

        repeat(DICE_COUNT) { index ->
            val durationMs = Random.nextLong(1_500L, 3_001L)

            workers += thread(name = "Dice-${index + 1}") {
                try {
                    val iterations = durationMs / TICK_MS
                    repeat(iterations.toInt()) {
                        val snapshot = synchronized(values) {
                            values[index] = Random.nextInt(1, 7)
                            values.toList()
                        }
                        // postValue — безпечна передача значення з фонового потоку в Main Thread
                        _diceValues.postValue(snapshot)
                        Thread.sleep(TICK_MS)
                    }
                    Log.d(
                        TAG,
                        "${Thread.currentThread().name}: зупинилась на ${values[index]} " +
                            "через $durationMs мс"
                    )

                    // Останній потік, що завершився, вмикає кнопку
                    if (unfinished.decrementAndGet() == 0) {
                        Log.d(TAG, "Усі $DICE_COUNT потоків завершені")
                        _isRolling.postValue(false)
                    }
                } catch (e: InterruptedException) {
                    Log.d(TAG, "${Thread.currentThread().name}: скасовано")
                }
            }
        }
    }

    /**
     * Частина 1: обертання всіх кісток в ОДНОМУ фоновому потоці
     * (20 ітерацій по 100 мс ≈ 2 с). Залишено для порівняння з rollDice().
     */
    fun rollDiceSingleThread() {
        if (_isRolling.value == true) return
        _isRolling.value = true
        workers.clear()

        Log.d(TAG, "rollDiceSingleThread: старт")

        workers += thread(name = "Dice-single") {
            try {
                repeat(20) {
                    _diceValues.postValue(List(DICE_COUNT) { Random.nextInt(1, 7) })
                    Thread.sleep(TICK_MS)
                }
                Log.d(TAG, "rollDiceSingleThread: завершено")
                _isRolling.postValue(false)
            } catch (e: InterruptedException) {
                Log.d(TAG, "rollDiceSingleThread: скасовано")
            }
        }
    }

    /**
     * Викликається лише при остаточному закритті екрана (не при повороті).
     * Перериваємо всі фонові потоки, щоб не було витоку пам'яті та зомбі-потоків.
     */
    override fun onCleared() {
        super.onCleared()
        workers.forEach { it.interrupt() }
        workers.clear()
        Log.d(TAG, "onCleared: ViewModel знищено, усі потоки скасовано")
    }

    companion object {
        const val TAG = "DiceMVVM"
        const val DICE_COUNT = 5
        private const val TICK_MS = 100L
    }
}
