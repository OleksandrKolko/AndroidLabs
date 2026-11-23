package com.example.lab7.task1

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lab7.task1.DiceViewModel.Companion.TAG
import com.example.lab7.task1.databinding.ActivityMainBinding

/**
 * View-шар: лише відображає стан із ViewModel і передає їй натискання кнопки.
 * Тут немає потоків, генерації чисел чи таймерів.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var imageViews: Array<ImageView>

    // Після повороту екрана повертається ТОЙ САМИЙ екземпляр ViewModel
    private val viewModel by viewModels<DiceViewModel>()

    private val drawables = arrayOf(
        R.drawable.die_1, R.drawable.die_2,
        R.drawable.die_3, R.drawable.die_4,
        R.drawable.die_5, R.drawable.die_6
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity.onCreate: $this, viewModel = $viewModel")

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageViews = arrayOf(
            binding.die1,
            binding.die2,
            binding.die3,
            binding.die4,
            binding.die5
        )

        // Підписка на значення кісток
        viewModel.diceValues.observe(this) { values ->
            values.forEachIndexed { index, value ->
                imageViews[index].setImageResource(drawables[value - 1])
            }
        }

        // Підписка на стан обертання
        viewModel.isRolling.observe(this) { isRolling ->
            binding.rollButton.isEnabled = !isRolling
        }

        binding.rollButton.setOnClickListener {
            viewModel.rollDice()
        }

        // На Android 12+ «Назад» лише згортає застосунок.
        // Явно закриваємо екран, щоб ViewModel знищилась і спрацював onCleared()
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }
}