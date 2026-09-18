package com.usbtoolkit.app.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.usbtoolkit.app.data.FileItem
import com.usbtoolkit.app.data.TransferProgress
import com.usbtoolkit.app.data.TransferState
import com.usbtoolkit.app.data.VerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

object FileUtils {

    fun listFiles(doc: DocumentFile?): List<FileItem> {
        if (doc == null || !doc.isDirectory) return emptyList()
        return doc.listFiles().mapNotNull { f ->
            try {
                FileItem(
                    name = f.name ?: "unknown",
                    uri = f.uri,
                    isDirectory = f.isDirectory,
                    size = if (f.isFile) f.length() else 0L,
                    lastModified = f.lastModified(),
                    mimeType = f.type,
                    canRead = f.canRead(),
                    canWrite = f.canWrite()
                )
            } catch (_: Exception) {
                null
            }
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 0) return "—"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var v = bytes.toDouble()
        var i = 0
        while (v >= 1024 && i < units.lastIndex) {
            v /= 1024
            i++
        }
        return if (i == 0) "$bytes B" else String.format("%.1f %s", v, units[i])
    }

    suspend fun sha256(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun copyFiles(
        context: Context,
        sources: List<DocumentFile>,
        destDir: DocumentFile,
        verify: Boolean,
        onProgress: (TransferProgress) -> Unit
    ): Flow<TransferProgress> = flow {
        var progress = TransferProgress(
            state = TransferState.PREPARING,
            totalFiles = sources.size
        )
        emit(progress)

        val totalBytes = sources.sumOf { if (it.isFile) it.length() else 0L }
        progress = progress.copy(totalBytes = totalBytes, state = TransferState.COPYING)
        emit(progress)

        var bytesDone = 0L
        var filesDone = 0
        val startTime = System.currentTimeMillis()

        for (src in sources) {
            if (!coroutineContext.isActive) {
                emit(progress.copy(state = TransferState.CANCELLED))
                return@flow
            }
            if (!src.exists()) {
                emit(progress.copy(state = TransferState.FAILED, errorMessage = "Source disappeared: ${src.name}"))
                return@flow
            }
            if (!destDir.canWrite()) {
                emit(progress.copy(state = TransferState.FAILED, errorMessage = "Destination is read-only or inaccessible"))
                return@flow
            }

            progress = progress.copy(currentFile = src.name ?: "file")
            emit(progress)

            try {
                if (src.isDirectory) {
                    destDir.createDirectory(src.name ?: "folder")
                } else {
                    val name = uniqueName(destDir, src.name ?: "file")
                    val destFile = destDir.createFile(src.type ?: "application/octet-stream", name)
                        ?: throw Exception("Cannot create file $name")

                    context.contentResolver.openInputStream(src.uri)?.use { input ->
                        context.contentResolver.openOutputStream(destFile.uri)?.use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                if (!coroutineContext.isActive) {
                                    destFile.delete()
                                    emit(progress.copy(state = TransferState.CANCELLED))
                                    return@flow
                                }
                                output.write(buffer, 0, read)
                                bytesDone += read
                                val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                                val speed = (bytesDone * 1000) / elapsed
                                progress = progress.copy(
                                    bytesTransferred = bytesDone,
                                    speedBytesPerSec = speed
                                )
                                emit(progress)
                            }
                            output.flush()
                        } ?: throw Exception("Cannot open destination stream")
                    } ?: throw Exception("Cannot open source stream")
                }
                filesDone++
                progress = progress.copy(filesCompleted = filesDone)
                emit(progress)
            } catch (e: Exception) {
                emit(progress.copy(state = TransferState.FAILED, errorMessage = e.message ?: "Copy failed"))
                return@flow
            }
        }

        if (verify && sources.isNotEmpty()) {
            progress = progress.copy(state = TransferState.VERIFYING)
            emit(progress)
            val src = sources.firstOrNull { it.isFile }
            if (src != null) {
                val dest = destDir.findFile(src.name ?: "") ?: destDir.listFiles().find { it.name == src.name }
                if (dest != null) {
                    val h1 = sha256(context, src.uri)
                    val h2 = sha256(context, dest.uri)
                    val result = when {
                        h1 == null || h2 == null -> VerificationResult.UNAVAILABLE
                        h1 == h2 -> VerificationResult.VERIFIED
                        else -> VerificationResult.FAILED
                    }
                    progress = progress.copy(verificationResult = result)
                } else {
                    progress = progress.copy(verificationResult = VerificationResult.UNAVAILABLE)
                }
                emit(progress)
            }
        }

        emit(progress.copy(state = TransferState.COMPLETED))
    }.flowOn(Dispatchers.IO)

    private fun uniqueName(dir: DocumentFile, desired: String): String {
        var name = desired
        var i = 1
        while (dir.findFile(name) != null) {
            val dot = desired.lastIndexOf('.')
            name = if (dot > 0) {
                desired.substring(0, dot) + " ($i)" + desired.substring(dot)
            } else {
                "$desired ($i)"
            }
            i++
        }
        return name
    }

    fun createFolder(parent: DocumentFile, name: String): DocumentFile? {
        return try {
            parent.createDirectory(name)
        } catch (_: Exception) {
            null
        }
    }

    fun delete(doc: DocumentFile): Boolean {
        return try {
            doc.delete()
        } catch (_: Exception) {
            false
        }
    }

    fun rename(doc: DocumentFile, newName: String): Boolean {
        return try {
            doc.renameTo(newName)
        } catch (_: Exception) {
            false
        }
    }
}
