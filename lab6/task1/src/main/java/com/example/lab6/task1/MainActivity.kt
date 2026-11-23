package com.example.lab6.task1

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lab6.task1.TimerModel.Companion.TAG
import com.example.lab6.task1.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // ViewModel переживає поворот екрана: при новій Activity повертається той самий об'єкт
    private val viewModel by viewModels<TimerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity.onCreate: $this, viewModel = $viewModel")

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Підписки на стан
        viewModel.timerLiveData.observe(this) { seconds ->
            binding.tvTimer.text = if (seconds == 0L) "Час вийшов!" else seconds.toString()
        }

        viewModel.isTimerRunning.observe(this) { isRunning ->
            binding.btnStart.isEnabled = !isRunning
        }

        // Дії користувача
        binding.btnStart.setOnClickListener {
            val userSeconds = binding.etSeconds.text.toString().toLongOrNull()
                ?.takeIf { it > 0 }
                ?: TimerViewModel.DEFAULT_SECONDS
            viewModel.startTimer(userSeconds)
        }

        binding.btnReset.setOnClickListener {
            viewModel.resetTimer()
        }
    }
}