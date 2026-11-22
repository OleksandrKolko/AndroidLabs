package com.example.lab4.task1

import android.os.Bundle
import android.os.Process
import android.widget.Button
import android.widget.TextView
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

        Timber.d("MainActivity успішно запущена!")

        val app = application as MyApp
        val tvPid: TextView = findViewById(R.id.tvPid)
        val tvCount: TextView = findViewById(R.id.tvCount)
        val btnIncrement: Button = findViewById(R.id.btnIncrement)

        // PID поточного процесу: зручно порівнювати з виводом ADB
        tvPid.text = "PID: ${Process.myPid()}"
        tvCount.text = "startCount = ${app.startCount}"

        // ⭐ Збільшуємо глобальний лічильник у MyApp
        btnIncrement.setOnClickListener {
            app.startCount++
            tvCount.text = "startCount = ${app.startCount}"
            Timber.d("startCount = ${app.startCount}")
        }
    }
}