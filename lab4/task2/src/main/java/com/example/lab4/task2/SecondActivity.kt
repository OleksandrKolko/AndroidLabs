package com.example.lab4.task2

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import timber.log.Timber

class SecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("SecondActivity: onCreate() викликано")
        enableEdgeToEdge()
        setContentView(R.layout.activity_second)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.d("SecondActivity: onStart() викликано")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("SecondActivity: onResume() викликано")
    }

    override fun onPause() {
        super.onPause()
        Timber.d("SecondActivity: onPause() викликано")
    }

    override fun onStop() {
        super.onStop()
        Timber.d("SecondActivity: onStop() викликано")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("SecondActivity: onDestroy() викликано")
    }
}