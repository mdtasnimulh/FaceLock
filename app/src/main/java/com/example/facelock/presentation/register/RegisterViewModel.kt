package com.example.facelock.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.facelock.domain.model.DisplayName
import com.example.facelock.domain.model.FaceImage
import com.example.facelock.domain.model.ScanProgress
import com.example.facelock.domain.policy.FaceMatchPolicy
import com.example.facelock.domain.usecase.RegisterUserUseCase
import com.example.facelock.presentation.AppLockManager
import com.example.facelock.presentation.common.ScanDialogState
import com.example.facelock.presentation.common.toDialogOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

enum class RegisterStep { Name, Face }

data class RegisterUiState(
    val name: String = "",
    val nameError: String? = null,
    val step: RegisterStep = RegisterStep.Name,
    val scanning: Boolean = false,
    val collected: Int = 0,
    val target: Int = FaceMatchPolicy.REGISTRATION_SAMPLES,
    val hint: String = DEFAULT_HINT,
    val failed: Boolean = false,
    val dialog: ScanDialogState? = null,
) {
    val progress: Float get() = if (target == 0) 0f else collected.toFloat() / target

    companion object {
        const val DEFAULT_HINT = "Place your face inside the circle"
    }
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUser: RegisterUserUseCase,
    private val lockManager: AppLockManager,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    private var scanJob: Job? = null

    /** The screen is shown again (for example after a reset): start from a clean form. */
    fun onShown() {
        scanJob?.cancel()
        _state.value = RegisterUiState()
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = null) }

    fun onContinue() {
        DisplayName.of(_state.value.name).fold(
            onSuccess = { name -> _state.update { it.copy(name = name.value, step = RegisterStep.Face) } },
            onFailure = { e -> _state.update { it.copy(nameError = e.message) } },
        )
    }

    fun onBack() {
        cancelScan()
        _state.update { it.copy(step = RegisterStep.Name) }
    }

    fun dismissDialog() = _state.update { it.copy(dialog = null) }

    fun cancelScan() {
        scanJob?.cancel()
        _state.update { it.copy(scanning = false, collected = 0, hint = RegisterUiState.DEFAULT_HINT) }
    }

    /** Starts the live scan: good frames fill the ring; at 100 % the face is saved and the app opens. */
    fun startScan(frames: Flow<FaceImage>) {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            _state.update { it.copy(scanning = true, collected = 0, failed = false, hint = "Looking for your face…") }
            try {
                val result = withTimeoutOrNull(SCAN_TIMEOUT_MS) {
                    registerUser(_state.value.name, frames, ::onProgress)
                }
                when {
                    result == null -> _state.update {
                        it.copy(collected = 0, failed = true, hint = "We could not see your face. Try again.")
                    }
                    result.isSuccess -> lockManager.unlock(accuracyPercent = null)
                    else -> {
                        val error = result.exceptionOrNull()
                        _state.update {
                            it.copy(
                                collected = 0,
                                failed = true,
                                hint = error?.message ?: "Registration failed",
                                dialog = error.toDialogOrNull(),
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _state.update { it.copy(scanning = false) }
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
                ScanProgress.Retrying -> it
            }
        }
    }

    private companion object {
        const val SCAN_TIMEOUT_MS = 30_000L
    }
}
