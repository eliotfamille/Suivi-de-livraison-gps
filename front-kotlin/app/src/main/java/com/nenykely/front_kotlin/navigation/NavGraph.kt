package com.nenykely.front_kotlin.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nenykely.front_kotlin.ui.screens.client.*
import com.nenykely.front_kotlin.ui.screens.driver.*
import com.nenykely.front_kotlin.ui.screens.common.*
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
    object DeliveryDetails : Screen("delivery_details/{deliveryId}") {
        fun createRoute(deliveryId: Int) = "delivery_details/$deliveryId"
    }
    object DriverProfile : Screen("driver_profile/{driverId}") {
        fun createRoute(driverId: Int) = "driver_profile/$driverId"
    }
    object DriverMission : Screen("driver_mission/{deliveryId}") {
        fun createRoute(deliveryId: Int) = "driver_mission/$deliveryId"
    }
    object Signature : Screen("signature/{deliveryId}") {
        fun createRoute(deliveryId: Int) = "signature/$deliveryId"
    }
    object Profile : Screen("profile")
    object Suivi : Screen("suivi")
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
            val userState by authViewModel.user.collectAsState()
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    val user = authViewModel.user.value
                    if (user?.isDriver() == true) {
                        navController.navigate(Screen.Deliveries.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        composable(Screen.Register.route) {
            val userState by authViewModel.user.collectAsState()
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    val user = authViewModel.user.value
                    if (user?.isDriver() == true) {
                        navController.navigate(Screen.Deliveries.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Home.route) {
            token?.let { t ->
                HomeScreen(
                    token = t,
                    viewModel = deliveryViewModel,
                    onNavigateToDeliveries = {
                        navController.navigate(Screen.Deliveries.route)
                    },
                    onNavigateToDelivery = { id, status ->
                        if (status == "delivered" || status == "failed") {
                            navController.navigate(Screen.DeliveryDetails.createRoute(id))
                        } else {
                            navController.navigate(Screen.Tracking.createRoute(id))
                        }
                    }
                )
            }
        }
        composable(Screen.Deliveries.route) {
            val user by authViewModel.user.collectAsState()
            token?.let { t ->
                if (user?.isDriver() == true) {
                    DriverDeliveriesScreen(t, deliveryViewModel, onDeliveryClick = { id ->
                        navController.navigate(Screen.DriverMission.createRoute(id))
                    })
                } else {
                    ClientDeliveriesScreen(t, user, deliveryViewModel, onDeliveryClick = { delivery ->
                        if (delivery.status == "delivered" || delivery.status == "failed") {
                            navController.navigate(Screen.DeliveryDetails.createRoute(delivery.id))
                        } else {
                            navController.navigate(Screen.Tracking.createRoute(delivery.id))
                        }
                    })
                }
            }
        }
        composable(Screen.Tracking.route) { backStackEntry ->
            val deliveryId = backStackEntry.arguments?.getString("deliveryId")?.toIntOrNull()
            token?.let { t ->
                deliveryId?.let { id ->
                    TrackingScreen(t, id, deliveryViewModel, authViewModel, onBack = {
                        navController.popBackStack()
                    }, onNavigateToDriver = { driverId ->
                        navController.navigate(Screen.DriverProfile.createRoute(driverId))
                    })
                }
            }
        }
        composable(Screen.DeliveryDetails.route) { backStackEntry ->
            val deliveryId = backStackEntry.arguments?.getString("deliveryId")?.toIntOrNull()
            token?.let { t ->
                deliveryId?.let { id ->
                    DeliveryDetailsScreen(t, id, deliveryViewModel, onBack = {
                        navController.popBackStack()
                    }, onNavigateToDriver = { driverId ->
                        navController.navigate(Screen.DriverProfile.createRoute(driverId))
                    })
                }
            }
        }
        composable(Screen.DriverMission.route) { backStackEntry ->
            val deliveryId = backStackEntry.arguments?.getString("deliveryId")?.toIntOrNull()
            token?.let { t ->
                deliveryId?.let { id ->
                    DriverMissionScreen(t, id, deliveryViewModel, authViewModel, onBack = {
                        navController.popBackStack()
                    }, onSignature = {
                        navController.navigate(Screen.Signature.createRoute(id))
                    })
                }
            }
        }
        composable(Screen.Signature.route) { backStackEntry ->
            val deliveryId = backStackEntry.arguments?.getString("deliveryId")?.toIntOrNull()
            token?.let { t ->
                deliveryId?.let { id ->
                    SignatureScreen(
                        viewModel = deliveryViewModel,
                        onSignatureCaptured = { signatureBase64 ->
                            val photo = deliveryViewModel.proofPhoto.value
                            deliveryViewModel.updateStatus(t, id, "delivered", signatureBase64 = signatureBase64, photoFile = photo) {
                                deliveryViewModel.setProofPhoto(null) // Reset after success
                                navController.navigate(Screen.Deliveries.route) {
                                    popUpTo(Screen.Deliveries.route) { inclusive = true }
                                }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
        composable(Screen.DriverProfile.route) { backStackEntry ->
            val driverId = backStackEntry.arguments?.getString("driverId")?.toIntOrNull()
            token?.let { t ->
                driverId?.let { id ->
                    DriverProfileScreen(t, id, deliveryViewModel, onBack = {
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
        composable(Screen.Suivi.route) {
            val user by authViewModel.user.collectAsState()
            token?.let {
                SuiviScreen(it, user, deliveryViewModel, authViewModel)
            }
        }
    }
}
