package com.vaultx.user.presentation.navigation

// ─────────────────────────────────────────────────────────────────────────────
// Screen — Sealed class defining all navigation routes
// ─────────────────────────────────────────────────────────────────────────────

sealed class Screen(val route: String) {

    // ── Auth flow ─────────────────────────────────────────────────────────────
    object VaultLock     : Screen("vault_lock")
    object Login         : Screen("login")
    object Register      : Screen("register")

    // ── Main app ──────────────────────────────────────────────────────────────
    object Home          : Screen("home")
    object AddAccount    : Screen("add_account")
    object AccountDetail : Screen("account_detail/{entryId}") {
        fun createRoute(entryId: String) = "account_detail/$entryId"
    }
    object EditAccount   : Screen("edit_account/{entryId}") {
        fun createRoute(entryId: String) = "edit_account/$entryId"
    }
    object Search        : Screen("search")

    // ── Settings ──────────────────────────────────────────────────────────────
    object Settings      : Screen("settings")
    object Profile       : Screen("profile")
    object AppLockSetup  : Screen("app_lock_setup")
    object HelpSupport   : Screen("help_support")

    // ── Payment / Premium ─────────────────────────────────────────────────────
    object Premium       : Screen("premium")
    object Payment       : Screen("payment/{planId}") {
        fun createRoute(planId: String) = "payment/$planId"
    }
}
