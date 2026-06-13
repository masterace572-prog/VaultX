package com.vaultx.user.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// BiometricHelper — Wraps AndroidX BiometricPrompt
// Supports fingerprint, face unlock, and device credential fallback.
// ─────────────────────────────────────────────────────────────────────────────

sealed class BiometricResult {
    object Success : BiometricResult()
    data class Error(val code: Int, val message: String) : BiometricResult()
    object Failed : BiometricResult()
    object NotAvailable : BiometricResult()
}

enum class BiometricAvailability {
    AVAILABLE,
    NOT_ENROLLED,         // Hardware present but no biometrics enrolled
    NO_HARDWARE,          // Device has no biometric hardware
    UNAVAILABLE           // Other errors
}

@Singleton
class BiometricHelper @Inject constructor() {

    /**
     * Checks whether biometric authentication is available on this device.
     */
    fun checkAvailability(context: Context): BiometricAvailability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS          -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE   -> BiometricAvailability.NO_HARDWARE
            else                                           -> BiometricAvailability.UNAVAILABLE
        }
    }

    /**
     * Shows the system biometric prompt.
     * Must be called from a [FragmentActivity].
     * [onResult] is invoked on the main thread with the authentication outcome.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock VaultX",
        subtitle: String = "Use your biometric to access your vault",
        negativeButtonText: String = "Use Password",
        onResult: (BiometricResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(BiometricResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(BiometricResult.Error(errorCode, errString.toString()))
            }

            override fun onAuthenticationFailed() {
                onResult(BiometricResult.Failed)
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            // Note: setNegativeButtonText is mutually exclusive with DEVICE_CREDENTIAL.
            // If you want a negative button (fallback to password), remove DEVICE_CREDENTIAL.
            .build()

        try {
            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onResult(BiometricResult.Error(-1, e.localizedMessage ?: "Biometric authentication failed"))
        }
    }

    /**
     * Variant with a custom crypto object (for Keystore-backed key unwrapping).
     * Used when we need to unwrap the DB passphrase using the biometric-protected key.
     */
    fun authenticateWithCrypto(
        activity: FragmentActivity,
        cryptoObject: BiometricPrompt.CryptoObject,
        title: String = "Unlock VaultX",
        subtitle: String = "Use your biometric to access your vault",
        onResult: (BiometricResult, BiometricPrompt.AuthenticationResult?) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(BiometricResult.Success, result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(BiometricResult.Error(errorCode, errString.toString()), null)
            }

            override fun onAuthenticationFailed() {
                onResult(BiometricResult.Failed, null)
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setNegativeButtonText("Use Master Password")
            .build()

        try {
            prompt.authenticate(promptInfo, cryptoObject)
        } catch (e: Exception) {
            onResult(BiometricResult.Error(-1, e.localizedMessage ?: "Biometric authentication failed"), null)
        }
    }
}
