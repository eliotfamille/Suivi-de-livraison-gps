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
    object ForgotPassword : Screen("forgot_password")
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
    val jeton by authViewModel.jeton.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            EcranConnexion(
                modeleDeVue = authViewModel,
                lorsSuccesConnexion = {
                    val user = authViewModel.utilisateur.value
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
                lorsNavigationVersInscription = {
                    navController.navigate(Screen.Register.route)
                },
                lorsMotDePasseOublie = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }
        composable(Screen.ForgotPassword.route) {
            EcranMotDePasseOublie(
                modeleDeVue = authViewModel,
                lorsRetour = { navController.popBackStack() }
            )
        }
        composable(Screen.Register.route) {
            EcranInscription(
                modeleDeVue = authViewModel,
                lorsSuccesInscription = {
                    val user = authViewModel.utilisateur.value
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
                lorsRetourConnexion = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Home.route) {
            jeton?.let { t ->
                EcranAccueil(
                    jeton = t,
                    modeleDeVue = deliveryViewModel,
                    lorsNavigationVersLivraisons = {
                        navController.navigate(Screen.Deliveries.route)
                    },
                    lorsNavigationVersLivraison = { id, status ->
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
            val user by authViewModel.utilisateur.collectAsState()
            jeton?.let { t ->
                if (user?.isDriver() == true) {
                    EcranLivraisonsLivreur(t, deliveryViewModel, lorsClicLivraison = { id ->
                        navController.navigate(Screen.DriverMission.createRoute(id))
                    })
                } else {
                    EcranLivraisonsClient(t, user, deliveryViewModel, lorsClicLivraison = { delivery ->
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
            jeton?.let { t ->
                deliveryId?.let { id ->
                    EcranSuivi(t, id, deliveryViewModel, authViewModel, lorsRetour = {
                        navController.popBackStack()
                    }, lorsNavigationVersLivreur = { driverId ->
                        navController.navigate(Screen.DriverProfile.createRoute(driverId))
                    })
                }
            }
        }
        composable(Screen.DeliveryDetails.route) { backStackEntry ->
            val deliveryId = backStackEntry.arguments?.getString("deliveryId")?.toIntOrNull()
            jeton?.let { t ->
                deliveryId?.let { id ->
                    EcranDetailsLivraison(t, id, deliveryViewModel, lorsRetour = {
                        navController.popBackStack()
                    }, lorsNavigationVersLivreur = { driverId ->
                        navController.navigate(Screen.DriverProfile.createRoute(driverId))
                    })
                }
            }
        }
        composable(Screen.DriverMission.route) { backStackEntry ->
            val deliveryId = backStackEntry.arguments?.getString("deliveryId")?.toIntOrNull()
            jeton?.let { t ->
                deliveryId?.let { id ->
                    EcranMissionLivreur(t, id, deliveryViewModel, authViewModel, lorsRetour = {
                        navController.popBackStack()
                    }, lorsSignature = {
                        navController.navigate(Screen.Signature.createRoute(id))
                    })
                }
            }
        }
        composable(Screen.Signature.route) { backStackEntry ->
            val deliveryId = backStackEntry.arguments?.getString("deliveryId")?.toIntOrNull()
            jeton?.let { t ->
                deliveryId?.let { id ->
                    EcranSignature(
                        modeleDeVue = deliveryViewModel,
                        lorsSignatureCapturee = { signatureBase64 ->
                            val photo = deliveryViewModel.photoPreuve.value
                            deliveryViewModel.mettreAJourStatut(t, id, "delivered", fichierPhoto = photo, signatureBase64 = signatureBase64) {
                                deliveryViewModel.definirPhotoPreuve(null)
                                navController.navigate(Screen.Deliveries.route) {
                                    popUpTo(Screen.Deliveries.route) { inclusive = true }
                                }
                            }
                        },
                        lorsRetour = { navController.popBackStack() }
                    )
                }
            }
        }
        composable(Screen.DriverProfile.route) { backStackEntry ->
            val driverId = backStackEntry.arguments?.getString("driverId")?.toIntOrNull()
            jeton?.let { t ->
                driverId?.let { id ->
                    EcranProfilLivreur(t, id, deliveryViewModel, lorsRetour = {
                        navController.popBackStack()
                    })
                }
            }
        }
        composable(Screen.Profile.route) {
            EcranProfil(authViewModel, lorsDeconnexion = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            })
        }
        composable(Screen.Suivi.route) {
            val user by authViewModel.utilisateur.collectAsState()
            jeton?.let {
                EcranSuiviGlobal(it, user, deliveryViewModel, authViewModel)
            }
        }
    }
}
