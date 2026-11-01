package com.example.facelock.domain.model

import kotlin.math.sqrt

/** Value object: an L2-normalised MobileFaceNet embedding. Immutable from the outside. */
class FaceEmbedding private constructor(private val values: FloatArray) {

    val dimension: Int get() = values.size

    fun toFloatArray(): FloatArray = values.copyOf()

    /** Both vectors are unit length, so the dot product is the cosine similarity. */
    fun similarityTo(other: FaceEmbedding): Similarity {
        require(dimension == other.dimension) { "Embedding dimensions differ" }
        var dot = 0f
        for (i in values.indices) {
            dot += values[i] * other.values[i]
        }
        return Similarity(dot.coerceIn(-1f, 1f))
    }

    companion object {
        private const val MIN_DIMENSION = 16

        /** Normalises any raw model output (or a stored vector) to unit length. */
        fun fromRaw(raw: FloatArray): FaceEmbedding {
            require(raw.size >= MIN_DIMENSION) { "Too small to be a face embedding: ${raw.size}" }
            var sumSquares = 0.0
            for (v in raw) {
                require(v.isFinite()) { "Embedding contains a non-finite value" }
                sumSquares += v.toDouble() * v
            }
            val norm = sqrt(sumSquares)
            require(norm > 1e-12) { "Embedding is empty" }
            return FaceEmbedding(FloatArray(raw.size) { (raw[it] / norm).toFloat() })
        }

        /** The mean of several embeddings of the same face, normalised again. */
        fun average(items: List<FaceEmbedding>): FaceEmbedding {
            require(items.isNotEmpty()) { "Nothing to average" }
            val dimension = items.first().dimension
            val sum = FloatArray(dimension)
            for (item in items) {
                require(item.dimension == dimension) { "Embedding dimensions differ" }
                for (i in 0 until dimension) {
                    sum[i] += item.values[i]
                }
            }
            return fromRaw(sum)
        }
    }
}
