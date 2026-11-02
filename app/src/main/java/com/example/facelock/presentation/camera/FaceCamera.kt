package com.example.facelock.presentation.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.facelock.domain.model.FaceImage
import com.example.facelock.presentation.components.FaceGlyph
import com.example.facelock.presentation.components.GradientBackground
import com.example.facelock.presentation.components.PrimaryButton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** The front camera as a live stream of upright frames. */
class FaceCameraState internal constructor(
    internal val controller: LifecycleCameraController,
    private val executor: ExecutorService,
) {
    /**
     * Frames at most every [minIntervalMs]. Collecting starts the analysis, cancelling the
     * collector stops it. Slow consumers drop old frames instead of building up a queue.
     */
    fun frames(minIntervalMs: Long): Flow<FaceImage> = callbackFlow {
        var lastMs = 0L
        controller.setImageAnalysisAnalyzer(executor) { proxy ->
            val now = SystemClock.elapsedRealtime()
            if (now - lastMs >= minIntervalMs) {
                lastMs = now
                val bitmap = runCatching { proxy.toBitmap().rotated(proxy.imageInfo.rotationDegrees) }.getOrNull()
                if (bitmap != null) trySend(FaceImage(bitmap))
            }
            proxy.close()
        }
        awaitClose { controller.clearImageAnalysisAnalyzer() }
    }.buffer(Channel.CONFLATED)
}

@Composable
fun rememberFaceCameraState(): FaceCameraState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            imageAnalysisOutputImageFormat = ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
        }
    }
    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            executor.shutdown()
        }
    }
    return remember(controller, executor) { FaceCameraState(controller, executor) }
}

@Composable
fun FaceCameraPreview(state: FaceCameraState, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                controller = state.controller
            }
        },
    )
}

/** Shows [content] only once the camera permission is granted; asks for it otherwise. */
@Composable
fun CameraPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    if (granted) {
        content()
    } else {
        GradientBackground {
            Column(
                modifier = Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FaceGlyph()
                Spacer(Modifier.height(24.dp))
                Text(
                    "Camera access needed",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "The camera is only used on this device to register and check your face.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                PrimaryButton("Allow camera", onClick = { launcher.launch(Manifest.permission.CAMERA) })
            }
        }
    }
}

private fun Bitmap.rotated(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
