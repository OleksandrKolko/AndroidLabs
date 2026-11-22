package com.example.lab4.task2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity: onCreate() викликано")
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Перехід на другий екран
        findViewById<Button>(R.id.btnOpenSecond).setOnClickListener {
            Timber.d("MainActivity: натиснуто кнопку переходу")
            startActivity(Intent(this, SecondActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.d("MainActivity: onStart() викликано")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("MainActivity: onResume() викликано")
    }

    override fun onPause() {
        super.onPause()
        Timber.d("MainActivity: onPause() викликано")
    }

    override fun onStop() {
        super.onStop()
        Timber.d("MainActivity: onStop() викликано")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("MainActivity: onDestroy() викликано")
    }
}