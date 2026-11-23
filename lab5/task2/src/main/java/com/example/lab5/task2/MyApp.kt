package com.example.lab5.task2

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import timber.log.Timber

class MyApp : Application() {

    val backgroundDetector = BackgroundDetector()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Стежимо за життєвим циклом усього процесу, а не окремих Activity
        ProcessLifecycleOwner.get().lifecycle.addObserver(backgroundDetector)
    }
}