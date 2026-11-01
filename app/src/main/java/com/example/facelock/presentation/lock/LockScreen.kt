package com.example.facelock.presentation.lock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.facelock.domain.policy.FaceMatchPolicy
import com.example.facelock.presentation.camera.CameraPermissionGate
import com.example.facelock.presentation.camera.FaceCameraPreview
import com.example.facelock.presentation.camera.rememberFaceCameraState
import java.util.Locale

private val Green = Color(0xFF2E7D32)

@Composable
fun LockScreen(name: String, viewModel: LockViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onShown() }

    CameraPermissionGate {
        val camera = rememberFaceCameraState()
        val ringColor = when (state) {
            is LockUiState.Granted -> Green
            is LockUiState.Denied, is LockUiState.Failed -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.outline
        }

        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Welcome back, $name", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Look at the camera to open the app.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))

            FaceCameraPreview(
                state = camera,
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .border(BorderStroke(4.dp, ringColor), CircleShape),
            )
            Spacer(Modifier.height(16.dp))

            ResultText(state)

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.verify { camera.capture() } },
                enabled = state !is LockUiState.Verifying && state !is LockUiState.Granted,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state is LockUiState.Denied || state is LockUiState.Failed) "Try again" else "Verify face") }

            TextButton(
                onClick = viewModel::resetFaceLogin,
                enabled = state !is LockUiState.Verifying && state !is LockUiState.Granted,
            ) { Text("Reset face login") }
        }
    }
}

@Composable
private fun ResultText(state: LockUiState) {
    val threshold = (FaceMatchPolicy.MATCH_THRESHOLD * 100).toInt()
    when (state) {
        LockUiState.Idle -> Text(" ")
        LockUiState.Verifying -> CircularProgressIndicator()
        is LockUiState.Granted -> Text(
            "Match ${percent(state.accuracyPercent)} - opening…",
            color = Green,
            style = MaterialTheme.typography.titleMedium,
        )
        is LockUiState.Denied -> Text(
            "Not matched: ${percent(state.accuracyPercent)} (needs at least $threshold%)",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        is LockUiState.Failed -> Text(
            state.message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

private fun percent(value: Float): String = String.format(Locale.US, "%.1f%%", value)
