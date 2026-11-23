package com.example.lab8.task1

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab8.task1.DiceModel.Companion.TAG
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Шар ViewModel: зберігає стан екрана та керує корутинами.
 * Усі корутини запускаються у viewModelScope — вони переживають поворот
 * екрана й автоматично скасовуються, коли ViewModel знищується (onCleared).
 */
class DiceViewModel : ViewModel() {

    private val diceModel = DiceModel()

    // Поточні значення 5 кісток
    private val _diceValues = MutableLiveData(List(DICE_COUNT) { 6 })
    val diceValues: LiveData<List<Int>>
        get() = _diceValues

    // Чи обертаються кістки зараз
    private val _isRolling = MutableLiveData(false)
    val isRolling: LiveData<Boolean>
        get() = _isRolling

    // Текст кнопки: "ROLL" або "RESTART"
    private val _buttonText = MutableLiveData(TEXT_ROLL)
    val buttonText: LiveData<String>
        get() = _buttonText

    // Поточна корутина обертання — щоб скасувати її при RESTART
    private var job: Job? = null

    init {
        Log.d(TAG, "DiceViewModel створено: $this")
    }

    /**
     * ROLL — запускає обертання.
     * RESTART (під час обертання) — скасовує поточну корутину і починає заново.
     */
    fun rollDice() {
        if (job?.isActive == true) {
            Log.d(TAG, "RESTART: скасовуємо поточне обертання")
        }
        job?.cancel()

        job = viewModelScope.launch {
            _isRolling.value = true
            _buttonText.value = TEXT_RESTART
            Log.d(TAG, "rollDice: старт $DICE_COUNT паралельних корутин")

            val values = MutableList(DICE_COUNT) { _diceValues.value?.getOrNull(it) ?: 6 }

            try {
                // viewModelScope працює на Main Thread, тому value (а не postValue)
                val result = diceModel.rollAll(DICE_COUNT) { index, value ->
                    values[index] = value
                    _diceValues.value = values.toList()
                }

                // Сюди доходимо, лише коли завершились УСІ 5 корутин
                _diceValues.value = result
                _isRolling.value = false
                _buttonText.value = TEXT_ROLL
                Log.d(TAG, "Усі $DICE_COUNT корутин завершені: $result")
            } catch (e: CancellationException) {
                Log.d(TAG, "Корутину обертання скасовано")
                throw e   // CancellationException обов'язково прокидаємо далі
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // viewModelScope скасується автоматично; лог — щоб це було видно в Logcat
        Log.d(TAG, "onCleared: ViewModel знищено, viewModelScope скасовано")
    }

    companion object {
        const val DICE_COUNT = 5
        const val TEXT_ROLL = "ROLL"
        const val TEXT_RESTART = "RESTART"
    }
}
