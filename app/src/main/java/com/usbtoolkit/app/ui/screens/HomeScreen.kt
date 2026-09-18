package com.usbtoolkit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usbtoolkit.app.data.UsbDriveInfo
import com.usbtoolkit.app.util.FileUtils
import com.usbtoolkit.app.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onFormat: () -> Unit,
    onBrowse: () -> Unit,
    onCopy: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {
    val drive by viewModel.primaryDrive.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshUsb() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("USB Toolkit", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Professional USB storage utility", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        if (drive != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Usb, null, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(drive!!.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(if (drive!!.isReadOnly) "Read-only" else "Connected")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(progress = { drive!!.usedPercent / 100f }, modifier = Modifier.fillMaxWidth().height(10.dp))
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total: ${FileUtils.formatSize(drive!!.totalBytes)}")
                        Text("Free: ${FileUtils.formatSize(drive!!.freeBytes)}")
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.UsbOff, null, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No USB drive connected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Connect a USB flash drive via OTG or USB-C.", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.refreshUsb() }) { Text("Refresh") }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        val buttons = listOf(
            Triple("Format USB", Icons.Default.FormatPaint, onFormat),
            Triple("Browse USB", Icons.Default.FolderOpen, onBrowse),
            Triple("Copy Files", Icons.Default.ContentCopy, onCopy),
            Triple("Downloads", Icons.Default.Download, onDownloads),
            Triple("Safely Eject", Icons.Default.Eject, { drive?.let { viewModel.safelyEject(it) } }),
            Triple("Settings", Icons.Default.Settings, onSettings)
        )

        buttons.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, icon, action) ->
                    Card(
                        onClick = action,
                        modifier = Modifier.weight(1f).height(100.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, null, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
