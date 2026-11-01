package com.example.facelock.domain.usecase

import com.example.facelock.domain.repository.UserProfileRepository
import javax.inject.Inject

/** Demo helper: forget the registered face so the app starts from the registration screen again. */
class ResetRegistrationUseCase @Inject constructor(
    private val repository: UserProfileRepository,
) {
    suspend operator fun invoke() = repository.clear()
}
