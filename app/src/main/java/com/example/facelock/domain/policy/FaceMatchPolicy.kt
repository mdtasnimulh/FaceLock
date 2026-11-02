package com.example.facelock.domain.policy

import com.example.facelock.domain.model.MatchResult
import com.example.facelock.domain.model.Similarity

/**
 * The business rules of the face login in one place.
 *
 * Same technique as the NID SDK's LivenessSessionIntegrity: never judge a face from one photo.
 * Several good frames are collected, averaged into one embedding, and only that is compared.
 *
 * MobileFaceNet cosine similarity: the same person is usually 0.7 to 1.0, a different person
 * about 0.0 to 0.4. The login gate is 80 %. If real users are rejected too often, lower
 * [MATCH_THRESHOLD] (for example to 0.70); raise it for an even stricter lock.
 */
object FaceMatchPolicy {
    /** Login needs at least this similarity (80 %). */
    const val MATCH_THRESHOLD = 0.80f

    // ---- registration ----
    /** Good frames collected while registering; their average becomes the template. */
    const val REGISTRATION_SAMPLES = 6

    /** Every registration frame must be at least this similar to the average, or the user moved. */
    const val MIN_SAMPLE_AGREEMENT = 0.75f

    // ---- login ----
    /** Good frames in one login batch (averaged before comparing). */
    const val LOGIN_BATCH_FRAMES = 4

    /** Frames of one batch must agree with their own average, otherwise the batch is discarded. */
    const val LOGIN_BATCH_CONSISTENCY = 0.60f

    /** A failed batch is repeated once before the login is rejected, so one unlucky batch never locks you out. */
    const val LOGIN_MAX_BATCHES = 2

    /** A second face must be seen in this many frames in a row before it counts (ignores one-frame glitches). */
    const val MULTIPLE_FACES_FRAMES = 2

    fun evaluate(similarity: Similarity): MatchResult = MatchResult(similarity, MATCH_THRESHOLD)
}
