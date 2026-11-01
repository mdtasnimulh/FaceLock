package com.example.facelock.presentation.register

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.facelock.domain.model.DisplayName
import com.example.facelock.domain.model.FaceImage
import com.example.facelock.domain.policy.FaceMatchPolicy
import com.example.facelock.domain.usecase.RegisterUserUseCase
import com.example.facelock.presentation.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RegisterStep { Name, Face }

data class RegisterUiState(
    val name: String = "",
    val nameError: String? = null,
    val step: RegisterStep = RegisterStep.Name,
    val busy: Boolean = false,
    val progress: String? = null,
    val error: String? = null,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUser: RegisterUserUseCase,
    private val lockManager: AppLockManager,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    /** The screen is shown again (for example after a reset): start from a clean form. */
    fun onShown() {
        _state.value = RegisterUiState()
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = null) }

    fun onContinue() {
        DisplayName.of(_state.value.name).fold(
            onSuccess = { name -> _state.update { it.copy(name = name.value, step = RegisterStep.Face, error = null) } },
            onFailure = { e -> _state.update { it.copy(nameError = e.message) } },
        )
    }

    fun onBack() = _state.update { it.copy(step = RegisterStep.Name, error = null, progress = null) }

    /** Takes [RegistrationPhotos] photos with [takePhoto] and registers the user with them. */
    fun register(takePhoto: suspend () -> Bitmap) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                val photos = mutableListOf<FaceImage>()
                repeat(FaceMatchPolicy.REGISTRATION_SAMPLES) { index ->
                    _state.update { it.copy(progress = "Hold still… ${index + 1}/${FaceMatchPolicy.REGISTRATION_SAMPLES}") }
                    delay(SAMPLE_GAP_MS)
                    photos += FaceImage(takePhoto())
                }
                _state.update { it.copy(progress = "Saving securely…") }

                registerUser(_state.value.name, photos).fold(
                    onSuccess = {
                        // registered: go straight in, no need to log in right after registering
                        lockManager.unlock(accuracyPercent = null)
                    },
                    onFailure = { e -> _state.update { it.copy(error = e.message ?: "Registration failed") } },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "Could not use the camera. Please try again.") }
            } finally {
                _state.update { it.copy(busy = false, progress = null) }
            }
        }
    }

    private companion object {
        const val SAMPLE_GAP_MS = 450L
    }
}
