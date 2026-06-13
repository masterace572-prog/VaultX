package com.vaultx.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vaultx.admin.data.model.AppConfig
import com.vaultx.admin.data.model.Payment
import com.vaultx.admin.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.vaultx.admin.data.model.Plan

import com.google.firebase.auth.FirebaseAuth

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val themePreferences: com.vaultx.admin.data.local.ThemePreferences
) : ViewModel() {

    private val _authState = MutableStateFlow<Boolean>(auth.currentUser != null)
    val authState: StateFlow<Boolean> = _authState.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _payments = MutableStateFlow<List<Payment>>(emptyList())
    val payments: StateFlow<List<Payment>> = _payments.asStateFlow()

    private val _appConfig = MutableStateFlow<AppConfig?>(null)
    val appConfig: StateFlow<AppConfig?> = _appConfig.asStateFlow()

    private val _plans = MutableStateFlow<List<Plan>>(emptyList())
    val plans: StateFlow<List<Plan>> = _plans.asStateFlow()

    private val _promoCodes = MutableStateFlow<List<com.vaultx.admin.data.model.PromoCode>>(emptyList())
    val promoCodes: StateFlow<List<com.vaultx.admin.data.model.PromoCode>> = _promoCodes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val isDarkMode: StateFlow<Boolean?> = themePreferences.isDarkMode
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, null)

    init {
        if (auth.currentUser != null) {
            loadData()
        }
    }

    fun login(email: String, pass: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                _authState.value = true
                loadData()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.localizedMessage ?: "Login failed")
                _isLoading.value = false
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load Users
                val usersSnapshot = firestore.collection("users")
                    .get().await()
                _users.value = usersSnapshot.documents.map { doc ->
                    User(
                        uid = doc.id,
                        email = doc.getString("email") ?: "",
                        name = doc.getString("name"),
                        tier = doc.getString("tier") ?: "free",
                        premiumExpiry = doc.getLong("premium_expiry"),
                        createdAt = doc.getTimestamp("created_at")
                    )
                }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }

                // Load Payments
                val paymentsSnapshot = firestore.collection("payments")
                    .orderBy("submitted_at", Query.Direction.DESCENDING)
                    .get().await()
                _payments.value = paymentsSnapshot.documents.map { doc ->
                    Payment(
                        paymentId = doc.id,
                        uid = doc.getString("uid") ?: "",
                        userEmail = "User ID: ${doc.getString("uid")}", // Fallback, could join with users
                        utr = doc.getString("utr") ?: "",
                        planId = doc.getString("plan_id") ?: "",
                        status = doc.getString("status") ?: "pending",
                        submittedAt = doc.getTimestamp("submitted_at"),
                        reviewedAt = doc.getTimestamp("reviewed_at"),
                        reviewedBy = doc.getString("reviewed_by")
                    )
                }

                // Load Config
                val configDoc = firestore.collection("app_config").document("main").get().await()
                if (configDoc.exists()) {
                    _appConfig.value = AppConfig(
                        latestVersionCode = configDoc.getLong("latest_version_code")?.toInt() ?: 1,
                        latestVersionName = configDoc.getString("latest_version_name") ?: "1.0",
                        apkDownloadUrl = configDoc.getString("apk_download_url") ?: "",
                        changelog = (configDoc.get("changelog") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        isForcedUpdate = configDoc.getBoolean("is_forced_update") ?: false,
                        announcement = configDoc.getString("announcement") ?: "",
                        isAnnouncementActive = configDoc.getBoolean("is_announcement_active") ?: false,
                        upiId = configDoc.getString("upi_id") ?: "",
                        payeeName = configDoc.getString("payee_name") ?: "",
                        isMaintenanceMode = configDoc.getBoolean("is_maintenance_mode") ?: false,
                        maintenanceMessage = configDoc.getString("maintenance_message") ?: "We are currently under maintenance. Please check back later.",
                        isAutofillEnabled = configDoc.getBoolean("is_autofill_enabled") ?: true,
                        isSignupEnabled = configDoc.getBoolean("is_signup_enabled") ?: true,
                        maxFreeAccounts = configDoc.getLong("max_free_accounts")?.toInt() ?: 5,
                        isScreenshotAllowed = configDoc.getBoolean("is_screenshot_allowed") ?: false,
                        supportEmail = configDoc.getString("support_email") ?: "",
                        discordLink = configDoc.getString("discord_link") ?: "",
                        updateDialogMessage = configDoc.getString("update_dialog_message") ?: "A new update is available. Please update to the latest version."
                    )
                }

                // Load Plans
                val plansSnapshot = firestore.collection("plans")
                    .orderBy("orderIndex")
                    .get().await()
                _plans.value = plansSnapshot.documents.map { doc ->
                    Plan(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        basePrice = doc.getDouble("basePrice"),
                        durationDays = doc.getLong("durationDays")?.toInt() ?: 30,
                        features = (doc.get("features") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        isActive = doc.getBoolean("isActive") ?: true,
                        isRecommended = doc.getBoolean("isRecommended") ?: false,
                        orderIndex = doc.getLong("orderIndex")?.toInt() ?: 0
                    )
                }

                // Load Promo Codes
                val promoSnapshot = firestore.collection("promo_codes").get().await()
                _promoCodes.value = promoSnapshot.documents.map { doc ->
                    com.vaultx.admin.data.model.PromoCode(
                        id = doc.id,
                        code = doc.getString("code") ?: "",
                        discountPercent = doc.getDouble("discountPercent"),
                        freePlanId = doc.getString("freePlanId"),
                        maxUses = doc.getLong("maxUses")?.toInt() ?: 100,
                        currentUses = doc.getLong("currentUses")?.toInt() ?: 0,
                        isActive = doc.getBoolean("isActive") ?: true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Admin Actions
    fun approvePayment(paymentId: String, uid: String, planDays: Int) {
        viewModelScope.launch {
            try {
                val newExpiry = System.currentTimeMillis() + (planDays * 86_400_000L)
                firestore.runBatch { batch ->
                    // Update Payment
                    val paymentRef = firestore.collection("payments").document(paymentId)
                    batch.update(paymentRef, mapOf(
                        "status" to "approved",
                        "reviewed_at" to com.google.firebase.Timestamp.now(),
                        "reviewed_by" to "admin"
                    ))
                    // Update User
                    val userRef = firestore.collection("users").document(uid)
                    batch.update(userRef, mapOf(
                        "tier" to "premium",
                        "premium_expiry" to newExpiry
                    ))
                }.await()
                loadData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun rejectPayment(paymentId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("payments").document(paymentId).update(mapOf(
                    "status" to "rejected",
                    "reviewed_at" to com.google.firebase.Timestamp.now(),
                    "reviewed_by" to "admin"
                )).await()
                loadData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateAppConfig(config: AppConfig) {
        viewModelScope.launch {
            try {
                firestore.collection("app_config").document("main").set(mapOf(
                    "latest_version_code" to config.latestVersionCode,
                    "latest_version_name" to config.latestVersionName,
                    "apk_download_url" to config.apkDownloadUrl,
                    "changelog" to config.changelog,
                    "is_forced_update" to config.isForcedUpdate,
                    "announcement" to config.announcement,
                    "is_announcement_active" to config.isAnnouncementActive,
                    "upi_id" to config.upiId,
                    "payee_name" to config.payeeName,
                    "is_maintenance_mode" to config.isMaintenanceMode,
                    "maintenance_message" to config.maintenanceMessage,
                    "is_autofill_enabled" to config.isAutofillEnabled,
                    "is_signup_enabled" to config.isSignupEnabled,
                    "max_free_accounts" to config.maxFreeAccounts,
                    "is_screenshot_allowed" to config.isScreenshotAllowed,
                    "support_email" to config.supportEmail,
                    "discord_link" to config.discordLink,
                    "update_dialog_message" to config.updateDialogMessage,
                    "updated_at" to com.google.firebase.Timestamp.now()
                )).await()
                loadData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun grantManualPremium(uid: String, days: Int) {
        viewModelScope.launch {
            try {
                val newExpiry = System.currentTimeMillis() + (days * 86_400_000L)
                firestore.collection("users").document(uid).update(mapOf(
                    "tier" to "premium",
                    "premium_expiry" to newExpiry
                )).await()
                loadData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun revokePremium(uid: String) {
        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid).update(mapOf(
                    "tier" to "free",
                    "premium_expiry" to null
                )).await()
                loadData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.saveTheme(enabled)
        }
    }

    // Plan Management
    fun createPlan(plan: Plan) {
        viewModelScope.launch {
            try {
                firestore.collection("plans").add(mapOf(
                    "name" to plan.name,
                    "price" to plan.price,
                    "basePrice" to plan.basePrice,
                    "durationDays" to plan.durationDays,
                    "features" to plan.features,
                    "isActive" to plan.isActive,
                    "isRecommended" to plan.isRecommended,
                    "orderIndex" to plan.orderIndex
                )).await()
                loadData()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun updatePlan(plan: Plan) {
        if (plan.id.isBlank()) return
        viewModelScope.launch {
            try {
                firestore.collection("plans").document(plan.id).set(mapOf(
                    "name" to plan.name,
                    "price" to plan.price,
                    "basePrice" to plan.basePrice,
                    "durationDays" to plan.durationDays,
                    "features" to plan.features,
                    "isActive" to plan.isActive,
                    "isRecommended" to plan.isRecommended,
                    "orderIndex" to plan.orderIndex
                )).await()
                loadData()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deletePlan(planId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("plans").document(planId).delete().await()
                loadData()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // Promo Code Management
    fun createPromoCode(promo: com.vaultx.admin.data.model.PromoCode) {
        val code = promo.code.uppercase().trim()
        if (code.isBlank()) return
        viewModelScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "code" to code,
                    "discountPercent" to promo.discountPercent,
                    "freePlanId" to promo.freePlanId,
                    "maxUses" to promo.maxUses,
                    "currentUses" to 0,
                    "isActive" to promo.isActive
                )
                firestore.collection("promo_codes").document(code).set(data).await()
                loadData()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun updatePromoCode(promo: com.vaultx.admin.data.model.PromoCode) {
        if (promo.id.isBlank()) return
        viewModelScope.launch {
            try {
                firestore.collection("promo_codes").document(promo.id).update(mapOf(
                    "discountPercent" to promo.discountPercent,
                    "freePlanId" to promo.freePlanId,
                    "maxUses" to promo.maxUses,
                    "currentUses" to promo.currentUses,
                    "isActive" to promo.isActive
                )).await()
                loadData()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deletePromoCode(promoId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("promo_codes").document(promoId).delete().await()
                loadData()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}
