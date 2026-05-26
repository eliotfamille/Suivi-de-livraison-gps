package com.nenykely.front_kotlin.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nenykely.front_kotlin.ui.screens.*
import com.nenykely.front_kotlin.viewmodel.AuthViewModel
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Deliveries : Screen("deliveries")
    object Tracking : Screen("tracking/{deliveryId}") {
        fun createRoute(deliveryId: Int) = "tracking/$deliveryId"
    }
    object DriverProfile : Screen("driver_profile/{driverId}") {
        fun createRoute(driverId: Int) = "driver_profile/$driverId"
    }
    object Profile : Screen("profile")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    deliveryViewModel: DeliveryViewModel
) {
    val token by authViewModel.token.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
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
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(onNavigateToDeliveries = {
                navController.navigate(Screen.Deliveries.route)
            })
        }
        composable(Screen.Deliveries.route) {
            token?.let { t ->
                DeliveriesScreen(t, deliveryViewModel, onDeliveryClick = { id ->
                    navController.navigate(Screen.Tracking.createRoute(id))
                })
            }
        }
        composable(Screen.Tracking.route) { backStackEntry ->
            val deliveryId = backStackEntry.arguments?.getString("deliveryId")?.toIntOrNull()
            token?.let { t ->
                deliveryId?.let { id ->
                    TrackingScreen(t, id, deliveryViewModel, onBack = {
                        navController.popBackStack()
                    }, onNavigateToDriver = { driverId ->
                        navController.navigate(Screen.DriverProfile.createRoute(driverId))
                    })
                }
            }
        }
        composable(Screen.DriverProfile.route) { backStackEntry ->
            val driverId = backStackEntry.arguments?.getString("driverId")?.toIntOrNull()
            token?.let { t ->
                driverId?.let { id ->
                    DriverProfileScreen(id, deliveryViewModel, onBack = {
                        navController.popBackStack()
                    })
                }
            }
        }
        composable(Screen.Profile.route) {
            ProfileScreen(authViewModel, onLogout = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            })
        }
    }
}
