package com.usbtoolkit.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.usbtoolkit.app.data.TransferState
import com.usbtoolkit.app.util.FileUtils
import com.usbtoolkit.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val progress by viewModel.transferProgress.collectAsState()
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var destUri by remember { mutableStateOf<Uri?>(null) }

    val sourceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            sourceUri = it
        }
    }
    val destLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            destUri = it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Copy Files") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Select source folder")
            OutlinedButton(onClick = { sourceLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                Text(sourceUri?.lastPathSegment ?: "Choose source")
            }
            Spacer(Modifier.height(16.dp))
            Text("Select destination folder")
            OutlinedButton(onClick = { destLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                Text(destUri?.lastPathSegment ?: "Choose destination")
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val srcDoc = sourceUri?.let { DocumentFile.fromTreeUri(context, it) }
                    val destDoc = destUri?.let { DocumentFile.fromTreeUri(context, it) }
                    if (srcDoc != null && destDoc != null) {
                        val files = srcDoc.listFiles().filter { it.isFile }.toList()
                        if (files.isNotEmpty()) viewModel.startCopy(files, destDoc)
                    }
                },
                enabled = sourceUri != null && destUri != null && progress.state != TransferState.COPYING,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, null)
                Spacer(Modifier.width(8.dp))
                Text("Start Copy")
            }

            if (progress.state != TransferState.IDLE) {
                Spacer(Modifier.height(24.dp))
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Status: ${progress.state.name}")
                        LinearProgressIndicator(progress = { progress.progressPercent / 100f }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                        Text("${FileUtils.formatSize(progress.bytesTransferred)} / ${FileUtils.formatSize(progress.totalBytes)}")
                        progress.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        progress.verificationResult?.let { Text("Verification: $it") }
                        if (progress.state == TransferState.COPYING) {
                            TextButton(onClick = { viewModel.cancelCopy() }) { Text("Cancel") }
                        }
                    }
                }
            }
        }
    }
}
