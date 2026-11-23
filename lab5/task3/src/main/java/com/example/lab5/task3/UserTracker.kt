package com.example.lab5.task3

import android.content.Context

object UserTracker {
    private val listeners = mutableListOf<Context>()

    // ❌ Було (виток пам'яті):
    //    listeners.add(context)
    // Синглтон живе весь час існування процесу і тримав посилання на кожну
    // знищену MainActivity, тож GC не міг звільнити старі екрани.

    // ✅ Спосіб A: зберігаємо глобальний контекст застосунку,
    // який і так живе весь час існування процесу
    fun register(context: Context) {
        listeners.add(context.applicationContext)
    }

    // ✅ Спосіб B: вчасно видаляємо слухача
    fun unregister(context: Context) {
        listeners.remove(context.applicationContext)
    }

    fun getListenersCount(): Int = listeners.size
}