package com.example.lab17.task1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lab17.task1.data.ExchangeRate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme { ExchangeArchiveScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeArchiveScreen(viewModel: ExchangeArchiveViewModel = viewModel()) {
    // Нова Activity після повороту підписується на стан ТІЄЇ САМОЇ ViewModel.
    // Запит запускає лише кнопка — не тіло composable і не onCreate.
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ПриватБанк — валюти") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Вибір дати + кнопка
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    enabled = !state.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📅 ${state.selectedDate}")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = viewModel::loadRates,
                    enabled = !state.isLoading
                ) {
                    Text("Показати курси")
                }
            }

            // ⭐ Пошук і сортування (лише над завантаженими даними)
            if (state.request is RequestState.Success) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        label = { Text("Пошук за кодом") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = state.sortByCode,
                        onClick = viewModel::onSortToggled,
                        label = { Text("A–Z") }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Вміст залежно від стану
            Box(Modifier.fillMaxSize()) {
                when (val request = state.request) {
                    RequestState.Idle -> CenterMessage("Оберіть дату та натисніть «Показати курси»")

                    RequestState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                    is RequestState.Error -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(request.message, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = viewModel::loadRates) { Text("Повторити") }
                    }

                    is RequestState.Success -> {
                        val rates = state.visibleRates
                        when {
                            request.rates.isEmpty() ->
                                CenterMessage("За ${request.date} немає даних про курси")
                            rates.isEmpty() ->
                                CenterMessage("Нічого не знайдено за запитом «${state.searchQuery}»")
                            else -> RatesList(date = request.date, rates = rates)
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        ArchiveDatePickerDialog(
            initialDate = state.selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                viewModel.onDateSelected(date)
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RatesList(date: String, rates: List<ExchangeRate>) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Курси ПриватБанку на $date",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(rates, key = { it.currency.orEmpty() }) { rate -> RateCard(rate) }
    }
}

@Composable
private fun RateCard(rate: ExchangeRate) {
    val base = rate.baseCurrency ?: "UAH"
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                rate.currency.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(72.dp)
            )
            Column(Modifier.weight(1f)) {
                Text("Банк купує: ${rate.purchaseRate.formatRate()} $base")
                Text("Банк продає: ${rate.saleRate.formatRate()} $base")
                Text(
                    "НБУ: ${rate.saleRateNB.formatRate(4)} $base",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Відсутній курс показуємо як «—», а не 0 і не курс НБУ */
private fun Double?.formatRate(decimals: Int = 2): String =
    this?.let { String.format(Locale.US, "%.${decimals}f", it) } ?: "—"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveDatePickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    // DatePicker працює з мілісекундами UTC
    val utcFormat = remember {
        SimpleDateFormat(ExchangeArchiveViewModel.DATE_PATTERN, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    val nowMillis = System.currentTimeMillis()
    val minMillis = Calendar.getInstance().apply { add(Calendar.YEAR, -4) }.timeInMillis

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = runCatching { utcFormat.parse(initialDate)?.time }.getOrNull(),
        selectableDates = object : SelectableDates {
            // Майбутні дати та дати старші за 4 роки обрати неможливо
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis in minMillis..nowMillis
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { onDateSelected(utcFormat.format(it)) }
                        ?: onDismiss()
                }
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    ) {
        DatePicker(state = pickerState)
    }
}
