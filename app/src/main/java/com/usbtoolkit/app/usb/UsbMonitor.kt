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
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.usbtoolkit.app.data.FilesystemType
import com.usbtoolkit.app.data.UsbDriveInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

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

                val path: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    volume.directory?.absolutePath
                } else {
                    try {
                        @Suppress("DEPRECATION")
                        (volume.javaClass.getMethod("getPath").invoke(volume) as? String)
                    } catch (_: Exception) {
                        null
                    }
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
                val readOnly = state == Environment.MEDIA_MOUNTED_READ_ONLY

                list.add(
                    UsbDriveInfo(
                        id = volume.uuid ?: path ?: name,
                        name = name,
                        path = path,
                        uri = null,
                        totalBytes = total,
                        freeBytes = free,
                        usedBytes = used,
                        filesystem = FilesystemType.UNKNOWN,
                        isRemovable = isRemovable,
                        isReadOnly = readOnly
                    )
                )
            }
        }

        if (list.isEmpty()) {
            val candidates = listOf("/storage/usb", "/mnt/usb", "/mnt/media_rw", "/storage")
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
                                filesystem = FilesystemType.UNKNOWN,
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

    fun safelyEject(drive: UsbDriveInfo): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException(
                "System does not allow third-party apps to unmount volumes. Close all open files and remove the drive safely."
            )
        )
    }
}
