package com.usbtoolkit.app.data

import android.net.Uri
import androidx.documentfile.provider.DocumentFile

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class FilesystemType(val displayName: String) {
    FAT32("FAT32"),
    EXFAT("exFAT"),
    NTFS("NTFS"),
    UNKNOWN("Unknown")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val defaultDownloadDir: String = "",
    val defaultUsbDestDir: String = "",
    val transferVerificationEnabled: Boolean = true,
    val autoUsbDetection: Boolean = true,
    val downloadWifiOnly: Boolean = false
)

data class UsbDriveInfo(
    val id: String,
    val name: String,
    val path: String?,
    val uri: Uri?,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val filesystem: FilesystemType,
    val isRemovable: Boolean,
    val isReadOnly: Boolean,
    val documentFile: DocumentFile? = null
) {
    val usedPercent: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) * 100f else 0f
}

data class FileItem(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String? = null,
    val canRead: Boolean = true,
    val canWrite: Boolean = true
)

enum class TransferState {
    IDLE, PREPARING, COPYING, VERIFYING, COMPLETED, FAILED, CANCELLED
}

data class TransferProgress(
    val state: TransferState = TransferState.IDLE,
    val currentFile: String = "",
    val bytesTransferred: Long = 0,
    val totalBytes: Long = 0,
    val filesCompleted: Int = 0,
    val totalFiles: Int = 0,
    val speedBytesPerSec: Long = 0,
    val errorMessage: String? = null,
    val verificationResult: VerificationResult? = null
) {
    val progressPercent: Float
        get() = if (totalBytes > 0) (bytesTransferred.toFloat() / totalBytes) * 100f else 0f
}

enum class VerificationResult { VERIFIED, FAILED, UNAVAILABLE }

data class DownloadItem(
    val id: String,
    val url: String,
    val fileName: String,
    val destinationUri: Uri?,
    val totalBytes: Long = -1,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val error: String? = null,
    val sha256Expected: String? = null,
    val sha256Actual: String? = null,
    val speedBytesPerSec: Long = 0,
    val startedAt: Long = System.currentTimeMillis()
) {
    val progressPercent: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes) * 100f else 0f
}

enum class DownloadStatus {
    PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
}
