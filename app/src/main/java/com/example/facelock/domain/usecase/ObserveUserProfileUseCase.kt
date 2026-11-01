package com.example.facelock.domain.usecase

import com.example.facelock.domain.model.UserProfile
import com.example.facelock.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository,
) {
    operator fun invoke(): Flow<UserProfile?> = repository.observe()
}
