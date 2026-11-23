package com.example.lab5.task3

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        UserTracker.register(this)
        Timber.d("Зареєстровано слухачів у UserTracker: ${UserTracker.getListenersCount()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        UserTracker.unregister(this)
        Timber.d("onDestroy: слухачів у UserTracker: ${UserTracker.getListenersCount()}")
    }
}