package com.example.lab3.task2

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lab3.task2.databinding.ActivityMainBinding
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {

    private val TAG = "FullCalculator"

    // ViewBinding: автоматично згенерований клас з activity_main.xml
    private lateinit var binding: ActivityMainBinding

    // ===== Стан калькулятора =====
    private var firstNumber: Double? = null        // перше введене число
    private var pendingOperation: String? = null   // обрана операція: "+", "-", "*", "/"
    private var isEnteringSecondNumber = false     // чи вводиться зараз друге число
    private var startNewInput = false              // наступна цифра почне нове число (після операції або "=")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Замість setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Цифри 0-9: один обробник на всі кнопки
        val digitButtons: List<Button> = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        )
        digitButtons.forEach { button ->
            button.setOnClickListener { onDigitClick(button.text.toString()) }
        }

        // Операції
        binding.btnPlus.setOnClickListener { onOperationClick("+") }
        binding.btnMinus.setOnClickListener { onOperationClick("-") }
        binding.btnMultiply.setOnClickListener { onOperationClick("*") }
        binding.btnDivide.setOnClickListener { onOperationClick("/") }

        // Інші кнопки
        binding.btnEquals.setOnClickListener { onEqualsClick() }
        binding.btnClear.setOnClickListener { onClearClick() }
        binding.btnDot.setOnClickListener { onDotClick() }
        binding.btnBackspace.setOnClickListener { onBackspaceClick() }
    }

    // ===== Цифри =====
    private fun onDigitClick(digit: String) {
        val current = binding.tvDisplay.text.toString()
        binding.tvDisplay.text =
            if (startNewInput || current == "0" || current == ERROR) digit
            else current + digit
        startNewInput = false
        Log.d(TAG, "Цифра $digit -> екран: ${binding.tvDisplay.text}")
    }

    // ===== Крапка (⭐): не більше однієї в числі =====
    private fun onDotClick() {
        val current = binding.tvDisplay.text.toString()
        when {
            startNewInput || current == ERROR -> binding.tvDisplay.text = "0."
            !current.contains(".") -> binding.tvDisplay.text = "$current."
            else -> Log.w(TAG, "Спроба ввести другу крапку в числі $current")
        }
        startNewInput = false
    }

    // ===== Операції + - * / =====
    private fun onOperationClick(operation: String) {
        val current = binding.tvDisplay.text.toString().toDoubleOrNull() ?: return

        // ⭐ Безперервні обчислення: 2 + 3 + ... одразу показує проміжний результат 5
        if (isEnteringSecondNumber && !startNewInput) {
            val intermediate = calculate(firstNumber!!, current, pendingOperation!!)
            if (intermediate == null) {
                showError()
                return
            }
            binding.tvDisplay.text = formatNumber(intermediate)
            firstNumber = intermediate
        } else if (!isEnteringSecondNumber) {
            firstNumber = current
        }
        // Якщо операцію натиснули двічі поспіль, просто змінюємо її

        pendingOperation = operation
        isEnteringSecondNumber = true
        startNewInput = true
        Log.d(TAG, "Операція '$operation', перше число = $firstNumber")
    }

    // ===== Дорівнює =====
    private fun onEqualsClick() {
        val first = firstNumber
        val operation = pendingOperation
        val second = binding.tvDisplay.text.toString().toDoubleOrNull()

        if (first == null || operation == null || second == null) {
            Log.w(TAG, "'=' натиснуто без повного виразу")
            return
        }

        val result = calculate(first, second, operation)
        if (result == null) {
            showError()
            return
        }

        binding.tvDisplay.text = formatNumber(result)
        Log.d(TAG, "Результат: $first $operation $second = $result")

        firstNumber = null
        pendingOperation = null
        isEnteringSecondNumber = false
        startNewInput = true
    }

    // ===== C: повне очищення =====
    private fun onClearClick() {
        binding.tvDisplay.text = "0"
        firstNumber = null
        pendingOperation = null
        isEnteringSecondNumber = false
        startNewInput = false
        Log.d(TAG, "Очищено")
    }

    // ===== ⌫ (⭐): видалення останнього символу =====
    private fun onBackspaceClick() {
        if (startNewInput) return  // результат/проміжне значення не редагуємо
        val current = binding.tvDisplay.text.toString()
        if (current == ERROR) {
            onClearClick()
            return
        }
        val newText = current.dropLast(1)
        binding.tvDisplay.text = if (newText.isEmpty() || newText == "-") "0" else newText
    }

    // ===== Обчислення; null = ділення на нуль =====
    private fun calculate(a: Double, b: Double, operation: String): Double? {
        return when (operation) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b == 0.0) null else a / b
            else -> null
        }
    }

    private fun showError() {
        binding.tvDisplay.text = ERROR
        Log.w(TAG, "Помилка: ділення на нуль")
        firstNumber = null
        pendingOperation = null
        isEnteringSecondNumber = false
        startNewInput = true
    }

    // 5.0 -> "5", 0.1 + 0.2 -> "0.3" (прибирає хвости на кшталт 0.30000000000000004)
    private fun formatNumber(value: Double): String {
        return BigDecimal.valueOf(value)
            .setScale(10, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    companion object {
        private const val ERROR = "Помилка"
    }
}