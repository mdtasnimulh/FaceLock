package com.example.facelock.data.ml

import android.content.Context
import com.example.facelock.domain.model.FaceEmbedding
import com.example.facelock.domain.model.FaceImage
import com.example.facelock.domain.model.FrameAnalysis
import com.example.facelock.domain.model.FrameIssue
import com.example.facelock.domain.policy.FaceQualityPolicy
import com.example.facelock.domain.service.FaceLoginException
import com.example.facelock.domain.service.FaceRecognizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceLandmark
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.hypot

/**
 * One camera frame -> [FrameAnalysis]. A frame only becomes an embedding when it passes the same
 * checks the NID SDK uses before it trusts a face: one face, looking at the camera, eyes open,
 * big enough, not too close.
 */
@Singleton
class MlKitFaceRecognizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detector: FaceDetector,
) : FaceRecognizer {

    // created on first use, inside the background dispatcher below
    private val model: MobileFaceNet by lazy { MobileFaceNet(context) }

    override suspend fun analyze(image: FaceImage): FrameAnalysis = withContext(Dispatchers.Default) {
        try {
            val faces = detector.process(InputImage.fromBitmap(image.bitmap, 0)).await()
            when {
                faces.isEmpty() -> FrameAnalysis.NoFace
                faces.size > 1 -> FrameAnalysis.MultipleFaces(faces.size)
                else -> analyzeSingleFace(image, faces.first())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw FaceLoginException.ModelFailure(e)
        }
    }

    private fun analyzeSingleFace(image: FaceImage, face: com.google.mlkit.vision.face.Face): FrameAnalysis {
        val left = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val right = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        if (left == null || right == null) return FrameAnalysis.Unusable(FrameIssue.EyesNotVisible)

        if (hypot(left.x - right.x, left.y - right.y) < FaceQualityPolicy.MIN_EYE_DISTANCE_PX) {
            return FrameAnalysis.Unusable(FrameIssue.TooFar)
        }
        if (face.boundingBox.width() > image.bitmap.width * FaceQualityPolicy.MAX_FACE_WIDTH_RATIO) {
            return FrameAnalysis.Unusable(FrameIssue.TooClose)
        }
        if (abs(face.headEulerAngleY) > FaceQualityPolicy.MAX_YAW_DEGREES ||
            abs(face.headEulerAngleX) > FaceQualityPolicy.MAX_PITCH_DEGREES
        ) {
            return FrameAnalysis.Unusable(FrameIssue.TurnedAway)
        }
        val leftOpen = face.leftEyeOpenProbability
        val rightOpen = face.rightEyeOpenProbability
        // no classification available -> don't block on it
        if ((leftOpen != null && leftOpen < FaceQualityPolicy.MIN_EYE_OPEN_PROBABILITY) ||
            (rightOpen != null && rightOpen < FaceQualityPolicy.MIN_EYE_OPEN_PROBABILITY)
        ) {
            return FrameAnalysis.Unusable(FrameIssue.EyesClosed)
        }

        val raw = model.extractEmbedding(image.bitmap, left, right)
            ?: return FrameAnalysis.Unusable(FrameIssue.EyesNotVisible)
        return FrameAnalysis.Good(FaceEmbedding.fromRaw(raw))
    }
}
