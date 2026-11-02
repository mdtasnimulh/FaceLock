package com.example.facelock.presentation.lock

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.facelock.domain.model.FaceImage
import com.example.facelock.domain.model.ScanProgress
import com.example.facelock.domain.policy.FaceMatchPolicy
import com.example.facelock.domain.usecase.ResetRegistrationUseCase
import com.example.facelock.domain.usecase.VerifyFaceUseCase
import com.example.facelock.presentation.AppLockManager
import com.example.facelock.presentation.common.ScanDialogState
import com.example.facelock.presentation.common.toDialogOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

enum class LockPhase { Idle, Scanning, Granted, Denied, Failed }

data class LockUiState(
    val phase: LockPhase = LockPhase.Idle,
    val hint: String = "Look at the camera",
    val collected: Int = 0,
    val target: Int = FaceMatchPolicy.LOGIN_BATCH_FRAMES,
    /** Best similarity of the last scan, shown after every attempt. */
    val accuracyPercent: Float? = null,
    val dialog: ScanDialogState? = null,
) {
    val progress: Float get() = if (target == 0) 0f else collected.toFloat() / target
}

@HiltViewModel
class LockViewModel @Inject constructor(
    private val verifyFace: VerifyFaceUseCase,
    private val resetRegistration: ResetRegistrationUseCase,
    private val lockManager: AppLockManager,
) : ViewModel() {

    private val _state = MutableStateFlow(LockUiState())
    val state: StateFlow<LockUiState> = _state.asStateFlow()

    private var scanJob: Job? = null

    /** The lock screen appears (again): forget the previous attempt. */
    fun onShown() {
        scanJob?.cancel()
        _state.value = LockUiState()
    }

    fun dismissDialog() = _state.update { it.copy(dialog = null) }

    /** Face unlock: collects good frames, averages them and compares with the stored face. */
    fun startScan(frames: Flow<FaceImage>) {
        if (scanJob?.isActive == true || _state.value.phase == LockPhase.Granted) return
        scanJob = viewModelScope.launch {
            _state.update {
                it.copy(phase = LockPhase.Scanning, collected = 0, hint = "Looking for your face…", dialog = null)
            }
            try {
                val result = withTimeoutOrNull(SCAN_TIMEOUT_MS) { verifyFace(frames, ::onProgress) }
                when {
                    result == null -> _state.update {
                        it.copy(phase = LockPhase.Failed, collected = 0, hint = "We could not see your face. Try again.")
                    }
                    result.isSuccess -> {
                        val match = result.getOrThrow()
                        Log.d(TAG, "similarity=${match.similarity.cosine} match=${match.isMatch}")
                        if (match.isMatch) {
                            _state.update {
                                it.copy(phase = LockPhase.Granted, accuracyPercent = match.accuracyPercent, hint = "Face recognised")
                            }
                            delay(SHOW_RESULT_MS)
                            lockManager.unlock(match.accuracyPercent)
                        } else {
                            _state.update {
                                it.copy(phase = LockPhase.Denied, accuracyPercent = match.accuracyPercent, hint = "Face not recognised")
                            }
                        }
                    }
                    else -> {
                        val error = result.exceptionOrNull()
                        _state.update {
                            it.copy(
                                phase = LockPhase.Failed,
                                collected = 0,
                                hint = error?.message ?: "Face check failed",
                                dialog = error.toDialogOrNull(),
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    private fun onProgress(progress: ScanProgress) {
        _state.update {
            when (progress) {
                is ScanProgress.Guidance -> it.copy(hint = progress.issue?.hint ?: "Looking for your face…")
                is ScanProgress.Collecting -> it.copy(
                    collected = progress.collected,
                    target = progress.target,
                    hint = "Hold still…",
                )
                ScanProgress.Retrying -> it.copy(collected = 0, hint = "Checking once more…")
            }
        }
    }

    /** Demo helper: forget the registered face and start again from the registration screen. */
    fun resetFaceLogin() {
        scanJob?.cancel()
        viewModelScope.launch { resetRegistration() }
    }

    private companion object {
        const val TAG = "FaceLock"
        const val SCAN_TIMEOUT_MS = 20_000L
        const val SHOW_RESULT_MS = 1_500L
    }
}
