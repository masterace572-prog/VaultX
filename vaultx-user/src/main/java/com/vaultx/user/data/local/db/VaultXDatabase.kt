package com.vaultx.user.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

// ─────────────────────────────────────────────────────────────────────────────
// VaultXDatabase — SQLCipher-encrypted Room database
// The database file itself is encrypted at rest using the user's derived key.
// ─────────────────────────────────────────────────────────────────────────────

@Database(
    entities = [
        AccountEntryEntity::class,
        UserProfileEntity::class,
        PendingSyncEntity::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class VaultXDatabase : RoomDatabase() {
    abstract fun accountEntryDao(): AccountEntryDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        private const val DB_NAME = "vaultx.db"

        /**
         * Builds the encrypted Room database.
         * [passphrase] = raw bytes of the AES key derived from PBKDF2.
         *                It is NEVER stored; caller must supply it on each launch.
         */
        fun buildEncrypted(context: Context, passphrase: ByteArray): VaultXDatabase {
            try {
                SQLiteDatabase.loadLibs(context.applicationContext)
            } catch (e: Exception) {
                // Ignore library load errors if already loaded
            }
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                VaultXDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration() // Replace with proper Migrations in prod
                .build()
        }
    }
}
