package com.usbtoolkit.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.usbtoolkit.app.UsbToolkitApp
import com.usbtoolkit.app.data.*
import com.usbtoolkit.app.util.AppDownloadManager
import com.usbtoolkit.app.util.FileUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: UsbToolkitApp) : AndroidViewModel(app) {

    private val settingsRepo = app.settingsRepository
    private val usbMonitor = app.usbMonitor
    private val downloadManager = AppDownloadManager(app)

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val usbDrives = usbMonitor.drives
    val primaryDrive = usbMonitor.primaryDrive

    private val _transferProgress = MutableStateFlow(TransferProgress())
    val transferProgress = _transferProgress.asStateFlow()

    private val _currentAndroidFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val currentAndroidFiles = _currentAndroidFiles.asStateFlow()

    private val _currentUsbFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val currentUsbFiles = _currentUsbFiles.asStateFlow()

    private val _androidTreeUri = MutableStateFlow<Uri?>(null)
    private val _usbTreeUri = MutableStateFlow<Uri?>(null)

    private val _downloadProgress = MutableStateFlow<DownloadItem?>(null)
    val downloadProgress = _downloadProgress.asStateFlow()

    val downloadHistory = downloadManager.history

    private var copyJob: Job? = null
    private var downloadJob: Job? = null

    fun refreshUsb() {
        usbMonitor.refresh()
    }

    fun setAndroidTree(uri: Uri) {
        _androidTreeUri.value = uri
        val doc = DocumentFile.fromTreeUri(getApplication(), uri)
        _currentAndroidFiles.value = FileUtils.listFiles(doc)
    }

    fun setUsbTree(uri: Uri, driveId: String? = null) {
        _usbTreeUri.value = uri
        val doc = DocumentFile.fromTreeUri(getApplication(), uri)
        _currentUsbFiles.value = FileUtils.listFiles(doc)
        if (driveId != null && doc != null) {
            usbMonitor.updateDriveUri(driveId, uri, doc)
        }
    }

    fun listUsbFiles(doc: DocumentFile?) {
        _currentUsbFiles.value = FileUtils.listFiles(doc)
    }

    fun listAndroidFiles(doc: DocumentFile?) {
        _currentAndroidFiles.value = FileUtils.listFiles(doc)
    }

    fun updateTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.updateTheme(mode) }
    }

    fun updateVerification(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateVerification(enabled) }
    }

    fun updateAutoUsb(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateAutoUsb(enabled) }
    }

    fun updateWifiOnly(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateWifiOnly(enabled) }
    }

    fun startCopy(
        sources: List<DocumentFile>,
        dest: DocumentFile,
        verify: Boolean = settings.value.transferVerificationEnabled
    ) {
        copyJob?.cancel()
        copyJob = viewModelScope.launch {
            FileUtils.copyFiles(getApplication(), sources, dest, verify) { p ->
                _transferProgress.value = p
            }.collect { p ->
                _transferProgress.value = p
            }
        }
    }

    fun cancelCopy() {
        copyJob?.cancel()
        _transferProgress.value = _transferProgress.value.copy(state = TransferState.CANCELLED)
    }

    fun startDownload(url: String, fileName: String, dest: DocumentFile?, sha256: String?) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            downloadManager.startDownload(url, fileName, dest, sha256).collect {
                _downloadProgress.value = it
            }
        }
    }

    fun cancelDownload(id: String) {
        downloadManager.cancel(id)
    }

    fun safelyEject(drive: UsbDriveInfo): Result<Unit> {
        return usbMonitor.safelyEject(drive)
    }

    fun formatNotSupportedMessage(): String {
        return "Formatting USB drives is not supported by third-party applications on modern Android versions due to system security restrictions. " +
                "Please use your device's built-in Storage settings or a computer to format the drive. " +
                "This app will never pretend that a format operation succeeded."
    }
}

class MainViewModelFactory(private val app: UsbToolkitApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
