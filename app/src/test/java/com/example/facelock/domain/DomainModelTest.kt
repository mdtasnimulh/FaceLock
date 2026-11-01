package com.example.facelock.domain

import com.example.facelock.domain.model.DisplayName
import com.example.facelock.domain.model.FaceEmbedding
import com.example.facelock.domain.policy.FaceMatchPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelTest {

    private fun vector(vararg head: Float): FloatArray =
        FloatArray(32).also { head.forEachIndexed { i, v -> it[i] = v } }

    @Test
    fun identicalFacesAreFullyAlike() {
        val a = FaceEmbedding.fromRaw(vector(1f, 2f, 3f))
        assertEquals(1f, a.similarityTo(a).cosine, 1e-5f)
        assertEquals(100f, a.similarityTo(a).percent, 1e-3f)
    }

    @Test
    fun differentFacesFailThePolicy() {
        val a = FaceEmbedding.fromRaw(vector(1f, 0f))
        val b = FaceEmbedding.fromRaw(vector(0f, 1f))
        assertFalse(FaceMatchPolicy.evaluate(a.similarityTo(b)).isMatch)
    }

    @Test
    fun closeFacesPassThePolicy() {
        val a = FaceEmbedding.fromRaw(vector(1f, 0.1f))
        val b = FaceEmbedding.fromRaw(vector(1f, 0.2f))
        assertTrue(FaceMatchPolicy.evaluate(a.similarityTo(b)).isMatch)
    }

    @Test
    fun averageIsUnitLength() {
        val avg = FaceEmbedding.average(
            listOf(FaceEmbedding.fromRaw(vector(1f, 0f)), FaceEmbedding.fromRaw(vector(0f, 1f)))
        )
        assertEquals(1f, avg.similarityTo(avg).cosine, 1e-5f)
    }

    @Test
    fun nameIsValidated() {
        assertTrue(DisplayName.of("  Rafi ").getOrThrow().value == "Rafi")
        assertTrue(DisplayName.of("   ").isFailure)
        assertTrue(DisplayName.of("x".repeat(DisplayName.MAX_LENGTH + 1)).isFailure)
    }
}
