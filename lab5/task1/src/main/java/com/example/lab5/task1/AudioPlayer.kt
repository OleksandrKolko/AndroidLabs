package com.example.lab5.task1

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

class AudioPlayer : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        Timber.d("AudioPlayer: Відтворення фонового аудіо відновлено")
    }

    override fun onStop(owner: LifecycleOwner) {
        Timber.d("AudioPlayer: Відтворення фонового аудіо поставлено на паузу")
    }
}