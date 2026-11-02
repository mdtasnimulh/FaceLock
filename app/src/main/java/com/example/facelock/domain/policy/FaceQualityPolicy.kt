package com.example.facelock.domain.policy

/** Which camera frames are good enough to build an embedding from (same limits as the NID SDK). */
object FaceQualityPolicy {
    const val MAX_YAW_DEGREES = 15f
    const val MAX_PITCH_DEGREES = 20f
    const val MIN_EYE_OPEN_PROBABILITY = 0.5f
    const val MIN_EYE_DISTANCE_PX = 28f

    /** A face wider than this share of the frame is too close to align well. */
    const val MAX_FACE_WIDTH_RATIO = 0.85f
}
