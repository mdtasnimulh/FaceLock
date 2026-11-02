package com.example.facelock.presentation.home

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.facelock.domain.policy.FaceMatchPolicy
import com.example.facelock.presentation.components.GradientBackground
import com.example.facelock.presentation.components.PrimaryButton
import com.example.facelock.presentation.components.ScoreCard

@Composable
fun HomeScreen(
    name: String,
    accuracyPercent: Float?,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val scheme = MaterialTheme.colorScheme

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text("Hello, $name", style = MaterialTheme.typography.titleLarge, color = scheme.onSurfaceVariant)
            Text(
                "Home",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onBackground,
            )
            Spacer(Modifier.height(28.dp))

            if (accuracyPercent != null) {
                ScoreCard(percent = accuracyPercent, thresholdPercent = FaceMatchPolicy.MATCH_THRESHOLD * 100f)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Face login is ready", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Your face was just registered. Next time, the app asks for it before opening.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            PrimaryButton(text = "Lock now", onClick = viewModel::lockNow)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = viewModel::resetFaceLogin,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Reset face login") }
        }
    }
}
