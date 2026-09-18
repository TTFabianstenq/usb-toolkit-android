package com.usbtoolkit.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.usbtoolkit.app.data.FileItem
import com.usbtoolkit.app.util.FileUtils
import com.usbtoolkit.app.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BrowseScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val androidFiles by viewModel.currentAndroidFiles.collectAsState()
    val usbFiles by viewModel.currentUsbFiles.collectAsState()
    val drive by viewModel.primaryDrive.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val androidTreeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            viewModel.setAndroidTree(it)
        }
    }

    val usbTreeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            viewModel.setUsbTree(it, drive?.id)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Android") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("USB") })
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            placeholder = { Text("Search files…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        val files = if (selectedTab == 0) androidFiles else usbFiles
        val filtered = if (searchQuery.isBlank()) files else files.filter { it.name.contains(searchQuery, ignoreCase = true) }

        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (selectedTab == 0) "Grant access to Android storage" else "Grant access to USB drive folder")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        if (selectedTab == 0) androidTreeLauncher.launch(null)
                        else usbTreeLauncher.launch(null)
                    }) { Text("Select folder") }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(filtered, key = { it.uri.toString() }) { item ->
                    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
                    ListItem(
                        headlineContent = { Text(item.name, fontWeight = FontWeight.Medium) },
                        supportingContent = {
                            Text(if (item.isDirectory) "Folder" else "${FileUtils.formatSize(item.size)} · ${dateFmt.format(Date(item.lastModified))}")
                        },
                        leadingContent = {
                            Icon(if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile, null)
                        }
                    )
                }
            }
        }
    }
}
