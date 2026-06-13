package com.vaultx.admin.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String,
    val email: String,
    val name: String?,
    val tier: String,
    val premiumExpiry: Long?,
    val createdAt: Timestamp?
)

data class Payment(
    val paymentId: String,
    val uid: String,
    val userEmail: String,
    val utr: String,
    val planId: String,
    val status: String,
    val submittedAt: Timestamp?,
    val reviewedAt: Timestamp?,
    val reviewedBy: String?
)

data class AppConfig(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val apkDownloadUrl: String,
    val changelog: List<String>,
    val isForcedUpdate: Boolean,
    val announcement: String,
    val isAnnouncementActive: Boolean,
    val upiId: String = "",
    val payeeName: String = "",
    val isMaintenanceMode: Boolean = false,
    val maintenanceMessage: String = "We are currently under maintenance. Please check back later.",
    val isAutofillEnabled: Boolean = true,
    val isSignupEnabled: Boolean = true,
    val maxFreeAccounts: Int = 5,
    val supportEmail: String = ""
)

data class PromoCode(
    val id: String = "",
    val code: String = "",
    val discountPercent: Double? = null,
    val freePlanId: String? = null, // Plan ID to grant instantly if it's a 100% free plan coupon
    val maxUses: Int = 100,
    val currentUses: Int = 0,
    val isActive: Boolean = true
)
