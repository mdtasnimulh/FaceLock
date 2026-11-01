package com.example.facelock.domain.repository

import androidx.datastore.preferences.core.Preferences
import com.example.facelock.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    /** Emits the registered user, or null when nobody has registered yet. */
    fun observe(): Flow<UserProfile?>

    suspend fun current(): UserProfile?

    suspend fun save(profile: UserProfile): Preferences

    suspend fun clear()
}
