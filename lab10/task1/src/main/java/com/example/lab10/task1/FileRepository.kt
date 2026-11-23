package com.example.lab10.task1

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Шар Model: імітує завантаження файлу з мережі.
 * Нічого не знає про UI, ViewModel чи LiveData.
 */
class FileRepository {

    /**
     * Холодний потік відсотків 0..100 з кроком 5%.
     * Код усередині flow { } запускається заново для кожного collect.
     */
    fun downloadFile(): Flow<Int> = flow {
        for (progress in 0..100 step 5) {
            Log.d(TAG, "emit $progress% (потік: ${Thread.currentThread().name})")
            emit(progress)
            if (progress < 100) delay(150)   // імітація мережевого запиту
        }
    }.flowOn(Dispatchers.IO)                 // емісія виконується у фоновому IO-потоці

    companion object {
        const val TAG = "FileDownload"
    }
}
