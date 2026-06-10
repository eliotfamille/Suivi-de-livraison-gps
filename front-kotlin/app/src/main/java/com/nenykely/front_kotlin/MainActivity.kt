package com.nenykely.front_kotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nenykely.front_kotlin.navigation.NavGraph
import com.nenykely.front_kotlin.navigation.*
import com.nenykely.front_kotlin.ui.screens.common.*
import com.nenykely.front_kotlin.ui.theme.FrontkotlinTheme
import com.nenykely.front_kotlin.viewmodel.AuthViewModel
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val deliveryViewModel: DeliveryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialisation globale d'osmdroid
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", 0))
        Configuration.getInstance().userAgentValue = packageName

        enableEdgeToEdge()
        setContent {
            val isDarkMode by authViewModel.isDarkMode.collectAsState()
            
            FrontkotlinTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val user by authViewModel.user.collectAsState()
                val isDriver = user?.isDriver() == true

                Scaffold(
                    bottomBar = {
                        if (currentDestination?.route != Screen.Login.route && currentDestination?.route != Screen.Register.route) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!isDriver) {
                                        NavigationItem(
                                            label = "Accueil",
                                            icon = Icons.Default.Home,
                                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Home.route } == true,
                                            onClick = {
                                                navController.navigate(Screen.Home.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                                    NavigationItem(
                                        label = "Livraisons",
                                        icon = Icons.Default.Inventory2,
                                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Deliveries.route } == true,
                                        onClick = {
                                            navController.navigate(Screen.Deliveries.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                    if (isDriver) {
                                        NavigationItem(
                                            label = "Carte",
                                            icon = Icons.Default.Map,
                                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Suivi.route } == true,
                                            onClick = {
                                                navController.navigate(Screen.Suivi.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                                    if (!isDriver) {
                                        NavigationItem(
                                            label = "Suivi",
                                            icon = Icons.Default.Map,
                                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Suivi.route } == true,
                                            onClick = {
                                                navController.navigate(Screen.Suivi.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                                    NavigationItem(
                                        label = "Profil",
                                        icon = Icons.Default.Person,
                                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Profile.route } == true,
                                        onClick = {
                                            navController.navigate(Screen.Profile.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding), color = MaterialTheme.colorScheme.background) {
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

@Composable
fun NavigationItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val mintColor = Color(0xFF69F0AE)
    val contentColor = if (selected) Color(0xFF1A5632) else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) mintColor else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
