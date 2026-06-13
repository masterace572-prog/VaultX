package com.vaultx.user.data.local.db

import androidx.room.*

// ─────────────────────────────────────────────────────────────────────────────
// Room Entities — local encrypted vault
// IMPORTANT: The plaintext account data is NEVER stored in these entities.
// Only the AES-256-GCM ciphertext blob (+ IV) is persisted by Room.
// Decryption happens in CryptoManager, never in the DAO/Repository.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents one saved account entry in the local vault.
 * [encryptedBlob] = Base64(AES-256-GCM(accountJson))
 * [iv]            = Base64(12-byte GCM nonce)
 */
@Entity(tableName = "account_entries")
data class AccountEntryEntity(
    @PrimaryKey val id: String,                // UUID v4
    val platformType: String,                  // "instagram" | "twitter" | "facebook" | "google" | "game" | "custom"
    val platformLabel: String,                 // User-visible label, e.g. "My Work Google"
    val encryptedBlob: String,                 // Base64 AES-256-GCM ciphertext
    val iv: String,                            // Base64 GCM nonce (12 bytes)
    val isGameAccount: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,             // true = has been pushed to Firestore
    val isDeleted: Boolean = false             // soft-delete for sync purposes
)

/**
 * Local user profile (tier, premium expiry, etc.)
 * Sensitive fields (master password hash) are stored in EncryptedSharedPreferences, NOT here.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val displayName: String,
    val tier: String = "free",                 // "free" | "premium"
    val premiumExpiryMs: Long? = null,
    val salt: String,                          // PBKDF2 salt — NOT the password
    val cloudSyncEnabled: Boolean = false,
    val lastSyncedAt: Long? = null
)

/**
 * Pending sync operations queue — used when device is offline.
 * On reconnect, the SyncWorker drains this queue.
 */
@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey val entryId: String,
    val operation: String,                     // "UPSERT" | "DELETE"
    val queuedAt: Long = System.currentTimeMillis()
)
