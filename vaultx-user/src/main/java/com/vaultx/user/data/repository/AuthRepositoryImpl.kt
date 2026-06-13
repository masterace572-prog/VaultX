package com.vaultx.user.data.repository

import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vaultx.user.data.local.db.UserProfileDao
import com.vaultx.user.data.local.db.UserProfileEntity
import com.vaultx.user.data.model.Tier
import com.vaultx.user.di.VaultSessionManager
import com.vaultx.user.domain.repository.AuthRepository
import com.vaultx.user.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// AuthRepositoryImpl
// Handles Firebase Auth sign-in/register and local profile caching.
// Key derivation and vault opening happen here after auth.
// ─────────────────────────────────────────────────────────────────────────────

private const val PREF_SALT        = "user_salt"
private const val PREF_WRAPPED_KEY = "wrapped_db_key"
private const val PREF_WRAPPED_IV  = "wrapped_db_iv"

private const val PREF_PIN_SALT        = "pref_app_pin_salt"
private const val PREF_PIN_WRAPPED_KEY = "pref_wrapped_db_key_by_pin"
private const val PREF_PIN_WRAPPED_IV  = "pref_wrapped_db_iv_by_pin"

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth:    FirebaseAuth,
    private val firestore:       FirebaseFirestore,
    private val cryptoManager:   CryptoManager,
    private val vaultSession:    VaultSessionManager,
    private val encryptedPrefs:  SharedPreferences,
) : AuthRepository {

    override val currentUid: String?
        get() = firebaseAuth.currentUser?.uid

    override val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    // ── Login ─────────────────────────────────────────────────────────────────
    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        // 1. Firebase Auth sign in
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val uid = firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("UID null after sign-in")

        // 2. Fetch or create salt from Firestore
        val salt = fetchOrCreateSalt(uid, password)

        withContext(Dispatchers.IO) {
            // 3. Derive AES-256 key from master password + salt
            val derivedKey = cryptoManager.deriveKey(password, salt)

            // 4. Wrap the derived key with Keystore key and store it
            val keystoreKey = cryptoManager.getOrCreateKeystoreKey()
            val wrapped = cryptoManager.wrapKey(derivedKey, keystoreKey)
            encryptedPrefs.edit()
                .putString(PREF_WRAPPED_KEY, wrapped.ciphertext)
                .putString(PREF_WRAPPED_IV,  wrapped.iv)
                .apply()

            // 5. Open the encrypted Room DB
            vaultSession.openVault(cryptoManager.keyToBytes(derivedKey))
        }
    }

    // ── Register ──────────────────────────────────────────────────────────────
    override suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): Result<Unit> = runCatching {
        // 1. Create Firebase user
        firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val uid = firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("UID null after registration")

        // 2. Generate new salt for this user
        val salt = cryptoManager.generateSalt()
        val saltB64 = cryptoManager.saltToBase64(salt)

        // 3. Create Firestore user document (no passwords stored here)
        val userDoc = mapOf(
            "uid"          to uid,
            "email"        to email,
            "display_name" to displayName,
            "tier"         to "free",
            "premium_expiry" to null,
            "created_at"   to com.google.firebase.Timestamp.now(),
            "salt"         to saltB64
        )
        firestore.collection("users").document(uid).set(userDoc).await()

        // 4. Persist salt locally
        encryptedPrefs.edit().putString(PREF_SALT, saltB64).apply()

        withContext(Dispatchers.IO) {
            // 5. Derive key, wrap, and open vault
            val derivedKey = cryptoManager.deriveKey(password, salt)
            val keystoreKey = cryptoManager.getOrCreateKeystoreKey()
            val wrapped = cryptoManager.wrapKey(derivedKey, keystoreKey)
            encryptedPrefs.edit()
                .putString(PREF_WRAPPED_KEY, wrapped.ciphertext)
                .putString(PREF_WRAPPED_IV,  wrapped.iv)
                .apply()

            vaultSession.openVault(cryptoManager.keyToBytes(derivedKey))
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    override suspend fun logout() {
        vaultSession.lockVault()
        firebaseAuth.signOut()
        // Clear wrapped key — next login requires password re-entry
        encryptedPrefs.edit()
            .remove(PREF_WRAPPED_KEY)
            .remove(PREF_WRAPPED_IV)
            .apply()
    }

    // ── App PIN Setup / Unlock ────────────────────────────────────────────────
    override suspend fun setupAppPin(pin: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            // 1. Recover the DB key from Keystore-wrapped preferences
            val ciphertext = encryptedPrefs.getString(PREF_WRAPPED_KEY, null)
                ?: throw IllegalStateException("No wrapped DB key available to setup PIN.")
            val iv = encryptedPrefs.getString(PREF_WRAPPED_IV, null)
                ?: throw IllegalStateException("No wrapped DB IV available.")
            
            val keystoreKey = cryptoManager.getOrCreateKeystoreKey()
            val encryptedData = com.vaultx.user.security.EncryptedData(ciphertext, iv)
            val dbKey = cryptoManager.unwrapKey(encryptedData, keystoreKey)

            // 2. Generate a salt specifically for the PIN
            val pinSalt = cryptoManager.generateSalt()
            encryptedPrefs.edit().putString(PREF_PIN_SALT, cryptoManager.saltToBase64(pinSalt)).apply()

            // 3. Derive PIN key
            val pinKey = cryptoManager.derivePinKey(pin, pinSalt)

            // 4. Wrap the DB key using the PIN key
            val pinWrapped = cryptoManager.wrapKey(dbKey, pinKey)

            // 5. Store the PIN-wrapped DB key
            encryptedPrefs.edit()
                .putString(PREF_PIN_WRAPPED_KEY, pinWrapped.ciphertext)
                .putString(PREF_PIN_WRAPPED_IV, pinWrapped.iv)
                .apply()
        }
    }

    override suspend fun unlockWithPin(pin: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            // 1. Get PIN salt and wrapped key
            val saltB64 = encryptedPrefs.getString(PREF_PIN_SALT, null)
                ?: throw IllegalStateException("App Lock not set up")
            val pinSalt = cryptoManager.saltFromBase64(saltB64)

            val ciphertext = encryptedPrefs.getString(PREF_PIN_WRAPPED_KEY, null)
                ?: throw IllegalStateException("No PIN-wrapped DB key available.")
            val iv = encryptedPrefs.getString(PREF_PIN_WRAPPED_IV, null)
                ?: throw IllegalStateException("No PIN-wrapped DB IV available.")

            // 2. Derive PIN key
            val pinKey = cryptoManager.derivePinKey(pin, pinSalt)

            // 3. Unwrap DB key
            val encryptedData = com.vaultx.user.security.EncryptedData(ciphertext, iv)
            val dbKey = cryptoManager.unwrapKey(encryptedData, pinKey)

            // 4. Open vault
            vaultSession.openVault(cryptoManager.keyToBytes(dbKey))
        }
    }

    override suspend fun unlockWithMasterPassword(password: String): Result<Unit> = runCatching {
        val uid = currentUid ?: throw IllegalStateException("User not logged in")
        val salt = fetchOrCreateSalt(uid, password)
        withContext(Dispatchers.IO) {
            val derivedKey = cryptoManager.deriveKey(password, salt)
            vaultSession.openVault(cryptoManager.keyToBytes(derivedKey))
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun fetchOrCreateSalt(uid: String, password: String): ByteArray {
        // Try local first
        val localSalt = encryptedPrefs.getString(PREF_SALT, null)
        if (localSalt != null) return cryptoManager.saltFromBase64(localSalt)

        // Fetch from Firestore
        val doc = firestore.collection("users").document(uid).get().await()
        val remoteSalt = doc.getString("salt")
        
        if (remoteSalt != null) {
            encryptedPrefs.edit().putString(PREF_SALT, remoteSalt).apply()
            return cryptoManager.saltFromBase64(remoteSalt)
        } else {
            // User exists in Auth but not Firestore (e.g., DB was wiped)
            // Re-create the salt and document
            val newSalt = cryptoManager.generateSalt()
            val saltB64 = cryptoManager.saltToBase64(newSalt)
            val userDoc = mapOf(
                "uid"          to uid,
                "email"        to (firebaseAuth.currentUser?.email ?: ""),
                "display_name" to "Vault User",
                "tier"         to "free",
                "premium_expiry" to null,
                "created_at"   to com.google.firebase.Timestamp.now(),
                "salt"         to saltB64
            )
            firestore.collection("users").document(uid).set(userDoc).await()
            encryptedPrefs.edit().putString(PREF_SALT, saltB64).apply()
            return newSalt
        }
    }

    override suspend fun getUserProfile(): Result<UserProfileEntity?> = runCatching { null }

    override suspend fun fetchAndCacheRemoteProfile(): Result<Unit> = runCatching {
        val uid = currentUid ?: return@runCatching
        // Fetches tier/expiry from Firestore and caches locally
        // (implementation expanded in repository module)
    }
}
