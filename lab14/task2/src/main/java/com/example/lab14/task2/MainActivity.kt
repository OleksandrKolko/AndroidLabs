package com.example.lab14.task2

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Сюди Activity переносить секунди зі служби, а звідси їх читає Compose
    private val timerFlow = MutableStateFlow(0)

    private var timerService: TimerForegroundService? = null
    private var isBound = false
    private var collectJob: Job? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerForegroundService.LocalBinder
            val myService = binder.getService()
            timerService = myService
            isBound = true
            Log.d(TimerForegroundService.TAG, "onServiceConnected")

            // Підключаємо потік секунд зі служби до Activity
            collectJob = lifecycleScope.launch {
                myService.getTimerFlow().collect { seconds ->
                    timerFlow.value = seconds
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            timerService = null
            collectJob?.cancel()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    TimerScreen(
                        timerFlow = timerFlow,
                        onStart = {
                            ContextCompat.startForegroundService(
                                this, Intent(this, TimerForegroundService::class.java)
                            )
                        },
                        onStop = {
                            timerService?.stopTimer()
                            stopService(Intent(this, TimerForegroundService::class.java))
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, TimerForegroundService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            collectJob?.cancel()
            unbindService(connection)
            isBound = false
            timerService = null
            Log.d(TimerForegroundService.TAG, "unbindService")
        }
    }
}

@Composable
fun TimerScreen(
    timerFlow: StateFlow<Int>,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val seconds by timerFlow.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bound Service", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Text("$seconds", fontSize = 96.sp, fontWeight = FontWeight.Bold)
        Text("секунд")
        Spacer(Modifier.height(32.dp))
        Button(onClick = onStart) { Text("Старт Таймер") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onStop) { Text("Стоп Таймер") }
    }
}
