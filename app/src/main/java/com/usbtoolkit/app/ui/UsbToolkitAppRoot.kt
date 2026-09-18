package com.usbtoolkit.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usbtoolkit.app.util.FileUtils
import com.usbtoolkit.app.viewmodel.MainViewModel

@Composable
fun UsbToolkitAppRoot(viewModel: MainViewModel) {
    val drive by viewModel.primaryDrive.collectAsState()
    val settings by viewModel.settings.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshUsb()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "USB Toolkit",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Professional USB storage utility",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            if (drive != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Usb,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = drive!!.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (drive!!.isReadOnly) "Read-only" else "Connected"
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { drive!!.usedPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total: ${FileUtils.formatSize(drive!!.totalBytes)}")
                            Text("Free: ${FileUtils.formatSize(drive!!.freeBytes)}")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Usb,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No USB drive connected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Connect a USB flash drive via OTG or USB-C.",
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.refreshUsb() }) {
                            Text("Refresh")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Theme: ${settings.themeMode}")
            Spacer(Modifier.height(8.dp))
            Text(
                "Formatting is not supported by third-party apps on modern Android. " +
                        "Use system Storage settings to format drives."
            )
        }
    }
}
