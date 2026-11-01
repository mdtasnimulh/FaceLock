package com.example.facelock.domain.model

/**
 * Cosine similarity of two L2-normalised face embeddings (-1..1).
 * [percent] is the number shown to the user: 0 % (different) to 100 % (identical).
 * It is a similarity score, not a statistical accuracy.
 */
@JvmInline
value class Similarity(val cosine: Float) {
    val percent: Float get() = cosine.coerceIn(0f, 1f) * 100f
}
