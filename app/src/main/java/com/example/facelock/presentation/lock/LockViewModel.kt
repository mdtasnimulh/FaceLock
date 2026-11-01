package com.example.facelock.presentation.lock

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.facelock.domain.model.FaceImage
import com.example.facelock.domain.usecase.ResetRegistrationUseCase
import com.example.facelock.domain.usecase.VerifyFaceUseCase
import com.example.facelock.presentation.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LockUiState {
    data object Idle : LockUiState
    data object Verifying : LockUiState

    /** Matched: the accuracy is shown for a moment, then the app opens. */
    data class Granted(val accuracyPercent: Float) : LockUiState

    /** Not matched: the accuracy is still shown, so the user sees how close it was. */
    data class Denied(val accuracyPercent: Float) : LockUiState

    data class Failed(val message: String) : LockUiState
}

@HiltViewModel
class LockViewModel @Inject constructor(
    private val verifyFace: VerifyFaceUseCase,
    private val resetRegistration: ResetRegistrationUseCase,
    private val lockManager: AppLockManager,
) : ViewModel() {

    private val _state = MutableStateFlow<LockUiState>(LockUiState.Idle)
    val state: StateFlow<LockUiState> = _state.asStateFlow()

    /** The lock screen appears (again): forget the previous attempt. */
    fun onShown() {
        _state.value = LockUiState.Idle
    }

    fun verify(takePhoto: suspend () -> Bitmap) {
        val current = _state.value
        if (current is LockUiState.Verifying || current is LockUiState.Granted) return

        viewModelScope.launch {
            _state.value = LockUiState.Verifying
            try {
                val photo = FaceImage(takePhoto())
                verifyFace(photo).fold(
                    onSuccess = { match ->
                        if (match.isMatch) {
                            _state.value = LockUiState.Granted(match.accuracyPercent)
                            delay(SHOW_RESULT_MS)
                            lockManager.unlock(match.accuracyPercent)
                        } else {
                            _state.value = LockUiState.Denied(match.accuracyPercent)
                        }
                    },
                    onFailure = { e -> _state.value = LockUiState.Failed(e.message ?: "Face check failed") },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = LockUiState.Failed("Could not use the camera. Please try again.")
            }
        }
    }

    /** Demo helper: forget the registered face and start again from the registration screen. */
    fun resetFaceLogin() {
        viewModelScope.launch { resetRegistration() }
    }

    private companion object {
        const val SHOW_RESULT_MS = 1_500L
    }
}
