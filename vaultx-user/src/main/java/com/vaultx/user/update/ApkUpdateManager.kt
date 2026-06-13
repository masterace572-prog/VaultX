package com.vaultx.user.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// ApkUpdateManager
// Downloads an APK via Android DownloadManager (background, system-managed),
// then triggers the system install intent via FileProvider.
//
// Why DownloadManager?
//  - Survives app backgrounding / process death
//  - System handles retry on failure
//  - No custom download progress polling needed
//  - User sees a system notification with progress
// ─────────────────────────────────────────────────────────────────────────────

sealed class DownloadState {
    object Idle                       : DownloadState()
    object Queued                     : DownloadState()
    data class Downloading(val percent: Int) : DownloadState()
    data class Completed(val file: File)     : DownloadState()
    data class Failed(val reason: String)    : DownloadState()
}

@Singleton
class ApkUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var activeDownloadId: Long = -1L
    private var targetApkName: String  = "vaultx_update.apk"

    // ── Start download ────────────────────────────────────────────────────────

    fun startDownload(apkUrl: String, versionName: String) {
        if (_downloadState.value is DownloadState.Downloading ||
            _downloadState.value is DownloadState.Queued) return

        targetApkName = "vaultx_$versionName.apk"

        // Clean up any stale APK file
        val destFile = getDestinationFile()
        if (destFile.exists()) destFile.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("VaultX Update")
            .setDescription("Downloading version $versionName…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                targetApkName
            )
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        activeDownloadId     = downloadManager.enqueue(request)
        _downloadState.value = DownloadState.Queued

        // Register receiver for completion
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadCompleteReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(downloadCompleteReceiver, filter)
        }
    }

    // ── Poll progress (call from a coroutine / LaunchedEffect) ────────────────

    fun pollProgress() {
        if (activeDownloadId < 0) return
        val query = DownloadManager.Query().setFilterById(activeDownloadId)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            when (status) {
                DownloadManager.STATUS_RUNNING -> {
                    val total     = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val percent   = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                    _downloadState.value = DownloadState.Downloading(percent)
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    _downloadState.value = DownloadState.Completed(getDestinationFile())
                }
                DownloadManager.STATUS_FAILED -> {
                    val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    _downloadState.value = DownloadState.Failed("Download failed (code $reason)")
                }
            }
        }
        cursor.close()
    }

    // ── Trigger system install ─────────────────────────────────────────────────

    fun installApk(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("ApkUpdateManager", "Failed to launch package installer", e)
            _downloadState.value = DownloadState.Failed("Installation failed: ${e.localizedMessage}")
        }
    }

    fun resetState() {
        _downloadState.value = DownloadState.Idle
        activeDownloadId     = -1L
    }

    private fun getDestinationFile(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        return File(dir, targetApkName)
    }

    // ── BroadcastReceiver for download completion ──────────────────────────────
    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id != activeDownloadId) return

            val query  = DownloadManager.Query().setFilterById(id)
            val cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    _downloadState.value = DownloadState.Completed(getDestinationFile())
                } else {
                    val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    _downloadState.value = DownloadState.Failed("Error code: $reason")
                }
            }
            cursor.close()
            try { context.unregisterReceiver(this) } catch (_: Exception) {}
        }
    }
}
