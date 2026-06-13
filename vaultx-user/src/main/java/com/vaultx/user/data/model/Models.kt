package com.vaultx.user.data.model

import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────────────────
// Domain Models
// These are the PLAINTEXT structures. They are NEVER stored directly.
// They are serialized to JSON → encrypted → stored as ciphertext blobs.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The complete account entry — serialized and then AES-GCM encrypted.
 * All fields below live only in memory; never touch disk or network in plaintext.
 */
@Serializable
data class AccountEntry(
    val id: String,
    val platformType: PlatformType,
    val platformLabel: String,

    // Standard fields (all optional)
    val username: String? = null,
    val email: String? = null,
    val password: String,               // PLAINTEXT — only in memory
    val websiteUrl: String = "",
    val appPackageName: String = "",

    // Game-specific fields
    val gameAccount: GameAccountDetails? = null,

    // Metadata
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class GameAccountDetails(
    val gameName: String = "",
    val inGameId: String? = null,
    val description: String? = null
)

@Serializable
enum class PlatformType(val displayName: String, val key: String) {
    INSTAGRAM("Instagram",  "instagram"),
    TWITTER  ("Twitter/X",  "twitter"),
    FACEBOOK ("Facebook",   "facebook"),
    GOOGLE   ("Google",     "google"),
    DISCORD  ("Discord",    "discord"),
    GAME     ("Game",       "game"),
    CUSTOM   ("Custom",     "custom");

    companion object {
        fun fromKey(key: String) = values().firstOrNull { it.key == key } ?: CUSTOM
    }
}

// ── Firestore-safe representation (no plaintext!) ─────────────────────────────

data class VaultEntryFirestore(
    val entryId: String      = "",
    val encryptedBlob: String = "",  // Base64 AES-GCM ciphertext
    val iv: String           = "",   // Base64 nonce
    val updatedAt: Long      = 0L,
    val platformHint: String = ""    // e.g. "instagram" — visible to admin but harmless
)

// ── User profile models ───────────────────────────────────────────────────────

data class UserTier(
    val tier: Tier,
    val premiumExpiryMs: Long? = null
) {
    val isPremium: Boolean get() = tier == Tier.PREMIUM
    val daysLeft: Int?
        get() = premiumExpiryMs?.let {
            val diff = it - System.currentTimeMillis()
            if (diff > 0) (diff / 86_400_000L).toInt() else 0
        }
}

enum class Tier { FREE, PREMIUM }

// ── Payment request model ─────────────────────────────────────────────────────

data class PaymentRequest(
    val paymentId: String   = "",
    val uid: String         = "",
    val utr: String         = "",     // 12-digit UTR
    val planId: String      = "",
    val status: PaymentStatus = PaymentStatus.PENDING,
    val submittedAt: Long   = System.currentTimeMillis(),
    val reviewedAt: Long?   = null,
    val reviewedBy: String? = null
)

enum class PaymentStatus { PENDING, APPROVED, REJECTED }

// ── App config model (read from Firestore) ────────────────────────────────────

data class AppConfig(
    val latestVersionCode: Int         = 0,
    val latestVersionName: String      = "",
    val apkDownloadUrl: String         = "",
    val changelog: List<String>        = emptyList(),
    val isForcedUpdate: Boolean        = false,
    val announcementText: String       = "",
    val announcementActive: Boolean    = false,
    val enabledTemplates: List<String> = listOf("instagram","twitter","facebook","google","custom"),
    val upiId: String                  = "",
    val payeeName: String              = "",
    val isMaintenanceMode: Boolean     = false,
    val maintenanceMessage: String     = "We are currently under maintenance. Please check back later."
)

// ── Plan model ────────────────────────────────────────────────────────────────

data class Plan(
    val id: String = "",
    val name: String = "",
    val price: Double              = 0.0,
    val basePrice: Double?         = null,
    val durationDays: Int          = 30,
    val features: List<String>     = emptyList(),
    val isActive: Boolean          = true,
    val isRecommended: Boolean     = false,
    val orderIndex: Int            = 0
)

// ── Promo Code model ─────────────────────────────────────────────────────────

data class PromoCode(
    val id: String = "",
    val code: String = "",
    val discountPercent: Double? = null,
    val freePlanId: String? = null,
    val maxUses: Int = 100,
    val currentUses: Int = 0,
    val isActive: Boolean = true
)
