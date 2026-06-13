package com.vaultx.user.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.vaultx.user.data.local.db.AccountEntryDao
import com.vaultx.user.data.local.db.AccountEntryEntity
import com.vaultx.user.data.local.db.PendingSyncDao
import com.vaultx.user.data.local.db.PendingSyncEntity
import com.vaultx.user.data.model.*
import com.vaultx.user.di.VaultSessionManager
import com.vaultx.user.domain.repository.AccountRepository
import com.vaultx.user.security.CryptoManager
import com.vaultx.user.security.EncryptedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.DataOutputStream
import java.util.UUID
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// AccountRepositoryImpl
// Bridge between Room (encrypted DB) ↔ domain models ↔ Firestore
//
// Encryption flow: AccountEntry → JSON → AES-256-GCM → AccountEntryEntity
// Decryption flow: AccountEntryEntity → AES-256-GCM → JSON → AccountEntry
// ─────────────────────────────────────────────────────────────────────────────

private const val PREF_SALT = "user_salt"
private const val PREF_MASTER_KEY_CIPHERTEXT = "wrapped_db_key"
private const val PREF_MASTER_KEY_IV = "wrapped_db_iv"

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val vaultSession:   VaultSessionManager,
    private val cryptoManager:  CryptoManager,
    private val firestore:      FirebaseFirestore,
    private val encryptedPrefs: SharedPreferences,
    private val authRepository: AuthRepositoryImpl,
) : AccountRepository {

    // ── Key resolution ─────────────────────────────────────────────────────────
    // Derives the AES key from the stored salt + wrapped key.
    // This is called once per repository operation when the vault is open.
    private fun resolveKey(): javax.crypto.SecretKey {
        val saltB64      = encryptedPrefs.getString(PREF_SALT, null)
            ?: throw IllegalStateException("Salt not found — vault not initialized")
        val wrappedCt    = encryptedPrefs.getString(PREF_MASTER_KEY_CIPHERTEXT, null)
            ?: throw IllegalStateException("Wrapped key not found")
        val wrappedIv    = encryptedPrefs.getString(PREF_MASTER_KEY_IV, null)
            ?: throw IllegalStateException("Wrapped IV not found")
        val keystoreKey  = cryptoManager.getOrCreateKeystoreKey()
        return cryptoManager.unwrapKey(EncryptedData(wrappedCt, wrappedIv), keystoreKey)
    }

    // ── Mappers ────────────────────────────────────────────────────────────────

    private fun encrypt(entry: AccountEntry): AccountEntryEntity {
        val key       = resolveKey()
        val jsonStr   = json.encodeToString(entry)
        val encrypted = cryptoManager.encrypt(jsonStr, key)
        return AccountEntryEntity(
            id            = entry.id,
            platformType  = entry.platformType.key,
            platformLabel = entry.platformLabel,
            encryptedBlob = encrypted.ciphertext,
            iv            = encrypted.iv,
            isGameAccount = entry.gameAccount != null,
            isFavorite    = entry.isFavorite,
            createdAt     = entry.createdAt,
            updatedAt     = entry.updatedAt,
            isSynced      = false
        )
    }

    private fun decrypt(entity: AccountEntryEntity): AccountEntry {
        val key     = resolveKey()
        val jsonStr = cryptoManager.decrypt(EncryptedData(entity.encryptedBlob, entity.iv), key)
        return json.decodeFromString(jsonStr)
    }

    private fun decryptOrNull(entity: AccountEntryEntity): AccountEntry? {
        return try {
            decrypt(entity)
        } catch (e: Exception) {
            android.util.Log.e("AccountRepository", "Decryption failed for entry ${entity.id}", e)
            null
        }
    }

    // ── Observe all (Flow) ────────────────────────────────────────────────────

    override fun observeAccounts(): Flow<List<AccountEntry>> =
        vaultSession.database.accountEntryDao()
            .observeAll()
            .map { entities -> entities.mapNotNull { decryptOrNull(it) } }

    override fun searchAccounts(query: String): Flow<List<AccountEntry>> =
        vaultSession.database.accountEntryDao()
            .searchEntries(query)
            .map { entities -> entities.mapNotNull { decryptOrNull(it) } }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    override suspend fun getAccountById(id: String): AccountEntry? =
        vaultSession.database.accountEntryDao()
            .getById(id)
            ?.let { decryptOrNull(it) }

    override suspend fun getAllAccountsSync(): List<AccountEntry> =
        vaultSession.database.accountEntryDao()
            .getAllSync()
            .mapNotNull { decryptOrNull(it) }

    override suspend fun saveAccount(entry: AccountEntry): Result<Unit> = runCatching {
        val entity = encrypt(entry.copy(id = UUID.randomUUID().toString()))
        vaultSession.database.accountEntryDao().upsert(entity)
        vaultSession.database.pendingSyncDao().enqueue(
            PendingSyncEntity(entryId = entity.id, operation = "UPSERT")
        )
        triggerAutoSync()
    }.onFailure { e ->
        android.util.Log.e("AccountRepository", "saveAccount failed", e)
    }

    override suspend fun updateAccount(entry: AccountEntry): Result<Unit> = runCatching {
        val entity = encrypt(entry.copy(updatedAt = System.currentTimeMillis()))
        vaultSession.database.accountEntryDao().upsert(entity)
        vaultSession.database.pendingSyncDao().enqueue(
            PendingSyncEntity(entryId = entity.id, operation = "UPSERT")
        )
        triggerAutoSync()
    }.onFailure { e ->
        android.util.Log.e("AccountRepository", "updateAccount failed", e)
    }

    override suspend fun deleteAccount(id: String): Result<Unit> = runCatching {
        vaultSession.database.accountEntryDao().softDelete(id)
        vaultSession.database.pendingSyncDao().enqueue(
            PendingSyncEntity(entryId = id, operation = "DELETE")
        )
        triggerAutoSync()
    }.onFailure { e ->
        android.util.Log.e("AccountRepository", "deleteAccount failed", e)
    }

    override suspend fun countAccounts(): Int =
        vaultSession.database.accountEntryDao().countActive()

    // ── Cloud sync ────────────────────────────────────────────────────────────

    override suspend fun syncToCloud(): Result<Unit> = runCatching {
        val uid = authRepository.currentUid ?: return@runCatching
        val dao = vaultSession.database.accountEntryDao()
        val pendingDao = vaultSession.database.pendingSyncDao()
        val pendingOps = pendingDao.getAll()

        pendingOps.forEach { op ->
            when (op.operation) {
                "UPSERT" -> {
                    val entity = dao.getById(op.entryId) ?: return@forEach
                    val firestoreDoc = mapOf(
                        "entry_id"       to entity.id,
                        "encrypted_blob" to entity.encryptedBlob,  // ciphertext only
                        "iv"             to entity.iv,
                        "updated_at"     to entity.updatedAt,
                        "platform_hint"  to entity.platformType    // safe metadata
                    )
                    firestore.collection("vaults")
                        .document(uid)
                        .collection("entries")
                        .document(entity.id)
                        .set(firestoreDoc)
                        .await()
                    dao.markSynced(entity.id)
                }
                "DELETE" -> {
                    firestore.collection("vaults")
                        .document(uid)
                        .collection("entries")
                        .document(op.entryId)
                        .delete()
                        .await()
                }
            }
            pendingDao.dequeue(op.entryId)
        }
        if (pendingOps.isNotEmpty()) {
            encryptedPrefs.edit().putLong("last_backup_timestamp", System.currentTimeMillis()).apply()
        }
    }

    override suspend fun syncFromCloud(): Result<Unit> = runCatching {
        val uid = authRepository.currentUid ?: return@runCatching
        val dao = vaultSession.database.accountEntryDao()

        val snapshot = firestore.collection("vaults")
            .document(uid)
            .collection("entries")
            .get()
            .await()

        snapshot.documents.forEach { doc ->
            val id = doc.getString("entry_id") ?: return@forEach
            val encryptedBlob = doc.getString("encrypted_blob") ?: return@forEach
            val iv = doc.getString("iv") ?: return@forEach
            val updatedAt = doc.getLong("updated_at") ?: 0L
            val platformHint = doc.getString("platform_hint") ?: "custom"

            val localEntity = dao.getById(id)
            if (localEntity == null || updatedAt > localEntity.updatedAt) {
                try {
                    val key = resolveKey()
                    val jsonStr = cryptoManager.decrypt(EncryptedData(encryptedBlob, iv), key)
                    val accountEntry = json.decodeFromString<AccountEntry>(jsonStr)

                    val entityToSave = AccountEntryEntity(
                        id = id,
                        platformType = accountEntry.platformType.key,
                        platformLabel = accountEntry.platformLabel,
                        encryptedBlob = encryptedBlob,
                        iv = iv,
                        isGameAccount = accountEntry.gameAccount != null,
                        isFavorite = accountEntry.isFavorite,
                        createdAt = accountEntry.createdAt,
                        updatedAt = accountEntry.updatedAt,
                        isSynced = true
                    )
                    dao.upsert(entityToSave)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @kotlinx.serialization.Serializable
    private data class ExportPayload(
        val salt: String,
        val iv: String,
        val ciphertext: String
    )

    override suspend fun exportToJson(password: String, context: Context): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val accounts = getAllAccountsSync()
            val jsonString = json.encodeToString(accounts)
            
            val salt = cryptoManager.generateSalt()
            val derivedKey = cryptoManager.derivePinKey(password, salt)
            
            val encryptedData = cryptoManager.encrypt(jsonString, derivedKey)
            
            val exportPayload = json.encodeToString(
                ExportPayload(
                    salt = cryptoManager.saltToBase64(salt),
                    iv = encryptedData.iv,
                    ciphertext = encryptedData.ciphertext
                )
            )
            
            val fileName = "vaultx_backup_${System.currentTimeMillis()}.json"
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(exportPayload.toByteArray(Charsets.UTF_8))
                        stream.flush()
                    } ?: throw IllegalStateException("Could not open output stream")
                } else {
                    throw IllegalStateException("Failed to create MediaStore entry")
                }
            } else {
                val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val file = java.io.File(dir, fileName)
                file.writeText(exportPayload)
            }
        }
    }.onFailure { e ->
        android.util.Log.e("AccountRepository", "exportToJson failed", e)
    }

    private suspend fun triggerAutoSync() {
        val isCloudSyncEnabled = encryptedPrefs.getBoolean("pref_cloud_sync_enabled", false)
        if (isCloudSyncEnabled) {
            syncToCloud()
        }
    }
}
