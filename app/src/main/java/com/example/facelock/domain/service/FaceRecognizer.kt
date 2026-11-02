package com.example.facelock.domain.service

import com.example.facelock.domain.model.FaceImage
import com.example.facelock.domain.model.FrameAnalysis

/** Port: looks at one camera frame. Implemented in the data layer (ML Kit + MobileFaceNet). */
interface FaceRecognizer {
    /** Throws [FaceLoginException.ModelFailure] when the recognition model itself fails. */
    suspend fun analyze(image: FaceImage): FrameAnalysis
}

/** Everything that can end the face flow, with a message that is safe to show to the user. */
sealed class FaceLoginException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class MultipleFaces : FaceLoginException("More than one face was found. Only one person can use the camera at a time.")
    class UnstableCapture : FaceLoginException("The scan was not steady. Hold still and try again.")
    class NotRegistered : FaceLoginException("No face is registered yet.")
    class ModelFailure(cause: Throwable) : FaceLoginException("Face recognition failed. Please try again.", cause)
}
