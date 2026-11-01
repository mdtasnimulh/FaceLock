package com.example.facelock.domain.service

import com.example.facelock.domain.model.FaceEmbedding
import com.example.facelock.domain.model.FaceImage

/** Port: turns a photo into a face embedding. Implemented in the data layer (ML Kit + MobileFaceNet). */
interface FaceRecognizer {
    suspend fun embed(image: FaceImage): Result<FaceEmbedding>
}

/** Everything that can go wrong in the face flow, with a message that is safe to show to the user. */
sealed class FaceLoginException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NoFace : FaceLoginException("No face found. Look straight at the camera.")
    class MultipleFaces : FaceLoginException("More than one face found. Only you should be in the frame.")
    class EyesNotVisible : FaceLoginException("Keep both eyes visible and look straight at the camera.")
    class TurnedAway : FaceLoginException("Look straight at the camera.")
    class UnstableCapture : FaceLoginException("The photos did not agree. Hold still and try again.")
    class NotRegistered : FaceLoginException("No face is registered yet.")
    class ModelFailure(cause: Throwable) : FaceLoginException("Face recognition failed. Please try again.", cause)
}
