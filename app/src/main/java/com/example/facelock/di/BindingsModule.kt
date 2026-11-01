package com.example.facelock.di

import com.example.facelock.data.ml.MlKitFaceRecognizer
import com.example.facelock.data.repository.UserProfileRepositoryImpl
import com.example.facelock.data.security.SecureCipher
import com.example.facelock.data.security.TinkSecureCipher
import com.example.facelock.domain.repository.UserProfileRepository
import com.example.facelock.domain.service.FaceRecognizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Wires every domain interface to its data-layer implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository

    @Binds
    abstract fun bindFaceRecognizer(impl: MlKitFaceRecognizer): FaceRecognizer

    @Binds
    abstract fun bindSecureCipher(impl: TinkSecureCipher): SecureCipher
}
