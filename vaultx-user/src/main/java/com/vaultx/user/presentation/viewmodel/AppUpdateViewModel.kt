package com.vaultx.user.presentation.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.vaultx.user.data.model.AppConfig
import com.vaultx.user.update.ApkUpdateManager
import com.vaultx.user.update.DownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// AppUpdateViewModel
// Checks Firestore for a newer version on startup, exposes update info,
// and delegates downloading to ApkUpdateManager.
// ─────────────────────────────────────────────────────────────────────────────

data class UpdateInfo(
    val versionCode:    Int,
    val versionName:    String,
    val changelog:      List<String>,
    val apkDownloadUrl: String,
    val isForced:       Boolean
)

sealed class UpdateCheckState {
    object Idle          : UpdateCheckState()
    object Checking      : UpdateCheckState()
    object NoUpdate      : UpdateCheckState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckState()
}

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val firestore:       FirebaseFirestore,
    private val apkUpdateManager: ApkUpdateManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val updateCheckState: StateFlow<UpdateCheckState> = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
        .also { checkForUpdate() }
        .asStateFlow()

    private val _updateCheckStateMutable = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateCheckStateMutable.asStateFlow()

    val downloadState: StateFlow<DownloadState> = apkUpdateManager.downloadState

    // ── Update check on startup ───────────────────────────────────────────────
    private fun checkForUpdate() {
        viewModelScope.launch {
            delay(2000) // Let the app load first, don't block UI
            _updateCheckStateMutable.value = UpdateCheckState.Checking
            runCatching {
                val doc = firestore.collection("app_config")
                    .document("main")
                    .get()
                    .await()

                val remoteVersionCode = doc.getLong("latest_version_code")?.toInt() ?: 0
                
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersionCode = PackageInfoCompat.getLongVersionCode(pInfo).toInt()

                if (remoteVersionCode > currentVersionCode) {
                    val info = UpdateInfo(
                        versionCode    = remoteVersionCode,
                        versionName    = doc.getString("latest_version_name") ?: "",
                        changelog      = (doc.get("changelog") as? List<*>)
                                            ?.filterIsInstance<String>() ?: emptyList(),
                        apkDownloadUrl = doc.getString("apk_download_url") ?: "",
                        isForced       = doc.getBoolean("is_forced_update") ?: false
                    )
                    _updateCheckStateMutable.value = UpdateCheckState.UpdateAvailable(info)
                } else {
                    _updateCheckStateMutable.value = UpdateCheckState.NoUpdate
                }
            }.onFailure {
                _updateCheckStateMutable.value = UpdateCheckState.NoUpdate
            }
        }
    }

    // ── Download actions ──────────────────────────────────────────────────────
    fun startDownload(info: UpdateInfo) {
        apkUpdateManager.startDownload(info.apkDownloadUrl, info.versionName)
        // Poll progress every 500ms
        viewModelScope.launch {
            while (downloadState.value is DownloadState.Downloading ||
                   downloadState.value is DownloadState.Queued) {
                delay(500)
                apkUpdateManager.pollProgress()
            }
        }
    }

    fun dismissUpdate() {
        _updateCheckStateMutable.value = UpdateCheckState.NoUpdate
    }

    fun installApk(context: Context, file: java.io.File) {
        apkUpdateManager.installApk(context, file)
    }
}
