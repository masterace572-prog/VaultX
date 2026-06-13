package com.vaultx.user.di

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vaultx.user.data.local.db.*
import com.vaultx.user.security.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.vaultx.user.data.local.dataStore

// ─────────────────────────────────────────────────────────────────────────────
// Hilt DI Module — App-level singleton dependencies
// ─────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── CryptoManager ─────────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideCryptoManager(): CryptoManager = CryptoManager()

    // ── EncryptedSharedPreferences ────────────────────────────────────────────
    // Stores: wrapped DB passphrase (AES-GCM encrypted via AndroidKeystore),
    //         user salt, theme preference, cloud sync toggle.
    // NEVER stores: master password, plaintext passwords.
    @Provides
    @Singleton
    fun provideEncryptedPrefs(@ApplicationContext context: Context): android.content.SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "vaultx_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            try {
                // Delete existing preferences file as it may be corrupted or have mismatched Keystore keys
                val sharedPrefsFile = java.io.File(context.filesDir.parentFile, "shared_prefs/vaultx_secure_prefs.xml")
                if (sharedPrefsFile.exists()) {
                    sharedPrefsFile.delete()
                }
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    "vaultx_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (ex: Exception) {
                // Fallback to normal shared preferences if keystore is completely broken
                context.getSharedPreferences("vaultx_secure_prefs_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    // ── Firebase ──────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // ── Theme Preferences ─────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
        return context.dataStore
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VaultSession — holds the unlocked DB instance during the app session.
// Cleared on app exit / biometric lock-out.
// ─────────────────────────────────────────────────────────────────────────────
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * The database is initially null and populated by [VaultSessionManager]
     * once the user authenticates. ViewModels should never touch this directly;
     * they go through repositories which check session state.
     */
    @Provides
    @Singleton
    fun provideVaultSessionManager(
        @ApplicationContext context: Context,
        cryptoManager: CryptoManager
    ): VaultSessionManager = VaultSessionManager(context, cryptoManager)
}

// ─────────────────────────────────────────────────────────────────────────────
// VaultSessionManager — manages the open/close lifecycle of the encrypted DB
// ─────────────────────────────────────────────────────────────────────────────

class VaultSessionManager(
    private val context: Context,
    private val cryptoManager: CryptoManager
) {
    private var _db: VaultXDatabase? = null

    val database: VaultXDatabase
        get() = _db ?: throw IllegalStateException(
            "Vault is locked. Call openVault() after authentication."
        )

    val isUnlocked: Boolean get() = _db != null

    fun openVault(passphrase: ByteArray) {
        if (_db == null) {
            _db = VaultXDatabase.buildEncrypted(context, passphrase)
        }
    }

    fun lockVault() {
        _db?.close()
        _db = null
    }
}
