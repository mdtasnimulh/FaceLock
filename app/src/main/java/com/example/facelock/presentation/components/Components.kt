package com.example.facelock.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.facelock.presentation.camera.FaceCameraPreview
import com.example.facelock.presentation.camera.FaceCameraState
import com.example.facelock.presentation.theme.SuccessColor
import java.util.Locale

/** Soft gradient behind every screen. */
@Composable
fun GradientBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.primaryContainer.copy(alpha = 0.6f), colors.background, colors.background)
                )
            ),
        content = content,
    )
}

/** Simple drawn face: a friendly emblem for the first screen. */
@Composable
fun FaceGlyph(modifier: Modifier = Modifier, diameter: Dp = 112.dp) {
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    Canvas(modifier.size(diameter)) {
        drawCircle(color = container)
        val stroke = 5.dp.toPx()
        drawCircle(color = primary, radius = this.size.minDimension / 2 - stroke, style = Stroke(stroke))
        val eyeY = this.size.height * 0.42f
        val eyeDx = this.size.width * 0.14f
        drawCircle(primary, radius = 6.dp.toPx(), center = Offset(this.size.width / 2 - eyeDx, eyeY))
        drawCircle(primary, radius = 6.dp.toPx(), center = Offset(this.size.width / 2 + eyeDx, eyeY))
        drawArc(
            color = primary,
            startAngle = 25f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(this.size.width * 0.30f, this.size.height * 0.38f),
            size = Size(this.size.width * 0.40f, this.size.height * 0.34f),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

enum class ScannerTone { Idle, Scanning, Success, Error }

/** Round camera preview inside a progress ring that fills while frames are collected. */
@Composable
fun FaceScanner(
    camera: FaceCameraState,
    tone: ScannerTone,
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 288.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val ringColor by animateColorAsState(
        targetValue = when (tone) {
            ScannerTone.Idle -> scheme.outline
            ScannerTone.Scanning -> scheme.primary
            ScannerTone.Success -> SuccessColor
            ScannerTone.Error -> scheme.error
        },
        label = "ringColor",
    )
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "ringProgress")
    val track = scheme.outlineVariant

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            val sweep = if (tone == ScannerTone.Success || tone == ScannerTone.Error) 360f else 360f * animatedProgress
            drawArc(ringColor, -90f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        FaceCameraPreview(camera, Modifier.size(diameter - 36.dp).clip(CircleShape))
    }
}

/** One short sentence about what the camera needs, animated when it changes. */
@Composable
fun StatusPill(text: String, tone: ScannerTone, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val container = when (tone) {
        ScannerTone.Success -> SuccessColor.copy(alpha = 0.15f)
        ScannerTone.Error -> scheme.error.copy(alpha = 0.12f)
        else -> scheme.surfaceVariant
    }
    val content = when (tone) {
        ScannerTone.Success -> SuccessColor
        ScannerTone.Error -> scheme.error
        else -> scheme.onSurfaceVariant
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = container) {
        AnimatedContent(targetState = text, label = "status") { value ->
            Text(
                text = value,
                color = content,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

/** The big accuracy number with a bar that shows where the 80 % line is. */
@Composable
fun ScoreCard(percent: Float, thresholdPercent: Float, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val matched = percent >= thresholdPercent
    val accent = if (matched) SuccessColor else scheme.error
    val animated by animateFloatAsState(targetValue = percent.coerceIn(0f, 100f), label = "score")
    val track = scheme.outlineVariant
    val marker = scheme.onSurface

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Face match accuracy", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
            Text(
                text = String.format(Locale.US, "%.1f%%", animated),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                val radius = CornerRadius(this.size.height / 2)
                drawRoundRect(track, size = this.size, cornerRadius = radius)
                drawRoundRect(
                    accent,
                    size = Size(this.size.width * (animated / 100f), this.size.height),
                    cornerRadius = radius,
                )
                val x = this.size.width * (thresholdPercent / 100f)
                drawLine(
                    marker,
                    Offset(x, -4.dp.toPx()),
                    Offset(x, this.size.height + 4.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Needs at least ${thresholdPercent.toInt()}% to open the app",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/** Two dots: which step of the registration the user is on. */
@Composable
fun StepIndicator(current: Int, total: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            val active = index + 1 <= current
            Box(
                Modifier
                    .height(8.dp)
                    .width(if (index + 1 == current) 28.dp else 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) scheme.primary else scheme.outlineVariant)
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        } else {
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier) { Text(text) }
}

/** A message the user has to read and acknowledge (for example: more than one face found). */
@Composable
fun MessageDialog(title: String, message: String, onDismiss: () -> Unit, buttonText: String = "Got it") {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(buttonText) } },
    )
}

@Composable
fun SectionTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
