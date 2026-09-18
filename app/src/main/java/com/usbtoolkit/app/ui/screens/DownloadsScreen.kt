package com.usbtoolkit.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.usbtoolkit.app.data.DownloadStatus
import com.usbtoolkit.app.util.FileUtils
import com.usbtoolkit.app.viewmodel.MainViewModel

@Composable
fun DownloadsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var sha256 by remember { mutableStateOf("") }
    var destUri by remember { mutableStateOf<Uri?>(null) }
    val progress by viewModel.downloadProgress.collectAsState()
    val history by viewModel.downloadHistory.collectAsState()

    val destLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            destUri = it
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Download file", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = fileName, onValueChange = { fileName = it }, label = { Text("File name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = sha256, onValueChange = { sha256 = it }, label = { Text("Expected SHA-256 (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { destLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
            Text(destUri?.lastPathSegment ?: "Choose destination folder")
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val name = fileName.ifBlank { url.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" } }
                val dest = destUri?.let { DocumentFile.fromTreeUri(context, it) }
                viewModel.startDownload(url.trim(), name, dest, sha256.ifBlank { null })
            },
            enabled = url.isNotBlank() && progress?.status != DownloadStatus.DOWNLOADING,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Download, null)
            Spacer(Modifier.width(8.dp))
            Text("Start Download")
        }

        progress?.let { p ->
            Spacer(Modifier.height(16.dp))
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(p.fileName)
                    Text("Status: ${p.status}")
                    if (p.totalBytes > 0) {
                        LinearProgressIndicator(progress = { p.progressPercent / 100f }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                        Text("${FileUtils.formatSize(p.downloadedBytes)} / ${FileUtils.formatSize(p.totalBytes)}")
                    }
                    p.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (p.status == DownloadStatus.DOWNLOADING) {
                        TextButton(onClick = { viewModel.cancelDownload(p.id) }) { Text("Cancel") }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("History", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(history, key = { it.id }) { item ->
                ListItem(headlineContent = { Text(item.fileName) }, supportingContent = { Text("${item.status} · ${FileUtils.formatSize(item.downloadedBytes)}") })
            }
        }
    }
}
