package com.example.facelock.domain.model

/** Value object: the name the user typed on first launch. Always valid once created. */
class DisplayName private constructor(val value: String) {

    override fun equals(other: Any?): Boolean = other is DisplayName && other.value == value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = value

    companion object {
        const val MAX_LENGTH = 40

        /** Validates user input. */
        fun of(raw: String): Result<DisplayName> {
            val trimmed = raw.trim()
            return when {
                trimmed.isEmpty() -> Result.failure(IllegalArgumentException("Please enter your name"))
                trimmed.length > MAX_LENGTH ->
                    Result.failure(IllegalArgumentException("Name must be at most $MAX_LENGTH characters"))
                else -> Result.success(DisplayName(trimmed))
            }
        }

        /** Rebuilds a name that was already validated before it was stored. */
        fun restore(stored: String): DisplayName = DisplayName(stored)
    }
}
