package com.example.lab10.task1

import android.os.Bundle
import android.util.Log
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lab10.task1.FileRepository.Companion.TAG
import com.example.lab10.task1.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * Шар View: лише відмальовує стан і передає кліки у ViewModel.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Після повороту екрана повертається ТОЙ САМИЙ екземпляр ViewModel
    private val viewModel by viewModels<DownloadViewModel>()

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

        binding.btnDownload.setOnClickListener { viewModel.startDownload() }
        binding.btnCancel.setOnClickListener { viewModel.cancelDownload() }

        // Збираємо стан, лише поки екран видимий (STARTED);
        // після повороту нова Activity одразу отримує актуальний відсоток
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.progress = state.progress
                    binding.tvStatus.text = state.status
                    binding.btnDownload.isEnabled = !state.isDownloading
                    binding.btnCancel.isEnabled = state.isDownloading
                }
            }
        }

        // На Android 12+ «Назад» лише згортає застосунок — явно закриваємо екран
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }
}
