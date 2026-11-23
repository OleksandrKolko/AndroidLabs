package com.example.lab18.task1

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lab18.task1.data.ExchangeRateEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme { ExchangeScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeScreen(viewModel: ExchangeViewModel = viewModel()) {
    val context = LocalContext.current
    val date by viewModel.selectedDate.collectAsStateWithLifecycle()
    val rates by viewModel.rates.collectAsStateWithLifecycle()
    val savedDates by viewModel.savedDates.collectAsStateWithLifecycle()
    val sync by viewModel.syncStatus.collectAsStateWithLifecycle()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val isSyncing = sync is SyncStatus.Running || sync is SyncStatus.Waiting || sync is SyncStatus.Retrying

    Scaffold(
        topBar = { TopAppBar(title = { Text("ПриватБанк — Room") }) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Text("📅 $date")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.refresh()?.let { error ->
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSyncing
                ) { Text("Оновити") }
            }

            // ⭐ Дати, збережені в Room — доступні без Інтернету
            if (savedDates.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    savedDates.forEach { saved ->
                        FilterChip(
                            selected = saved == date,
                            onClick = { viewModel.onDateSelected(saved) },
                            label = { Text(saved) }
                        )
                    }
                }
            }

            SyncStatusLine(sync = sync, hasSavedRates = !rates.isNullOrEmpty())
            Spacer(Modifier.height(8.dp))

            // Список читається ЛИШЕ з Room
            Box(Modifier.fillMaxSize()) {
                val list = rates
                when {
                    list == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    list.isEmpty() -> Text(
                        "Курси за цю дату ще не завантажені",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                    else -> RatesList(date, list)
                }
            }
        }
    }

    if (showDatePicker) {
        ArchiveDatePickerDialog(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                viewModel.onDateSelected(it)
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun SyncStatusLine(sync: SyncStatus, hasSavedRates: Boolean) {
    val (text, color) = when (sync) {
        SyncStatus.None -> return
        SyncStatus.Waiting -> "⏳ Очікує мережі / запуску" to Color.Gray
        SyncStatus.Running -> "🔄 Оновлюється…" to Color(0xFF1565C0)
        is SyncStatus.Retrying -> "⚠️ Помилка мережі, повторна спроба №${sync.attempt} очікує…" to Color(0xFFEF6C00)
        is SyncStatus.Done ->
            if (sync.isEmptyResponse && hasSavedRates)
                "ℹ️ ${sync.message}. Показано раніше збережені курси" to Color(0xFFEF6C00)
            else "✅ ${sync.message}" to Color(0xFF2E7D32)
        is SyncStatus.Failed ->
            (if (hasSavedRates) "❌ ${sync.message}. Показано раніше збережені курси"
            else "❌ ${sync.message}") to Color(0xFFC62828)
    }
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (sync is SyncStatus.Running) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = color, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RatesList(date: String, rates: List<ExchangeRateEntity>) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Збережені курси ПриватБанку на $date (${rates.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(rates, key = { "${it.date}_${it.currency}" }) { rate ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        rate.currency,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(72.dp)
                    )
                    Column {
                        Text("Банк купує: ${rate.purchaseRate.formatRate()} ${rate.baseCurrency}")
                        Text("Банк продає: ${rate.saleRate.formatRate()} ${rate.baseCurrency}")
                    }
                }
            }
        }
    }
}

/** Відсутній курс — «—», не 0 */
private fun Double?.formatRate(): String =
    this?.let { String.format(Locale.US, "%.2f", it) } ?: "—"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveDatePickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val utcFormat = remember {
        SimpleDateFormat(DateUtils.PATTERN, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }
    val nowMillis = System.currentTimeMillis()
    val minMillis = Calendar.getInstance().apply { add(Calendar.YEAR, -DateUtils.ARCHIVE_YEARS) }.timeInMillis

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = runCatching { utcFormat.parse(initialDate)?.time }.getOrNull(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis in minMillis..nowMillis
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onDateSelected(utcFormat.format(it)) } ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    ) {
        DatePicker(state = pickerState)
    }
}
