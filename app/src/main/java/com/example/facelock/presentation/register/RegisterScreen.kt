package com.example.facelock.presentation.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.facelock.presentation.camera.CameraPermissionGate
import com.example.facelock.presentation.camera.rememberFaceCameraState
import com.example.facelock.presentation.components.FaceGlyph
import com.example.facelock.presentation.components.FaceScanner
import com.example.facelock.presentation.components.GradientBackground
import com.example.facelock.presentation.components.MessageDialog
import com.example.facelock.presentation.components.PrimaryButton
import com.example.facelock.presentation.components.QuietButton
import com.example.facelock.presentation.components.ScannerTone
import com.example.facelock.presentation.components.SectionTitle
import com.example.facelock.presentation.components.StatusPill
import com.example.facelock.presentation.components.StepIndicator

/** One frame every 250 ms while registering, so the 6 samples are spread over about 1.5 seconds. */
private const val REGISTER_FRAME_INTERVAL_MS = 250L

@Composable
fun RegisterScreen(viewModel: RegisterViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onShown() }

    GradientBackground {
        when (state.step) {
            RegisterStep.Name -> NameStep(
                name = state.name,
                error = state.nameError,
                onNameChange = viewModel::onNameChange,
                onContinue = viewModel::onContinue,
            )
            RegisterStep.Face -> CameraPermissionGate {
                FaceStep(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun NameStep(
    name: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepIndicator(current = 1, total = 2)
        Spacer(Modifier.height(32.dp))
        FaceGlyph()
        Spacer(Modifier.height(28.dp))
        SectionTitle(
            title = "Set up face login",
            subtitle = "Your face replaces a password. Everything stays on this phone, encrypted.",
        )
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("What should we call you?") },
            singleLine = true,
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onContinue() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = "Continue", onClick = onContinue)
    }
}

@Composable
private fun FaceStep(state: RegisterUiState, viewModel: RegisterViewModel) {
    val camera = rememberFaceCameraState()
    val tone = when {
        state.failed -> ScannerTone.Error
        state.scanning -> ScannerTone.Scanning
        else -> ScannerTone.Idle
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepIndicator(current = 2, total = 2)
        Spacer(Modifier.height(24.dp))
        SectionTitle(
            title = "Hi, ${state.name}",
            subtitle = "Look straight at the camera, in good light. Only you should be in the frame.",
        )
        Spacer(Modifier.height(28.dp))

        FaceScanner(camera = camera, tone = tone, progress = state.progress)
        Spacer(Modifier.height(20.dp))
        StatusPill(text = state.hint, tone = tone)

        Spacer(Modifier.height(28.dp))
        if (state.scanning) {
            PrimaryButton(text = "Cancel", onClick = viewModel::cancelScan)
        } else {
            PrimaryButton(
                text = if (state.failed) "Try again" else "Start scanning",
                onClick = { viewModel.startScan(camera.frames(REGISTER_FRAME_INTERVAL_MS)) },
            )
        }
        QuietButton(text = "Back", onClick = viewModel::onBack, enabled = !state.scanning)
    }

    state.dialog?.let {
        MessageDialog(title = it.title, message = it.message, onDismiss = viewModel::dismissDialog)
    }
}
