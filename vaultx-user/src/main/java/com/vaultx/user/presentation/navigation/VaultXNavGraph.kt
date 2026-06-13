package com.vaultx.user.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.vaultx.user.presentation.ui.auth.VaultLockScreen
import com.vaultx.user.presentation.ui.auth.LoginScreen
import com.vaultx.user.presentation.ui.auth.RegisterScreen
import com.vaultx.user.presentation.ui.home.HomeScreen
import com.vaultx.user.presentation.ui.home.SearchScreen
import com.vaultx.user.presentation.ui.account.AddAccountScreen
import com.vaultx.user.presentation.ui.account.AccountDetailScreen
import com.vaultx.user.presentation.ui.account.EditAccountScreen
import com.vaultx.user.presentation.ui.settings.SettingsScreen
import com.vaultx.user.presentation.ui.settings.AppLockSetupScreen
import com.vaultx.user.presentation.ui.settings.EditProfileScreen
import com.vaultx.user.presentation.ui.settings.HelpSupportScreen
import com.vaultx.user.presentation.ui.premium.PremiumScreen
import com.vaultx.user.presentation.ui.premium.PaymentScreen
import com.vaultx.user.presentation.viewmodel.AppViewModel
import com.vaultx.user.presentation.viewmodel.AuthState

// ─────────────────────────────────────────────────────────────────────────────
// VaultXNavGraph — Main Compose Navigation graph with slide transitions
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VaultXNavGraph(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val authState by appViewModel.authState.collectAsState()

    // Determine start destination based on auth state
    val startDestination = when (authState) {
        AuthState.Unauthenticated -> Screen.Login.route
        AuthState.NeedsVaultUnlock -> Screen.VaultLock.route
        AuthState.Authenticated -> Screen.Home.route
        AuthState.Loading -> Screen.VaultLock.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition  = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
        exitTransition   = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
        popEnterTransition  = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) },
        popExitTransition   = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) },
    ) {

        // ── Auth flow ─────────────────────────────────────────────────────────
        composable(
            route = Screen.VaultLock.route,
            enterTransition = { fadeIn(tween(400)) },
            exitTransition  = { fadeOut(tween(400)) }
        ) {
            VaultLockScreen(
                onUnlockSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.VaultLock.route) { inclusive = true }
                    }
                },
                onFallbackToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.VaultLock.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Login.route,
            enterTransition = { fadeIn(tween(400)) },
            exitTransition  = { fadeOut(tween(300)) }
        ) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(route = Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ── Home ──────────────────────────────────────────────────────────────
        composable(
            route = Screen.Home.route,
            enterTransition = { fadeIn(tween(350)) },
            exitTransition  = { fadeOut(tween(250)) }
        ) {
            HomeScreen(
                onNavigateToAdd     = { navController.navigate(Screen.AddAccount.route) },
                onNavigateToDetail  = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
                onNavigateToSearch  = { navController.navigate(Screen.Search.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPremium = { navController.navigate(Screen.Premium.route) },
            )
        }

        // ── Search ────────────────────────────────────────────────────────────
        composable(
            route = Screen.Search.route,
            enterTransition = { fadeIn(tween(250)) },
            exitTransition  = { fadeOut(tween(200)) }
        ) {
            SearchScreen(
                onNavigateToDetail = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
                onBack             = { navController.popBackStack() }
            )
        }

        // ── Add Account ───────────────────────────────────────────────────────
        composable(route = Screen.AddAccount.route) {
            AddAccountScreen(
                onSaved  = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        // ── Account Detail ────────────────────────────────────────────────────
        composable(
            route = Screen.AccountDetail.route,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType })
        ) { back ->
            val entryId = back.arguments?.getString("entryId") ?: return@composable
            AccountDetailScreen(
                entryId = entryId,
                onEdit  = { navController.navigate(Screen.EditAccount.createRoute(entryId)) },
                onBack  = { navController.popBackStack() }
            )
        }

        // ── Edit Account ──────────────────────────────────────────────────────
        composable(
            route = Screen.EditAccount.route,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType })
        ) { back ->
            val entryId = back.arguments?.getString("entryId") ?: return@composable
            EditAccountScreen(
                entryId = entryId,
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onBack     = { navController.popBackStack() },
                onLogout   = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToAppLockSetup = { navController.navigate(Screen.AppLockSetup.route) },
                onNavigateToPremium = { navController.navigate(Screen.Premium.route) },
                onNavigateToHelpSupport = { navController.navigate(Screen.HelpSupport.route) }
            )
        }

        composable(route = Screen.AppLockSetup.route) {
            AppLockSetupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Profile.route) {
            EditProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.HelpSupport.route) {
            HelpSupportScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Premium ───────────────────────────────────────────────────────────
        composable(route = Screen.Premium.route) {
            PremiumScreen(
                onSelectPlan = { planId -> navController.navigate(Screen.Payment.createRoute(planId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Payment.route,
            arguments = listOf(navArgument("planId") { type = NavType.StringType })
        ) { back ->
            val planId = back.arguments?.getString("planId") ?: return@composable
            PaymentScreen(
                planId = planId,
                onSubmitted = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
