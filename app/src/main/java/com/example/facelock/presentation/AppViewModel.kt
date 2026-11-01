package com.example.facelock.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.facelock.domain.usecase.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AppUiState {
    data object Loading : AppUiState
    data object NeedsRegistration : AppUiState
    data class Locked(val name: String) : AppUiState
    data class Unlocked(val name: String, val accuracyPercent: Float?) : AppUiState
}

/** Decides which screen the user sees: register, face login, or home. */
@HiltViewModel
class AppViewModel @Inject constructor(
    observeUserProfile: ObserveUserProfileUseCase,
    lockManager: AppLockManager,
) : ViewModel() {

    val state: StateFlow<AppUiState> = combine(
        observeUserProfile(),
        lockManager.locked,
        lockManager.lastAccuracyPercent,
    ) { profile, locked, accuracy ->
        when {
            profile == null -> AppUiState.NeedsRegistration
            locked -> AppUiState.Locked(profile.displayName.value)
            else -> AppUiState.Unlocked(profile.displayName.value, accuracy)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState.Loading)
}
