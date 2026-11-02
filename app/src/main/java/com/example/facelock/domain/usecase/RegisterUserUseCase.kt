package com.example.facelock.domain.usecase

import com.example.facelock.domain.model.DisplayName
import com.example.facelock.domain.model.FaceEmbedding
import com.example.facelock.domain.model.FaceImage
import com.example.facelock.domain.model.FrameAnalysis
import com.example.facelock.domain.model.ScanProgress
import com.example.facelock.domain.model.UserProfile
import com.example.facelock.domain.policy.FaceMatchPolicy
import com.example.facelock.domain.repository.UserProfileRepository
import com.example.facelock.domain.service.FaceLoginException
import com.example.facelock.domain.service.FaceRecognizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * First launch: collects several GOOD live frames (right pose, eyes open, one face), averages their
 * embeddings into one template and stores it encrypted. Stops with an error if a second person
 * steps into the frame.
 */
class RegisterUserUseCase @Inject constructor(
    private val recognizer: FaceRecognizer,
    private val repository: UserProfileRepository,
) {
    private sealed interface Outcome {
        data object MultipleFaces : Outcome
        data object Enough : Outcome
    }

    suspend operator fun invoke(
        rawName: String,
        frames: Flow<FaceImage>,
        onProgress: (ScanProgress) -> Unit,
    ): Result<UserProfile> {
        return try {
            val name = DisplayName.of(rawName).getOrThrow()
            val target = FaceMatchPolicy.REGISTRATION_SAMPLES
            val samples = mutableListOf<FaceEmbedding>()
            var multipleStreak = 0

            val outcome = frames
                .map { recognizer.analyze(it) }
                .map { analysis ->
                    if (analysis is FrameAnalysis.MultipleFaces) multipleStreak++ else multipleStreak = 0
                    when (analysis) {
                        FrameAnalysis.NoFace -> {
                            onProgress(ScanProgress.Guidance(null))
                            null
                        }
                        is FrameAnalysis.MultipleFaces ->
                            if (multipleStreak >= FaceMatchPolicy.MULTIPLE_FACES_FRAMES) Outcome.MultipleFaces else null
                        is FrameAnalysis.Unusable -> {
                            onProgress(ScanProgress.Guidance(analysis.issue))
                            null
                        }
                        is FrameAnalysis.Good -> {
                            samples += analysis.embedding
                            onProgress(ScanProgress.Collecting(samples.size, target))
                            if (samples.size >= target) Outcome.Enough else null
                        }
                    }
                }
                .filterNotNull()
                .first()

            if (outcome == Outcome.MultipleFaces) throw FaceLoginException.MultipleFaces()

            val template = FaceEmbedding.average(samples)
            // every frame must agree with the average, otherwise the user moved or someone else stepped in
            val steady = samples.all { it.similarityTo(template).cosine >= FaceMatchPolicy.MIN_SAMPLE_AGREEMENT }
            if (!steady) throw FaceLoginException.UnstableCapture()

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
