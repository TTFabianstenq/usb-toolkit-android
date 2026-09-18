package com.usbtoolkit.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usbtoolkit.app.data.ThemeMode
import com.usbtoolkit.app.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel, onAbout: () -> Unit) {
    val settings by viewModel.settings.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ThemeMode.entries.forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.updateTheme(mode) }
                    .padding(vertical = 8.dp)
            ) {
                RadioButton(
                    selected = settings.themeMode == mode,
                    onClick = { viewModel.updateTheme(mode) }
                )
                Text(
                    when (mode) {
                        ThemeMode.SYSTEM -> "System default"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        Text("Transfers", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Verify copies with SHA-256")
            Switch(
                checked = settings.transferVerificationEnabled,
                onCheckedChange = { viewModel.updateVerification(it) }
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Automatic USB detection")
            Switch(
                checked = settings.autoUsbDetection,
                onCheckedChange = { viewModel.updateAutoUsb(it) }
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        Text("Downloads", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Wi-Fi only")
            Switch(
                checked = settings.downloadWifiOnly,
                onCheckedChange = { viewModel.updateWifiOnly(it) }
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        ListItem(
            headlineContent = { Text("About & licenses") },
            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
            modifier = Modifier.clickable { onAbout() }
        )
    }
}
