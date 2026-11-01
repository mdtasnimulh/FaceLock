package com.example.facelock.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Locale

@Composable
fun HomeScreen(
    name: String,
    accuracyPercent: Float?,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Home", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(8.dp))
        Text("Hello, $name", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Face login", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = accuracyPercent
                        ?.let { "Face match accuracy: ${String.format(Locale.US, "%.1f", it)}%" }
                        ?: "Your face was just registered.",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = viewModel::lockNow, modifier = Modifier.fillMaxWidth()) { Text("Lock now") }
        OutlinedButton(onClick = viewModel::resetFaceLogin, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Reset face login")
        }
    }
}
