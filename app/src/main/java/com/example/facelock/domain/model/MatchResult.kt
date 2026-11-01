package com.example.facelock.domain.model

/** Outcome of comparing a live face with the registered one. */
data class MatchResult(
    val similarity: Similarity,
    val threshold: Float,
) {
    val isMatch: Boolean get() = similarity.cosine >= threshold
    val accuracyPercent: Float get() = similarity.percent
}
