package com.example.facelock.data.repository

import com.example.facelock.data.local.ProfileStore
import com.example.facelock.domain.model.UserProfile
import com.example.facelock.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val store: ProfileStore,
) : UserProfileRepository {
    override fun observe(): Flow<UserProfile?> = store.observe()
    override suspend fun current(): UserProfile? = store.current()
    override suspend fun save(profile: UserProfile) = store.save(profile)
    override suspend fun clear() = store.clear()
}
