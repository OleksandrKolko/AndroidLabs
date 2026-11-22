package com.example.lab4.task1

import android.app.Application
import timber.log.Timber

class MyApp : Application() {

    // ⭐ Глобальний лічильник: живе, поки живе процес
    var startCount = 0

    override fun onCreate() {
        super.onCreate()

        // Timber логує лише в Debug-збірці; у Release логів не буде
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("MyApp.onCreate(): процес застосунку створено")
    }
}