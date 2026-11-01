package com.example.facelock.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.facelock.domain.usecase.ResetRegistrationUseCase
import com.example.facelock.presentation.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val lockManager: AppLockManager,
    private val resetRegistration: ResetRegistrationUseCase,
) : ViewModel() {

    fun lockNow() = lockManager.lock()

    fun resetFaceLogin() {
        viewModelScope.launch { resetRegistration() }
    }
}
