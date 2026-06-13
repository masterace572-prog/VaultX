package com.vaultx.user.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vaultx.user.data.model.Plan
import com.vaultx.user.data.model.UserTier
import com.vaultx.user.data.model.Tier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// PremiumViewModel — loads plans and user tier from Firestore
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore:    FirebaseFirestore,
) : ViewModel() {

    private val _plans     = MutableStateFlow<List<Plan>>(emptyList())
    val plans: StateFlow<List<Plan>> = _plans.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userTier = MutableStateFlow<UserTier?>(null)
    val userTier: StateFlow<UserTier?> = _userTier.asStateFlow()

    init {
        loadPlansAndTier()
    }

    private fun loadPlansAndTier() {
        viewModelScope.launch {
            runCatching {
                // Load plans
                val plansSnapshot = firestore.collection("plans")
                    .whereEqualTo("isActive", true)
                    .get().await()
                if (plansSnapshot.isEmpty) {
                    _plans.value = emptyList()
                } else {
                    _plans.value = plansSnapshot.documents.map { doc ->
                        Plan(
                            id              = doc.id,
                            name            = doc.getString("name") ?: "",
                            price           = doc.getDouble("price") ?: 0.0,
                            basePrice       = doc.getDouble("basePrice"),
                            durationDays    = doc.getLong("durationDays")?.toInt() ?: 30,
                            features        = (doc.get("features") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            isActive        = doc.getBoolean("isActive") ?: true,
                            isRecommended   = doc.getBoolean("isRecommended") ?: false,
                            orderIndex      = doc.getLong("orderIndex")?.toInt() ?: 0
                        )
                    }.sortedBy { it.orderIndex }
                }

                // Load user tier
                val uid = firebaseAuth.currentUser?.uid
                if (uid != null) {
                    val userDoc = firestore.collection("users").document(uid).get().await()
                    val tier    = userDoc.getString("tier") ?: "free"
                    val expiry  = userDoc.getLong("premium_expiry")
                    val daysLeft = expiry?.let {
                        val diff = it - System.currentTimeMillis()
                        if (diff > 0) (diff / 86_400_000L).toInt() else 0
                    }
                    _userTier.value = UserTier(
                        tier            = if (tier == "premium" && (daysLeft ?: 0) > 0) Tier.PREMIUM else Tier.FREE,
                        premiumExpiryMs = expiry
                    )
                }
            }
            _isLoading.value = false
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PaymentViewModel — UTR input + Firestore payment request submission
// ─────────────────────────────────────────────────────────────────────────────

sealed class PaymentUiState {
    object Idle      : PaymentUiState()
    object Loading   : PaymentUiState()
    object Submitted : PaymentUiState()
    data class Error(val message: String) : PaymentUiState()
}

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore:    FirebaseFirestore,
) : ViewModel() {

    private val _plan     = MutableStateFlow<Plan?>(null)
    val plan: StateFlow<Plan?> = _plan.asStateFlow()

    private val _utr      = MutableStateFlow("")
    val utr: StateFlow<String> = _utr.asStateFlow()

    val utrError: StateFlow<String?> = _utr.map { v ->
        when {
            v.isBlank()      -> null
            v.length != 12   -> "UTR must be exactly 12 digits"
            !v.all { it.isDigit() } -> "UTR must contain digits only"
            else             -> null
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _uiState  = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _upiId = MutableStateFlow("")
    val upiId: StateFlow<String> = _upiId.asStateFlow()

    private val _payeeName = MutableStateFlow("")
    val payeeName: StateFlow<String> = _payeeName.asStateFlow()

    init {
        viewModelScope.launch {
            val doc = firestore.collection("app_config").document("main").get().await()
            _upiId.value = doc.getString("upi_id") ?: ""
            _payeeName.value = doc.getString("payee_name") ?: ""
        }
    }

    private val _appliedPromo = MutableStateFlow<com.vaultx.user.data.model.PromoCode?>(null)
    val appliedPromo: StateFlow<com.vaultx.user.data.model.PromoCode?> = _appliedPromo.asStateFlow()

    private val _promoError = MutableStateFlow<String?>(null)
    val promoError: StateFlow<String?> = _promoError.asStateFlow()

    fun onUtrChanged(v: String) { _utr.value = v.filter { it.isDigit() }.take(12) }

    fun applyPromoCode(code: String) {
        viewModelScope.launch {
            _promoError.value = null
            runCatching {
                val doc = firestore.collection("promo_codes").document(code.uppercase()).get().await()
                if (doc.exists() && doc.getBoolean("isActive") == true) {
                    val maxUses = doc.getLong("maxUses")?.toInt() ?: 100
                    val currentUses = doc.getLong("currentUses")?.toInt() ?: 0
                    if (currentUses < maxUses) {
                        _appliedPromo.value = com.vaultx.user.data.model.PromoCode(
                            id = doc.id,
                            code = doc.getString("code") ?: "",
                            discountPercent = doc.getDouble("discountPercent"),
                            freePlanId = doc.getString("freePlanId"),
                            maxUses = maxUses,
                            currentUses = currentUses,
                            isActive = true
                        )
                    } else {
                        _promoError.value = "Promo code has reached its usage limit."
                    }
                } else {
                    _promoError.value = "Invalid or expired promo code."
                }
            }.onFailure {
                _promoError.value = "Error validating promo code."
            }
        }
    }

    fun loadPlan(planId: String) {
        viewModelScope.launch {
            runCatching {
                val doc = firestore.collection("plans").document(planId).get().await()
                if (doc.exists()) {
                    _plan.value = Plan(
                        id              = doc.id,
                        name            = doc.getString("name") ?: "",
                        price           = doc.getDouble("price") ?: 0.0,
                        basePrice       = doc.getDouble("basePrice"),
                        durationDays    = doc.getLong("durationDays")?.toInt() ?: 30,
                        features        = (doc.get("features") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        isActive        = doc.getBoolean("isActive") ?: true,
                        isRecommended   = doc.getBoolean("isRecommended") ?: false,
                        orderIndex      = doc.getLong("orderIndex")?.toInt() ?: 0
                    )
                } else {
                    _plan.value = null
                }
            }
        }
    }

    fun submitPayment(planId: String, onResult: (Boolean, String?) -> Unit) {
        val promo = _appliedPromo.value
        if (promo?.freePlanId == planId) {
            // It's a 100% free plan promo code! Grant premium instantly.
            viewModelScope.launch {
                _uiState.value = PaymentUiState.Loading
                runCatching {
                    val uid = firebaseAuth.currentUser?.uid ?: throw IllegalStateException("Not logged in")
                    val planToGrant = _plan.value ?: throw IllegalStateException("Plan not loaded")
                    val currentExpiry = firestore.collection("users").document(uid).get().await().getLong("premium_expiry") ?: System.currentTimeMillis()
                    val newExpiry = maxOf(currentExpiry, System.currentTimeMillis()) + (planToGrant.durationDays * 86_400_000L)
                    
                    firestore.collection("users").document(uid).update(
                        mapOf(
                            "tier" to "premium",
                            "premium_expiry" to newExpiry
                        )
                    ).await()

                    // Increment promo code usage
                    firestore.collection("promo_codes").document(promo.id).update(
                        "currentUses", promo.currentUses + 1
                    ).await()

                    _uiState.value = PaymentUiState.Submitted
                    onResult(true, "Promo applied! You are now premium.")
                }.onFailure { e ->
                    val msg = e.message ?: "Failed to apply free promo code"
                    _uiState.value = PaymentUiState.Error(msg)
                    onResult(false, msg)
                }
            }
            return
        }

        val utrValue = _utr.value
        if (utrValue.length != 12) {
            onResult(false, "Invalid UTR")
            return
        }

        viewModelScope.launch {
            _uiState.value = PaymentUiState.Loading
            runCatching {
                val uid = firebaseAuth.currentUser?.uid
                    ?: throw IllegalStateException("Not logged in")
                val paymentId = UUID.randomUUID().toString()
                val paymentDoc = mapOf(
                    "payment_id"   to paymentId,
                    "uid"          to uid,
                    "utr"          to utrValue,
                    "plan_id"      to planId,
                    "status"       to "pending",
                    "submitted_at" to com.google.firebase.Timestamp.now(),
                    "reviewed_at"  to null,
                    "reviewed_by"  to null
                )
                firestore.collection("payments").document(paymentId).set(paymentDoc).await()
                _uiState.value = PaymentUiState.Submitted
                onResult(true, "Payment request submitted successfully")
            }.onFailure { e ->
                val msg = e.message ?: "Failed to submit payment"
                _uiState.value = PaymentUiState.Error(msg)
                onResult(false, msg)
            }
        }
    }
}
