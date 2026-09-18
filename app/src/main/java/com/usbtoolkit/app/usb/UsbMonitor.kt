package com.usbtoolkit.app.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.usbtoolkit.app.data.FilesystemType
import com.usbtoolkit.app.data.UsbDriveInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Monitors USB / removable storage attachment and detachment.
 * Uses StorageManager + UsbManager. File access relies on SAF / DocumentFile
 * after the user grants tree permission.
 */
class UsbMonitor(private val context: Context) {

    private val _drives = MutableStateFlow<List<UsbDriveInfo>>(emptyList())
    val drives: StateFlow<List<UsbDriveInfo>> = _drives.asStateFlow()

    private val _primaryDrive = MutableStateFlow<UsbDriveInfo?>(null)
    val primaryDrive: StateFlow<UsbDriveInfo?> = _primaryDrive.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED,
                UsbManager.ACTION_USB_DEVICE_DETACHED,
                Intent.ACTION_MEDIA_MOUNTED,
                Intent.ACTION_MEDIA_UNMOUNTED,
                Intent.ACTION_MEDIA_EJECT,
                Intent.ACTION_MEDIA_REMOVED,
                Intent.ACTION_MEDIA_BAD_REMOVAL -> refresh()
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
            addDataScheme("file")
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        refresh()
    }

    fun stop() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
    }

    fun refresh() {
        val list = mutableListOf<UsbDriveInfo>()
        val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            for (volume in sm.storageVolumes) {
                if (!volume.isRemovable && volume.isPrimary) continue
                val isRemovable = volume.isRemovable
                if (!isRemovable && !volume.isPrimary) continue

                val path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    volume.directory?.absolutePath
                } else {
                    @Suppress("DEPRECATION")
                    volume.getPath()
                }

                val state = volume.state
                if (state != Environment.MEDIA_MOUNTED && state != Environment.MEDIA_MOUNTED_READ_ONLY) {
                    continue
                }

                val total = try {
                    if (path != null) File(path).totalSpace else 0L
                } catch (_: Exception) {
                    0L
                }
                val free = try {
                    if (path != null) File(path).freeSpace else 0L
                } catch (_: Exception) {
                    0L
                }
                val used = (total - free).coerceAtLeast(0)

                val name = volume.getDescription(context) ?: path?.let { File(it).name } ?: "USB Drive"
                val readOnly = state == Environment.MEDIA_MOUNTED_READ_ONLY || volume.isEmulated.not() && free == 0L && total > 0

                list.add(
                    UsbDriveInfo(
                        id = volume.uuid ?: path ?: name,
                        name = name,
                        path = path,
                        uri = null,
                        totalBytes = total,
                        freeBytes = free,
                        usedBytes = used,
                        filesystem = detectFs(path),
                        isRemovable = isRemovable,
                        isReadOnly = readOnly
                    )
                )
            }
        }

        if (list.isEmpty()) {
            val candidates = listOf(
                "/storage/usb",
                "/mnt/usb",
                "/mnt/media_rw",
                "/storage"
            )
            for (base in candidates) {
                val dir = File(base)
                if (!dir.exists() || !dir.isDirectory) continue
                dir.listFiles()?.forEach { child ->
                    if (child.isDirectory && child.canRead() && child.totalSpace > 0) {
                        val total = child.totalSpace
                        val free = child.freeSpace
                        list.add(
                            UsbDriveInfo(
                                id = child.absolutePath,
                                name = child.name,
                                path = child.absolutePath,
                                uri = null,
                                totalBytes = total,
                                freeBytes = free,
                                usedBytes = (total - free).coerceAtLeast(0),
                                filesystem = detectFs(child.absolutePath),
                                isRemovable = true,
                                isReadOnly = !child.canWrite()
                            )
                        )
                    }
                }
            }
        }

        _drives.value = list.distinctBy { it.id }
        _primaryDrive.value = list.firstOrNull { it.isRemovable } ?: list.firstOrNull()
    }

    fun updateDriveUri(driveId: String, treeUri: Uri, doc: DocumentFile) {
        val updated = _drives.value.map { d ->
            if (d.id == driveId) d.copy(uri = treeUri, documentFile = doc) else d
        }
        _drives.value = updated
        if (_primaryDrive.value?.id == driveId) {
            _primaryDrive.value = updated.find { it.id == driveId }
        }
    }

    private fun detectFs(path: String?): FilesystemType {
        return FilesystemType.UNKNOWN
    }

    fun safelyEject(drive: UsbDriveInfo): Result<Unit> {
        return try {
            val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val volume = sm.storageVolumes.find {
                    it.uuid == drive.id || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                            it.directory?.absolutePath == drive.path)
                }
                if (volume != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Result.failure(SecurityException("System does not allow third-party apps to unmount volumes. Close all open files and remove the drive safely."))
                } else {
                    Result.failure(UnsupportedOperationException("Eject not supported on this Android version via public API."))
                }
            } else {
                Result.failure(UnsupportedOperationException("Eject not supported."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
