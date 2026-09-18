package com.usbtoolkit.app.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.usbtoolkit.app.data.DownloadItem
import com.usbtoolkit.app.data.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class AppDownloadManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _history = MutableStateFlow<List<DownloadItem>>(emptyList())
    val history = _history.asStateFlow()

    private val activeJobs = mutableMapOf<String, Boolean>()

    fun startDownload(
        url: String,
        fileName: String,
        destDir: DocumentFile?,
        expectedSha256: String? = null
    ): Flow<DownloadItem> = flow {
        val id = UUID.randomUUID().toString()
        var item = DownloadItem(
            id = id,
            url = url,
            fileName = fileName,
            destinationUri = null,
            status = DownloadStatus.DOWNLOADING,
            sha256Expected = expectedSha256?.lowercase()?.trim()
        )
        activeJobs[id] = false
        updateHistory(item)
        emit(item)

        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    item = item.copy(status = DownloadStatus.FAILED, error = "HTTP ${response.code}")
                    updateHistory(item)
                    emit(item)
                    return@flow
                }
                val body = response.body ?: run {
                    item = item.copy(status = DownloadStatus.FAILED, error = "Empty body")
                    updateHistory(item)
                    emit(item)
                    return@flow
                }
                val total = body.contentLength()
                item = item.copy(totalBytes = total)
                emit(item)

                val targetDir = destDir ?: DocumentFile.fromFile(context.getExternalFilesDir(null)!!)
                val outFile = targetDir.createFile(
                    response.header("Content-Type") ?: "application/octet-stream",
                    fileName
                ) ?: run {
                    item = item.copy(status = DownloadStatus.FAILED, error = "Cannot create file")
                    updateHistory(item)
                    emit(item)
                    return@flow
                }

                var downloaded = 0L
                val start = System.currentTimeMillis()
                body.byteStream().use { input ->
                    context.contentResolver.openOutputStream(outFile.uri)?.use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            if (activeJobs[id] == true || !coroutineContext.isActive) {
                                outFile.delete()
                                item = item.copy(status = DownloadStatus.CANCELLED)
                                updateHistory(item)
                                emit(item)
                                return@flow
                            }
                            output.write(buffer, 0, read)
                            downloaded += read
                            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1)
                            val speed = (downloaded * 1000) / elapsed
                            item = item.copy(
                                downloadedBytes = downloaded,
                                speedBytesPerSec = speed,
                                destinationUri = outFile.uri
                            )
                            emit(item)
                            updateHistory(item)
                        }
                        output.flush()
                    }
                }

                if (!item.sha256Expected.isNullOrBlank()) {
                    val actual = FileUtils.sha256(context, outFile.uri)
                    item = item.copy(sha256Actual = actual)
                    if (actual != null && actual != item.sha256Expected) {
                        item = item.copy(status = DownloadStatus.FAILED, error = "SHA-256 mismatch")
                        updateHistory(item)
                        emit(item)
                        return@flow
                    }
                }

                item = item.copy(status = DownloadStatus.COMPLETED, downloadedBytes = downloaded)
                updateHistory(item)
                emit(item)
            }
        } catch (e: Exception) {
            item = item.copy(status = DownloadStatus.FAILED, error = e.message ?: "Download error")
            updateHistory(item)
            emit(item)
        } finally {
            activeJobs.remove(id)
        }
    }.flowOn(Dispatchers.IO)

    fun cancel(id: String) {
        activeJobs[id] = true
    }

    private fun updateHistory(item: DownloadItem) {
        val current = _history.value.toMutableList()
        val idx = current.indexOfFirst { it.id == item.id }
        if (idx >= 0) current[idx] = item else current.add(0, item)
        _history.value = current.take(50)
    }

    fun clearHistory() {
        _history.value = emptyList()
    }
}
