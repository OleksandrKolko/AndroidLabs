package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // Тег для фільтрації повідомлень у Logcat
    private val TAG = "SimpleCalculator"

    // Посилання на елементи інтерфейсу
    private lateinit var etFirstNumber: EditText
    private lateinit var etSecondNumber: EditText
    private lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Знаходимо елементи за їх id (класичний findViewById)
        etFirstNumber = findViewById(R.id.etFirstNumber)
        etSecondNumber = findViewById(R.id.etSecondNumber)
        tvResult = findViewById(R.id.tvResult)

        val btnPlus: Button = findViewById(R.id.btnPlus)
        val btnMinus: Button = findViewById(R.id.btnMinus)
        val btnMultiply: Button = findViewById(R.id.btnMultiply)
        val btnDivide: Button = findViewById(R.id.btnDivide)
        val btnClear: Button = findViewById(R.id.btnClear)

        // Обробники натискань
        btnPlus.setOnClickListener { calculate("+") }
        btnMinus.setOnClickListener { calculate("-") }
        btnMultiply.setOnClickListener { calculate("*") }
        btnDivide.setOnClickListener { calculate("/") }

        btnClear.setOnClickListener {
            etFirstNumber.text.clear()
            etSecondNumber.text.clear()
            tvResult.text = "Результат: "
            Log.d(TAG, "Натиснуто 'Очистити': поля та результат скинуто")
        }
    }

    private fun calculate(operation: String) {
        val firstText = etFirstNumber.text.toString().trim()
        val secondText = etSecondNumber.text.toString().trim()

        // 1. Перевірка на порожні поля
        if (firstText.isEmpty() || secondText.isEmpty()) {
            tvResult.text = "Будь ласка, введіть обидва числа"
            Log.w(TAG, "Операція '$operation': одне або обидва поля порожні")
            return
        }

        // 2. Перетворення тексту в числа (toDoubleOrNull не впаде на некоректному вводі, наприклад "-")
        val a = firstText.toDoubleOrNull()
        val b = secondText.toDoubleOrNull()
        if (a == null || b == null) {
            tvResult.text = "Будь ласка, введіть коректні числа"
            Log.w(TAG, "Операція '$operation': некоректний ввід ('$firstText', '$secondText')")
            return
        }

        Log.d(TAG, "Натиснуто '$operation': a = $a, b = $b")

        // 3. Обробка ділення на нуль
        if (operation == "/" && b == 0.0) {
            tvResult.text = "Помилка: ділення на нуль неможливе!"
            Log.w(TAG, "Спроба ділення на нуль: $a / $b")
            return
        }

        // 4. Обчислення
        val result = when (operation) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> a / b
            else -> return
        }

        tvResult.text = "Результат: ${formatNumber(result)}"
        Log.d(TAG, "Результат: $a $operation $b = $result")
    }

    // Завдання із зірочкою: 5.0 -> "5", 5.5 -> "5.5"
    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }
}