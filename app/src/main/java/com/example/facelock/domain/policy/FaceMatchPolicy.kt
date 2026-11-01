package com.example.facelock.domain.policy

import com.example.facelock.domain.model.MatchResult
import com.example.facelock.domain.model.Similarity

/**
 * The business rules of the face login in one place.
 *
 * MobileFaceNet cosine similarity: the same person is usually 0.7 to 1.0, a different person
 * about 0.0 to 0.4. 0.60 is a sensible gate for entering an app; raise it for stricter checks,
 * lower it if real users are rejected too often.
 */
object FaceMatchPolicy {
    const val MATCH_THRESHOLD = 0.60f

    /** How many photos are taken while registering; their average becomes the template. */
    const val REGISTRATION_SAMPLES = 3

    /** Every registration photo must be at least this similar to the average, or the user moved. */
    const val MIN_SAMPLE_AGREEMENT = 0.70f

    fun evaluate(similarity: Similarity): MatchResult = MatchResult(similarity, MATCH_THRESHOLD)
}
