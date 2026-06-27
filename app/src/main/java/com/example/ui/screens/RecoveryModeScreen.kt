package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RecoveryModeScreen(errorMsg: String?) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp)
        ) {
            Text(
                "Recovery Mode", 
                style = MaterialTheme.typography.headlineLarge, 
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "The manuscript database could not be safely opened. To prevent data loss, the app has entered recovery mode and will not create a new blank database.", 
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Error details: ${errorMsg ?: "Unknown error"}", 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { /* TODO: Implement recovery export */ }) {
                Text("Export Raw Data (Not Implemented Yet)")
            }
        }
    }
}
