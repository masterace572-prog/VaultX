package com.vaultx.user.domain.repository

import com.vaultx.user.data.model.*
import com.vaultx.user.data.local.db.UserProfileEntity
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// Repository interfaces — domain layer contracts
// ─────────────────────────────────────────────────────────────────────────────

interface AuthRepository {
    val currentUid: String?
    val isLoggedIn: Boolean

    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(email: String, password: String, displayName: String): Result<Unit>
    suspend fun logout()
    suspend fun setupAppPin(pin: String): Result<Unit>
    suspend fun unlockWithPin(pin: String): Result<Unit>
    suspend fun unlockWithMasterPassword(password: String): Result<Unit>
    suspend fun getUserProfile(): Result<UserProfileEntity?>
    suspend fun fetchAndCacheRemoteProfile(): Result<Unit>
}

interface AccountRepository {
    fun observeAccounts(): Flow<List<AccountEntry>>
    fun searchAccounts(query: String): Flow<List<AccountEntry>>
    suspend fun getAccountById(id: String): AccountEntry?
    suspend fun getAllAccountsSync(): List<AccountEntry>
    suspend fun saveAccount(entry: AccountEntry): Result<Unit>
    suspend fun updateAccount(entry: AccountEntry): Result<Unit>
    suspend fun deleteAccount(id: String): Result<Unit>
    suspend fun countAccounts(): Int
    suspend fun syncToCloud(): Result<Unit>
    suspend fun syncFromCloud(): Result<Unit>
    suspend fun exportToJson(password: String, context: android.content.Context): Result<Unit>
}

interface AppConfigRepository {
    suspend fun fetchAppConfig(): Result<AppConfig>
    suspend fun fetchPlans(): Result<List<Plan>>
    suspend fun submitPaymentRequest(utr: String, planId: String): Result<Unit>
}
