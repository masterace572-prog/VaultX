package com.vaultx.admin.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vaultx.admin.presentation.ui.AdminDashboardScreen
import com.vaultx.admin.presentation.ui.AppConfigScreen
import com.vaultx.admin.presentation.ui.ManageUsersScreen
import com.vaultx.admin.presentation.ui.PaymentApprovalsScreen
import com.vaultx.admin.presentation.ui.AdminAuthScreen
import com.vaultx.admin.presentation.ui.ManagePlansScreen
import com.vaultx.admin.presentation.ui.AppUpdateManagerScreen
import com.vaultx.admin.presentation.ui.PromoCodesScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import com.vaultx.admin.presentation.AdminViewModel

@Composable
fun AdminNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route,
        enterTransition  = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
        exitTransition   = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
        popEnterTransition  = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) },
        popExitTransition   = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) },
    ) {
        composable(route = Screen.Auth.route) {
            val viewModel: AdminViewModel = hiltViewModel()
            val isLoggedIn by viewModel.authState.collectAsState()

            if (isLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            } else {
                AdminAuthScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    },
                    viewModel = viewModel
                )
            }
        }

        composable(route = Screen.Dashboard.route) {
            AdminDashboardScreen(
                onNavigateToUsers = { navController.navigate(Screen.ManageUsers.route) },
                onNavigateToPayments = { navController.navigate(Screen.PaymentApprovals.route) },
                onNavigateToConfig = { navController.navigate(Screen.AppConfig.route) },
                onNavigateToUpdates = { navController.navigate("app_update") },
                onNavigateToPlans = { navController.navigate(Screen.ManagePlans.route) },
                onNavigateToPromoCodes = { navController.navigate("promo_codes") }
            )
        }

        composable(route = Screen.ManageUsers.route) {
            ManageUsersScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.PaymentApprovals.route) {
            PaymentApprovalsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.AppConfig.route) {
            AppConfigScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = "app_update") {
            AppUpdateManagerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.ManagePlans.route) {
            ManagePlansScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = "promo_codes") {
            PromoCodesScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
