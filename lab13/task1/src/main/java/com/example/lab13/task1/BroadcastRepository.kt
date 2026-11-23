package com.example.lab13.task1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BroadcastEvent(
    val id: String,
    val type: String,
    val timestamp: String,
    val details: String
)

class BroadcastRepository(private val context: Context) {

    fun events(): Flow<BroadcastEvent> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val event = intent.toEvent()
                Log.d(TAG, "Отримано: ${event.type}")
                trySend(event)
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }

        ContextCompat.registerReceiver(
            context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        Log.d(TAG, "registerReceiver")

        awaitClose {
            context.unregisterReceiver(receiver)
            Log.d(TAG, "unregisterReceiver")
        }
    }

    private fun Intent.toEvent(): BroadcastEvent {
        val now = Date()
        val title = when (action) {
            Intent.ACTION_POWER_CONNECTED -> "Зарядний пристрій підключено"
            Intent.ACTION_POWER_DISCONNECTED -> "Зарядний пристрій відключено"
            Intent.ACTION_AIRPLANE_MODE_CHANGED ->
                if (getBooleanExtra("state", false)) "Режим «У літаку» увімкнено"
                else "Режим «У літаку» вимкнено"
            Intent.ACTION_BATTERY_LOW -> "Низький рівень батареї"
            Intent.ACTION_BATTERY_OKAY -> "Рівень батареї в нормі"
            Intent.ACTION_HEADSET_PLUG -> "Навушники: зміна підключення"
            Intent.ACTION_SCREEN_ON -> "Екран увімкнено"
            Intent.ACTION_SCREEN_OFF -> "Екран вимкнено"
            else -> action ?: "Невідома подія"
        }

        @Suppress("DEPRECATION")
        val extrasText = extras?.keySet()
            ?.joinToString("\n") { key -> "• $key = ${extras?.get(key)}" }
            ?.ifEmpty { null } ?: "немає"

        val details = buildString {
            appendLine("Action: $action")
            appendLine("Точний час: " +
                    SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS", Locale.getDefault()).format(now))
            append("Extras:\n$extrasText")
        }

        return BroadcastEvent(
            id = UUID.randomUUID().toString(),
            type = title,
            timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now),
            details = details
        )
    }

    companion object {
        const val TAG = "BroadcastLog"
    }
}