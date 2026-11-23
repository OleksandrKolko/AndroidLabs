package com.example.lab9.task1

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.lab9.task1.databinding.ActivityMainBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Блок 1: поточна «важка» задача
    private var calculationJob: Job? = null

    // Блок 4: глобальний обробник необроблених винятків
    private val ceh = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Глобальне перехоплення крашу: ${throwable.message}")
    }

    // Блок 4: scope із SupervisorJob + Main + ceh
    private val safeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + ceh)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnStartNoCoop.setOnClickListener { startCalculation(cooperative = false) }
        binding.btnStart.setOnClickListener { startCalculation(cooperative = true) }
        binding.btnCancel.setOnClickListener {
            Log.d(TAG, "Cancel натиснуто: calculationJob?.cancel()")
            calculationJob?.cancel()
        }

        binding.btnTestCrash.setOnClickListener { testCrash() }
        binding.btnSupervisor.setOnClickListener { testSupervisor() }
        binding.btnGlobalCrash.setOnClickListener { testGlobalHandler() }
        binding.btnNestedCrash.setOnClickListener { testNestedHandlerTrap() }
    }

    // ==================== Блок 1. Кооперативне скасування ====================

    /**
     * cooperative = false — пункт 3 (помилка): Thread.sleep не перевіряє скасування,
     *   тому після cancel() цикл доходить до 100%.
     * cooperative = true — пункт 4 (виправлення): ensureActive() після кожної ітерації
     *   кидає CancellationException, щойно корутину скасовано.
     */
    private fun startCalculation(cooperative: Boolean) {
        calculationJob?.cancel()
        calculationJob = lifecycleScope.launch(Dispatchers.Default) {
            Log.d(TAG, "Старт обробки (кооперативна = $cooperative)")
            try {
                for (i in 1..100) {
                    Thread.sleep(50)            // симуляція блокуючої роботи процесора
                    if (cooperative) {
                        ensureActive()          // точка перевірки скасування
                    }
                    Log.d(TAG, "Обробка: $i%")
                    // View.post передає оновлення в Main-потік і (на відміну від withContext)
                    // сам не перевіряє скасування — щоб не «виправити» блок без кооперації
                    binding.tvProgress.post { binding.tvProgress.text = "Прогрес: $i%" }
                }
                Log.d(TAG, "Роботу успішно завершено!")
            } catch (e: CancellationException) {
                Log.d(TAG, "Обробку скасовано користувачем!")
                throw e                         // CancellationException обов'язково прокидаємо
            }
        }
    }

    // ==================== Блок 2. Ланцюгова реакція ====================

    /**
     * Звичайний Job: перша дочірня корутина падає → скасовується батько
     * → скасовується «сестра» (стрічка новин) → виняток доходить до lifecycleScope
     * без обробника → КРАШ застосунку. Лог «Стрічка новин завантажена» не з'явиться.
     */
    private fun testCrash() {
        lifecycleScope.launch {
            Log.d(TAG, "Блок 2: старт батьківської корутини")

            launch {
                delay(1000)
                throw RuntimeException("Банер не завантажився!")
            }

            launch {
                delay(2000)
                Log.d(TAG, "Стрічка новин завантажена")   // НІКОЛИ не виконається
            }
        }
    }

    // ==================== Блок 3. supervisorScope ====================

    /**
     * supervisorScope: падіння однієї дитини не скасовує інших.
     * Але сам виняток supervisorScope не ковтає — тому перехоплюємо його try-catch
     * усередині першої корутини. Застосунок не падає, стрічка новин завантажується.
     */
    private fun testSupervisor() {
        lifecycleScope.launch {
            Log.d(TAG, "Блок 3: старт supervisorScope")
            supervisorScope {
                launch {
                    delay(1000)
                    try {
                        throw RuntimeException("Банер не завантажився!")
                    } catch (e: Exception) {
                        Log.e(TAG, "Помилка завантаження банера: ${e.message}")
                    }
                }

                launch {
                    delay(2000)
                    Log.d(TAG, "Стрічка новин завантажена")
                }
            }
            Log.d(TAG, "Блок 3: supervisorScope завершено")
        }
    }

    // ==================== Блок 4. CoroutineExceptionHandler ====================

    /**
     * safeScope має SupervisorJob і ceh. Корутина викидає виняток без try-catch,
     * але застосунок не падає: ceh — у кореневій корутині, тож він спрацьовує.
     */
    private fun testGlobalHandler() {
        safeScope.launch {
            Log.d(TAG, "Блок 4: старт у safeScope")
            delay(500)
            throw IllegalArgumentException("Ой!")
        }
    }

    // ==================== ⭐ Пастка вкладеності ====================

    /**
     * ceh передано ДОЧІРНІЙ корутині — він ІГНОРУЄТЬСЯ, і застосунок падає.
     *
     * Чому: дочірня корутина не обробляє виняток сама, а передає його батькові
     * (structured concurrency). Обробник спрацьовує лише в КОРЕНЕВІЙ корутині
     * (або в прямій дитині SupervisorJob / supervisorScope).
     * Тут коренева — lifecycleScope.launch, а в lifecycleScope обробника немає →
     * виняток стає необробленим → КРАШ. Лог із ceh не з'являється.
     */
    private fun testNestedHandlerTrap() {
        lifecycleScope.launch {
            Log.d(TAG, "⭐: старт, ceh передано у вкладену корутину")
            launch(ceh) {
                throw RuntimeException("Вкладений краш!")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Власний scope треба скасовувати вручну (lifecycleScope скасовується сам)
        safeScope.coroutineContext[Job]?.cancel()
    }

    companion object {
        const val TAG = "CoroutineErrors"
    }
}
