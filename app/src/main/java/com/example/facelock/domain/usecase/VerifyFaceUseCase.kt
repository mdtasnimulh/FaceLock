package com.example.facelock.domain.usecase

import com.example.facelock.domain.model.FaceEmbedding
import com.example.facelock.domain.model.FaceImage
import com.example.facelock.domain.model.FrameAnalysis
import com.example.facelock.domain.model.MatchResult
import com.example.facelock.domain.model.ScanProgress
import com.example.facelock.domain.model.Similarity
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
 * Login, done like the NID SDK's session check: a batch of good live frames is averaged and compared
 * with the stored template. A batch whose frames disagree with each other is thrown away, and a batch
 * that does not match is repeated once before the login is rejected.
 * The returned accuracy is the best batch similarity.
 */
class VerifyFaceUseCase @Inject constructor(
    private val recognizer: FaceRecognizer,
    private val repository: UserProfileRepository,
) {
    private sealed interface Outcome {
        data object MultipleFaces : Outcome
        data class Finished(val result: MatchResult) : Outcome
    }

    suspend operator fun invoke(
        frames: Flow<FaceImage>,
        onProgress: (ScanProgress) -> Unit,
    ): Result<MatchResult> {
        return try {
            val template = (repository.current() ?: throw FaceLoginException.NotRegistered()).faceTemplate

            val batch = mutableListOf<FaceEmbedding>()
            var failedBatches = 0
            var bestCosine = -1f
            var multipleStreak = 0

            fun evaluateBatch(): Outcome? {
                val average = FaceEmbedding.average(batch)
                val consistent = batch.all {
                    it.similarityTo(average).cosine >= FaceMatchPolicy.LOGIN_BATCH_CONSISTENCY
                }
                batch.clear()
                if (!consistent) return null // the frames disagreed with each other: collect again

                val similarity = template.similarityTo(average)
                if (similarity.cosine > bestCosine) bestCosine = similarity.cosine
                if (similarity.cosine >= FaceMatchPolicy.MATCH_THRESHOLD) {
                    return Outcome.Finished(FaceMatchPolicy.evaluate(similarity))
                }

                failedBatches++
                if (failedBatches >= FaceMatchPolicy.LOGIN_MAX_BATCHES) {
                    return Outcome.Finished(FaceMatchPolicy.evaluate(Similarity(bestCosine)))
                }
                onProgress(ScanProgress.Retrying)
                return null
            }

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
                            batch += analysis.embedding
                            onProgress(ScanProgress.Collecting(batch.size, FaceMatchPolicy.LOGIN_BATCH_FRAMES))
                            if (batch.size >= FaceMatchPolicy.LOGIN_BATCH_FRAMES) evaluateBatch() else null
                        }
                    }
                }
                .filterNotNull()
                .first()

            when (outcome) {
                Outcome.MultipleFaces -> Result.failure(FaceLoginException.MultipleFaces())
                is Outcome.Finished -> Result.success(outcome.result)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
