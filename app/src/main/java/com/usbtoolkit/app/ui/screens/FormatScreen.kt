package com.usbtoolkit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.usbtoolkit.app.data.FilesystemType
import com.usbtoolkit.app.util.FileUtils
import com.usbtoolkit.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val drive by viewModel.primaryDrive.collectAsState()
    var selectedFs by remember { mutableStateOf(FilesystemType.FAT32) }
    var showConfirm by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Format USB") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            if (drive == null) {
                Text("No USB drive detected.")
                return@Column
            }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null)
                    Spacer(Modifier.width(12.dp))
                    Text("Formatting erases ALL data permanently.")
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Selected drive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(drive!!.name, style = MaterialTheme.typography.titleLarge)
                    Text("Capacity: ${FileUtils.formatSize(drive!!.totalBytes)}")
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Filesystem", style = MaterialTheme.typography.titleMedium)
            listOf(FilesystemType.FAT32, FilesystemType.EXFAT, FilesystemType.NTFS).forEach { fs ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedFs == fs, onClick = { selectedFs = fs })
                    Text(fs.displayName)
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { showConfirm = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Format Drive")
            }

            resultMessage?.let {
                Spacer(Modifier.height(16.dp))
                Text(it)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirm format") },
            text = { Text(viewModel.formatNotSupportedMessage()) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    resultMessage = viewModel.formatNotSupportedMessage()
                }) { Text("I understand") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
