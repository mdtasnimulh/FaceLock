package com.example.facelock.data.ml

import android.content.Context
import com.example.facelock.domain.model.FaceEmbedding
import com.example.facelock.domain.model.FaceImage
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

/** Face detection (ML Kit) + eye alignment + MobileFaceNet embedding. */
@Singleton
class MlKitFaceRecognizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detector: FaceDetector,
) : FaceRecognizer {

    // created on first use, inside the background dispatcher below
    private val model: MobileFaceNet by lazy { MobileFaceNet(context) }

    override suspend fun embed(image: FaceImage): Result<FaceEmbedding> = withContext(Dispatchers.Default) {
        try {
            val faces = detector.process(InputImage.fromBitmap(image.bitmap, 0)).await()
            if (faces.isEmpty()) throw FaceLoginException.NoFace()
            if (faces.size > 1) throw FaceLoginException.MultipleFaces()

            val face = faces.first()
            if (abs(face.headEulerAngleY) > MAX_YAW_DEGREES) throw FaceLoginException.TurnedAway()

            val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
            val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
            if (leftEye == null || rightEye == null) throw FaceLoginException.EyesNotVisible()

            val raw = model.extractEmbedding(image.bitmap, leftEye, rightEye)
                ?: throw FaceLoginException.EyesNotVisible()
            Result.success(FaceEmbedding.fromRaw(raw))
        } catch (e: CancellationException) {
            throw e
        } catch (e: FaceLoginException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(FaceLoginException.ModelFailure(e))
        }
    }

    private companion object {
        const val MAX_YAW_DEGREES = 30f
    }
}
