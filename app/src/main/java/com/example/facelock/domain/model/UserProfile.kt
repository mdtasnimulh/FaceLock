package com.example.facelock.domain.model

/** Aggregate root: the one registered user and the face template used to log in. */
data class UserProfile(
    val displayName: DisplayName,
    val faceTemplate: FaceEmbedding,
    val registeredAtMillis: Long,
)
