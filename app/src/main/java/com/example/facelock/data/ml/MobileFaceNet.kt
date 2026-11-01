package com.example.facelock.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Kotlin port of the NID SDK's MobileFaceNetIdentityVerifier.
 *
 * Model (assets/models/mobile_face_net.tflite): input [2,112,112,3] RGB scaled to [-1,1],
 * output [2,192]. The two batch slots are independent, so the same face goes into both and
 * slot 0 is read back.
 *
 * The face is warped so both eyes land on the standard 112x112 template positions, which removes
 * head roll and face-size differences. A plain bounding-box crop is far less stable.
 */
class MobileFaceNet(context: Context) : AutoCloseable {

    private class Spec(
        val batch: Int,
        val height: Int,
        val width: Int,
        val isFloat: Boolean,
        val outputElements: Int,
        val outputBytes: Int,
    )

    private val interpreter: Interpreter
    private val spec: Spec
    private val aligned: Bitmap
    private val canvas: Canvas
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()
    private val pixels: IntArray
    private val inputBuffer: ByteBuffer
    private val outputBuffer: ByteBuffer
    private var closed = false

    init {
        val created = Interpreter(loadModel(context), Interpreter.Options().apply { setNumThreads(NUM_THREADS) })
        spec = try {
            readSpec(created)
        } catch (e: Exception) {
            created.close()
            throw IllegalStateException("Could not initialise MobileFaceNet. Check the model file.", e)
        }
        interpreter = created
        val bytesPerChannel = if (spec.isFloat) 4 else 1
        inputBuffer = ByteBuffer
            .allocateDirect(spec.batch * spec.width * spec.height * 3 * bytesPerChannel)
            .order(ByteOrder.nativeOrder())
        outputBuffer = ByteBuffer.allocateDirect(spec.outputBytes).order(ByteOrder.nativeOrder())
        pixels = IntArray(spec.width * spec.height)
        aligned = Bitmap.createBitmap(spec.width, spec.height, Bitmap.Config.ARGB_8888)
        canvas = Canvas(aligned)
    }

    /**
     * Raw (not yet normalised) embedding of the face whose eye centres are given, or null when the
     * eyes are too close together to align reliably. Call from a background thread.
     */
    @Synchronized
    fun extractEmbedding(frame: Bitmap, eyeA: PointF, eyeB: PointF): FloatArray? {
        check(!closed) { "MobileFaceNet is closed" }

        val left = if (eyeA.x <= eyeB.x) eyeA else eyeB
        val right = if (left === eyeA) eyeB else eyeA
        val dx = right.x - left.x
        val dy = right.y - left.y
        val distance = hypot(dx, dy)
        if (!distance.isFinite() || distance < MIN_EYE_DISTANCE_PX) return null

        // similarity transform: left eye -> template left eye, right eye -> template right eye
        val targetLeftX = TEMPLATE_LEFT_EYE_X * spec.width
        val targetRightX = TEMPLATE_RIGHT_EYE_X * spec.width
        val targetY = TEMPLATE_EYE_Y * spec.height
        val scale = (targetRightX - targetLeftX) / distance
        val angleDegrees = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()

        matrix.reset()
        matrix.postTranslate(-left.x, -left.y)
        matrix.postRotate(-angleDegrees)
        matrix.postScale(scale, scale)
        matrix.postTranslate(targetLeftX, targetY)

        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(frame, matrix, paint)
        aligned.getPixels(pixels, 0, spec.width, 0, 0, spec.width, spec.height)

        inputBuffer.clear()
        repeat(spec.batch) {
            for (pixel in pixels) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                if (spec.isFloat) {
                    inputBuffer.putFloat(r / 127.5f - 1f)
                    inputBuffer.putFloat(g / 127.5f - 1f)
                    inputBuffer.putFloat(b / 127.5f - 1f)
                } else {
                    inputBuffer.put(r.toByte())
                    inputBuffer.put(g.toByte())
                    inputBuffer.put(b.toByte())
                }
            }
        }
        inputBuffer.rewind()
        outputBuffer.clear()

        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()

        // batch-major output: the first outputElements floats belong to slot 0
        return FloatArray(spec.outputElements) { outputBuffer.getFloat() }
    }

    @Synchronized
    override fun close() {
        if (!closed) {
            closed = true
            interpreter.close()
            if (!aligned.isRecycled) aligned.recycle()
        }
    }

    private fun readSpec(interpreter: Interpreter): Spec {
        check(interpreter.inputTensorCount == 1) { "Expected one input tensor" }
        check(interpreter.outputTensorCount == 1) { "Expected one output tensor" }

        val input = interpreter.getInputTensor(0)
        val shape = input.shape()
        check(shape.size == 4 && shape[0] > 0 && shape[1] > 0 && shape[2] > 0 && shape[3] == 3) {
            "Unsupported input shape ${shape.contentToString()}, expected [batch, height, width, 3]"
        }
        check(input.dataType() == DataType.FLOAT32 || input.dataType() == DataType.UINT8) {
            "Unsupported input type ${input.dataType()}"
        }

        val output = interpreter.getOutputTensor(0)
        check(output.dataType() == DataType.FLOAT32) { "Expected FLOAT32 embeddings" }
        val outShape = output.shape()
        check(outShape.size >= 2 && outShape[0] == shape[0]) {
            "Output shape ${outShape.contentToString()} must start with the batch size ${shape[0]}"
        }
        val elementsPerItem = outShape.drop(1).fold(1) { acc, n -> Math.multiplyExact(acc, n) }
        check(elementsPerItem >= 16) { "Output is too small to be a face embedding" }

        return Spec(
            batch = shape[0],
            height = shape[1],
            width = shape[2],
            isFloat = input.dataType() == DataType.FLOAT32,
            outputElements = elementsPerItem,
            outputBytes = output.numBytes(),
        )
    }

    private fun loadModel(context: Context): MappedByteBuffer =
        context.assets.openFd(MODEL_ASSET).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { stream ->
                stream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    descriptor.startOffset,
                    descriptor.declaredLength,
                )
            }
        }

    private companion object {
        const val MODEL_ASSET = "models/mobile_face_net.tflite"
        const val NUM_THREADS = 2
        const val MIN_EYE_DISTANCE_PX = 12f

        // eye positions of the standard 112x112 alignment template, as fractions of the input size
        const val TEMPLATE_LEFT_EYE_X = 38.2946f / 112f
        const val TEMPLATE_RIGHT_EYE_X = 73.5318f / 112f
        const val TEMPLATE_EYE_Y = 51.6f / 112f
    }
}
