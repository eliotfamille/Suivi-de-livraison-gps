package com.nenykely.front_kotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nenykely.front_kotlin.navigation.NavGraph
import com.nenykely.front_kotlin.navigation.Screen
import com.nenykely.front_kotlin.ui.theme.FrontkotlinTheme
import com.nenykely.front_kotlin.viewmodel.AuthViewModel
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val deliveryViewModel: DeliveryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrontkotlinTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                Scaffold(
                    bottomBar = {
                        if (currentDestination?.route != Screen.Login.route && currentDestination?.route != Screen.Register.route) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text("Accueil") },
                                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Home.route } == true,
                                    onClick = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    label = { Text("Profil") },
                                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Profile.route } == true,
                                    onClick = {
                                        navController.navigate(Screen.Profile.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        NavGraph(
                            navController = navController,
                            authViewModel = authViewModel,
                            deliveryViewModel = deliveryViewModel
                        )
                    }
                }
            }
        }
    }
}
