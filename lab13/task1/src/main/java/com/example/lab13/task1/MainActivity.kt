package com.example.lab13.task1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                BroadcastLogScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastLogScreen(viewModel: BroadcastViewModel = viewModel()) {
    val events by viewModel.events.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Журнал подій (${events.size})") },
                actions = {
                    TextButton(onClick = viewModel::clearAll, enabled = events.isNotEmpty()) {
                        Text("Очистити")
                    }
                }
            )
        }
    ) { padding ->
        if (events.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Подій ще немає.\nПідключіть зарядку, увімкніть режим «У літаку»\nабо вимкніть екран.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = events, key = { event -> event.id }) { event ->
                    ExpandableEventCard(
                        event = event,
                        onDelete = { viewModel.deleteEvent(event) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandableEventCard(
    event: BroadcastEvent,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // rememberSaveable + key у LazyColumn → стан зберігається при скролі та повороті
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .animateContentSize()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(event.type, fontWeight = FontWeight.Bold)
                    Text(event.timestamp, style = MaterialTheme.typography.bodySmall)
                }
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Видалити")
                }
            }
            if (isExpanded) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(event.details, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}