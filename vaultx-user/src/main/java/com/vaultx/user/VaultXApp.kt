package com.vaultx.user

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// ─────────────────────────────────────────────────────────────────────────────
// VaultXApp — Hilt Application entry point
// ─────────────────────────────────────────────────────────────────────────────

@HiltAndroidApp
class VaultXApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Future: init Timber, crash reporting (self-hosted only), etc.
    }
}
