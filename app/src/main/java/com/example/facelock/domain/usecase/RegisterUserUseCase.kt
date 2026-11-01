package com.example.facelock.domain.usecase

import com.example.facelock.domain.model.DisplayName
import com.example.facelock.domain.model.FaceEmbedding
import com.example.facelock.domain.model.FaceImage
import com.example.facelock.domain.model.UserProfile
import com.example.facelock.domain.policy.FaceMatchPolicy
import com.example.facelock.domain.repository.UserProfileRepository
import com.example.facelock.domain.service.FaceLoginException
import com.example.facelock.domain.service.FaceRecognizer
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/** First launch: name + several face photos -> one averaged template, stored encrypted. */
class RegisterUserUseCase @Inject constructor(
    private val recognizer: FaceRecognizer,
    private val repository: UserProfileRepository,
) {
    suspend operator fun invoke(rawName: String, photos: List<FaceImage>): Result<UserProfile> {
        return try {
            val name = DisplayName.of(rawName).getOrThrow()
            require(photos.isNotEmpty()) { "No photos to register" }

            val embeddings = photos.map { recognizer.embed(it).getOrThrow() }
            val template = FaceEmbedding.average(embeddings)

            // every photo must agree with the average, otherwise the user moved or someone else stepped in
            val agreeing = embeddings.all {
                it.similarityTo(template).cosine >= FaceMatchPolicy.MIN_SAMPLE_AGREEMENT
            }
            if (!agreeing) throw FaceLoginException.UnstableCapture()

            val profile = UserProfile(name, template, System.currentTimeMillis())
            repository.save(profile)
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
