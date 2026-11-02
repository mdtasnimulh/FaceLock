package com.example.facelock.presentation.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.facelock.domain.policy.FaceMatchPolicy
import com.example.facelock.presentation.camera.CameraPermissionGate
import com.example.facelock.presentation.camera.rememberFaceCameraState
import com.example.facelock.presentation.components.FaceScanner
import com.example.facelock.presentation.components.GradientBackground
import com.example.facelock.presentation.components.MessageDialog
import com.example.facelock.presentation.components.PrimaryButton
import com.example.facelock.presentation.components.QuietButton
import com.example.facelock.presentation.components.ScannerTone
import com.example.facelock.presentation.components.ScoreCard
import com.example.facelock.presentation.components.StatusPill

/** One frame every 150 ms: a login batch of 4 good frames takes well under a second. */
private const val LOGIN_FRAME_INTERVAL_MS = 150L

@Composable
fun LockScreen(name: String, viewModel: LockViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CameraPermissionGate {
        val camera = rememberFaceCameraState()

        // face unlock starts by itself as soon as the lock screen is shown
        LaunchedEffect(Unit) {
            viewModel.onShown()
            viewModel.startScan(camera.frames(LOGIN_FRAME_INTERVAL_MS))
        }

        val tone = when (state.phase) {
            LockPhase.Idle -> ScannerTone.Idle
            LockPhase.Scanning -> ScannerTone.Scanning
            LockPhase.Granted -> ScannerTone.Success
            LockPhase.Denied, LockPhase.Failed -> ScannerTone.Error
        }
        val canScanAgain = state.phase == LockPhase.Denied || state.phase == LockPhase.Failed

        GradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Welcome back",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(28.dp))

                FaceScanner(camera = camera, tone = tone, progress = state.progress)
                Spacer(Modifier.height(20.dp))
                StatusPill(text = state.hint, tone = tone)

                state.accuracyPercent?.let { accuracy ->
                    Spacer(Modifier.height(20.dp))
                    ScoreCard(
                        percent = accuracy,
                        thresholdPercent = FaceMatchPolicy.MATCH_THRESHOLD * 100f,
                    )
                }

                Spacer(Modifier.height(24.dp))
                if (canScanAgain) {
                    PrimaryButton(
                        text = "Scan again",
                        onClick = { viewModel.startScan(camera.frames(LOGIN_FRAME_INTERVAL_MS)) },
                    )
                }
                QuietButton(
                    text = "Reset face login",
                    onClick = viewModel::resetFaceLogin,
                    enabled = state.phase != LockPhase.Granted,
                )
            }
        }

        state.dialog?.let {
            MessageDialog(title = it.title, message = it.message, onDismiss = viewModel::dismissDialog)
        }
    }
}
