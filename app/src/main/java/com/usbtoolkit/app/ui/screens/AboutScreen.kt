package com.usbtoolkit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("USB Toolkit", style = MaterialTheme.typography.headlineMedium)
            Text("Version 1.0.0")
            Spacer(Modifier.height(16.dp))
            Text(
                "A local-first USB storage utility for Android. " +
                        "All file operations stay on your device unless you explicitly start a download.\n\n" +
                        "No analytics, no ads, no automatic uploads of your files.\n\n" +
                        "Formatting of USB drives is intentionally not performed by this app — " +
                        "Android does not expose public APIs that allow third-party apps to format volumes safely. " +
                        "Use system settings or a computer for formatting.\n\n" +
                        "Open-source components:\n" +
                        "• AndroidX / Jetpack Compose (Apache 2.0)\n" +
                        "• OkHttp (Apache 2.0)\n" +
                        "• Kotlin Coroutines (Apache 2.0)"
            )
        }
    }
}
