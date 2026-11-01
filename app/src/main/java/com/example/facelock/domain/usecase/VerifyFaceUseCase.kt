package com.example.facelock.domain.usecase

import com.example.facelock.domain.model.FaceImage
import com.example.facelock.domain.model.MatchResult
import com.example.facelock.domain.policy.FaceMatchPolicy
import com.example.facelock.domain.repository.UserProfileRepository
import com.example.facelock.domain.service.FaceLoginException
import com.example.facelock.domain.service.FaceRecognizer
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/** Login: compares the live face with the stored template and returns the similarity. */
class VerifyFaceUseCase @Inject constructor(
    private val recognizer: FaceRecognizer,
    private val repository: UserProfileRepository,
) {
    suspend operator fun invoke(photo: FaceImage): Result<MatchResult> {
        return try {
            val profile = repository.current() ?: throw FaceLoginException.NotRegistered()
            val live = recognizer.embed(photo).getOrThrow()
            val similarity = profile.faceTemplate.similarityTo(live)
            Result.success(FaceMatchPolicy.evaluate(similarity))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
