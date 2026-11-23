package com.example.lab5.task1

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Один обробник для ВСІХ Activity застосунку
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                log("onActivityCreated", activity)
            }

            override fun onActivityStarted(activity: Activity) {
                log("onActivityStarted", activity)
            }

            override fun onActivityResumed(activity: Activity) {
                log("onActivityResumed", activity)
            }

            override fun onActivityPaused(activity: Activity) {
                log("onActivityPaused", activity)
            }

            override fun onActivityStopped(activity: Activity) {
                log("onActivityStopped", activity)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                log("onActivityDestroyed", activity)
            }
        })
    }

    private fun log(event: String, activity: Activity) {
        // Lifecycle є лише в Activity, що реалізують LifecycleOwner (наш AppCompatActivity реалізує)
        val state = (activity as? LifecycleOwner)?.lifecycle?.currentState
        Timber.d("$event: ${activity.javaClass.simpleName}, currentState = $state")
    }
}