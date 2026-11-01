package com.example.facelock.presentation

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session state of the app lock. The app starts locked, and locks again every time the whole app
 * goes to the background (home button, app switcher, closing). The last successful match accuracy
 * is kept so the home screen can show it.
 */
@Singleton
class AppLockManager @Inject constructor() : DefaultLifecycleObserver {

    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private val _lastAccuracyPercent = MutableStateFlow<Float?>(null)
    val lastAccuracyPercent: StateFlow<Float?> = _lastAccuracyPercent.asStateFlow()

    init {
        // created on the main thread (first injected into a ViewModel), as addObserver requires
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** The whole app (all activities) is no longer visible. */
    override fun onStop(owner: LifecycleOwner) {
        lock()
    }

    fun lock() {
        _locked.value = true
    }

    fun unlock(accuracyPercent: Float?) {
        _lastAccuracyPercent.value = accuracyPercent
        _locked.value = false
    }
}
