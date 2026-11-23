package com.example.lab17.task1

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.lab17.task1.data.ExchangeArchiveResponse
import com.example.lab17.task1.data.ExchangeRate
import com.example.lab17.task1.data.PrivatBankApi
import com.example.lab17.task1.data.PrivatBankNetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Стан запиту */
sealed interface RequestState {
    data object Idle : RequestState                              // до першого запиту
    data object Loading : RequestState                           // завантаження
    data class Success(val date: String, val rates: List<ExchangeRate>) : RequestState
    data class Error(val message: String) : RequestState
}

/** Стан усього екрана */
data class ExchangeUiState(
    val selectedDate: String,
    val request: RequestState = RequestState.Idle,
    val searchQuery: String = "",          // ⭐ пошук
    val sortByCode: Boolean = false        // ⭐ сортування за абеткою
) {
    val isLoading: Boolean get() = request is RequestState.Loading

    /** ⭐ Пошук і сортування над уже завантаженими даними — без нових запитів */
    val visibleRates: List<ExchangeRate>
        get() {
            val rates = (request as? RequestState.Success)?.rates ?: return emptyList()
            val filtered = rates.filter {
                it.currency.orEmpty().contains(searchQuery.trim(), ignoreCase = true)
            }
            return if (sortByCode) filtered.sortedBy { it.currency } else filtered
        }
}

class ExchangeArchiveViewModel(
    private val api: PrivatBankApi = PrivatBankNetworkModule.api
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExchangeUiState(selectedDate = today()))
    val uiState: StateFlow<ExchangeUiState> = _uiState.asStateFlow()

    // Поточний мережевий виклик — щоб скасувати його в onCleared()
    private var currentCall: Call<ExchangeArchiveResponse>? = null

    fun onDateSelected(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSortToggled() {
        _uiState.update { it.copy(sortByCode = !it.sortByCode) }
    }

    fun loadRates() {
        val date = _uiState.value.selectedDate
        validateDate(date)?.let { error ->
            _uiState.update { it.copy(request = RequestState.Error(error)) }
            return
        }

        currentCall?.cancel()
        _uiState.update { it.copy(request = RequestState.Loading) }
        Log.d(TAG, "Запит до API: date=$date")

        val call = api.getExchangeArchive(date)
        currentCall = call
        call.enqueue(object : Callback<ExchangeArchiveResponse> {

            override fun onResponse(
                call: Call<ExchangeArchiveResponse>,
                response: Response<ExchangeArchiveResponse>
            ) {
                val body = response.body()
                val newState = when {
                    !response.isSuccessful ->
                        RequestState.Error("Помилка сервера: HTTP ${response.code()}")
                    body == null ->
                        RequestState.Error("Сервер повернув порожню відповідь")
                    else -> {
                        // Записи без коду валюти не показуємо
                        val rates = body.exchangeRate.orEmpty().filter { !it.currency.isNullOrBlank() }
                        RequestState.Success(date = body.date ?: date, rates = rates)
                    }
                }
                Log.d(TAG, "Відповідь: HTTP ${response.code()}, стан = ${newState::class.simpleName}")
                _uiState.update { it.copy(request = newState) }
            }

            override fun onFailure(call: Call<ExchangeArchiveResponse>, t: Throwable) {
                if (call.isCanceled) return
                Log.e(TAG, "Помилка мережі: ${t.message}")
                _uiState.update {
                    it.copy(
                        request = RequestState.Error(
                            "Не вдалося завантажити курси. Перевірте з'єднання та спробуйте знову"
                        )
                    )
                }
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        currentCall?.cancel()
        Log.d(TAG, "onCleared: незавершений запит скасовано")
    }

    companion object {
        const val TAG = "PrivatApi"
        const val DATE_PATTERN = "dd.MM.yyyy"
        private const val ARCHIVE_YEARS = 4

        private fun formatter() = SimpleDateFormat(DATE_PATTERN, Locale.US).apply {
            isLenient = false      // 31.02.2026 → ParseException
        }

        fun today(): String = formatter().format(Calendar.getInstance().time)

        /** null — дата коректна; інакше текст помилки */
        fun validateDate(date: String): String? {
            val parsed = try {
                formatter().parse(date)
            } catch (e: ParseException) {
                null
            } ?: return "Некоректна дата «$date». Формат: дд.ММ.рррр"

            val now = Calendar.getInstance()
            val minDate = Calendar.getInstance().apply { add(Calendar.YEAR, -ARCHIVE_YEARS) }
            return when {
                parsed.after(now.time) -> "Не можна обрати майбутню дату"
                parsed.before(minDate.time) -> "Архів доступний лише за останні $ARCHIVE_YEARS роки"
                else -> null
            }
        }
    }
}
