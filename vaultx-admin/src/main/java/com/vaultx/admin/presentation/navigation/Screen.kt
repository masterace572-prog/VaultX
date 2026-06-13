package com.vaultx.admin.presentation.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Dashboard : Screen("dashboard")
    object ManageUsers : Screen("manage_users")
    object PaymentApprovals : Screen("payment_approvals")
    object AppConfig : Screen("app_config")
    object ManagePlans : Screen("manage_plans")
}
