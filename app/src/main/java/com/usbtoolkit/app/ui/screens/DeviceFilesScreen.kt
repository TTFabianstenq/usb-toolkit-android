package com.usbtoolkit.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.usbtoolkit.app.data.TransferState
import com.usbtoolkit.app.util.FileUtils
import com.usbtoolkit.app.viewmodel.MainViewModel

@Composable
fun DeviceFilesScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val drive by viewModel.primaryDrive.collectAsState()
    val progress by viewModel.transferProgress.collectAsState()
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var destUri by remember { mutableStateOf<Uri?>(null) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

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
            drive?.id?.let { id -> viewModel.setUsbTree(it, id) }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Device Files", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Copy your own files to the connected USB drive. No bundled payloads.")
        Spacer(Modifier.height(24.dp))

        OutlinedButton(onClick = { sourceLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Folder, null)
            Spacer(Modifier.width(8.dp))
            Text(sourceUri?.lastPathSegment ?: "Select source files folder")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { destLauncher.launch(null) }, modifier = Modifier.fillMaxWidth(), enabled = drive != null) {
            Icon(Icons.Default.Usb, null)
            Spacer(Modifier.width(8.dp))
            Text(destUri?.lastPathSegment ?: "Select USB destination folder")
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val src = sourceUri?.let { DocumentFile.fromTreeUri(context, it) }
                val dest = destUri?.let { DocumentFile.fromTreeUri(context, it) }
                if (src != null && dest != null) {
                    val files = src.listFiles().filter { it.isFile }
                    if (files.isEmpty()) statusMsg = "No files found"
                    else {
                        viewModel.startCopy(files, dest, verify = true)
                        statusMsg = null
                    }
                }
            },
            enabled = sourceUri != null && destUri != null && progress.state !in listOf(TransferState.COPYING, TransferState.VERIFYING),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Copy & Verify") }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                drive?.let {
                    val result = viewModel.safelyEject(it)
                    statusMsg = result.fold({ "Ejected" }, { e -> e.message ?: "Eject failed" })
                }
            },
            enabled = drive != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Safely Eject") }

        if (progress.state != TransferState.IDLE) {
            Spacer(Modifier.height(16.dp))
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Transfer: ${progress.state}")
                    LinearProgressIndicator(progress = { progress.progressPercent / 100f }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    progress.verificationResult?.let { Text("Verification: $it") }
                    progress.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }

        statusMsg?.let {
            Spacer(Modifier.height(12.dp))
            Text(it)
        }
    }
}
