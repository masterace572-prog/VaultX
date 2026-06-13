package com.vaultx.user.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// Account Entries DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface AccountEntryDao {

    @Query("SELECT * FROM account_entries WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<AccountEntryEntity>>

    @Query("SELECT * FROM account_entries WHERE isDeleted = 0")
    suspend fun getAllSync(): List<AccountEntryEntity>

    @Query("""
        SELECT * FROM account_entries
        WHERE isDeleted = 0
          AND (platformLabel LIKE '%' || :query || '%' OR platformType LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun searchEntries(query: String): Flow<List<AccountEntryEntity>>

    @Query("SELECT * FROM account_entries WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: String): AccountEntryEntity?

    @Query("SELECT COUNT(*) FROM account_entries WHERE isDeleted = 0")
    suspend fun countActive(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: AccountEntryEntity)

    @Query("UPDATE account_entries SET isDeleted = 1, isSynced = 0, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE account_entries SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT * FROM account_entries WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsynced(): List<AccountEntryEntity>

    @Query("SELECT * FROM account_entries WHERE isDeleted = 1 AND isSynced = 0")
    suspend fun getPendingDeletions(): List<AccountEntryEntity>

    @Query("DELETE FROM account_entries WHERE isDeleted = 1")
    suspend fun purgeDeleted()

    @Query("UPDATE account_entries SET isFavorite = :isFav WHERE id = :id")
    suspend fun setFavorite(id: String, isFav: Boolean)
}

// ─────────────────────────────────────────────────────────────────────────────
// User Profile DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET tier = :tier, premiumExpiryMs = :expiryMs WHERE uid = :uid")
    suspend fun updateTier(uid: String, tier: String, expiryMs: Long?)

    @Query("UPDATE user_profile SET cloudSyncEnabled = :enabled WHERE uid = :uid")
    suspend fun updateCloudSync(uid: String, enabled: Boolean)

    @Query("UPDATE user_profile SET lastSyncedAt = :ts WHERE uid = :uid")
    suspend fun updateLastSynced(uid: String, ts: Long)

    @Query("DELETE FROM user_profile")
    suspend fun clearAll()
}

// ─────────────────────────────────────────────────────────────────────────────
// Pending Sync DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface PendingSyncDao {

    @Query("SELECT * FROM pending_sync ORDER BY queuedAt ASC")
    suspend fun getAll(): List<PendingSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: PendingSyncEntity)

    @Query("DELETE FROM pending_sync WHERE entryId = :entryId")
    suspend fun dequeue(entryId: String)

    @Query("DELETE FROM pending_sync")
    suspend fun clearAll()
}
