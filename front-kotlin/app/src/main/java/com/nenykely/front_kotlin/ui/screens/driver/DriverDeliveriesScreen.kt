package com.nenykely.front_kotlin.ui.screens.driver

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nenykely.front_kotlin.data.models.Delivery
import com.nenykely.front_kotlin.ui.screens.client.TabButton
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDeliveriesScreen(token: String, viewModel: DeliveryViewModel, onDeliveryClick: (Int) -> Unit) {
    val deliveries by viewModel.deliveries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.fetchDeliveries(token)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mes Missions", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tabs for Driver
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    TabButton(
                        text = "Disponibles", 
                        isSelected = selectedTab == 0, 
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    TabButton(
                        text = "En cours", 
                        isSelected = selectedTab == 1, 
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                }
            }

            val filteredDeliveries = if (selectedTab == 0) {
                deliveries.filter { it.status == "pending" || it.status == "assigned" }
            } else {
                deliveries.filter { it.status == "picked_up" || it.status == "in_transit" }
            }

            if (isLoading && filteredDeliveries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { viewModel.fetchDeliveries(token) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (filteredDeliveries.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            val emptyMessage = if (selectedTab == 0) "Aucune mission disponible" else "Aucune mission en cours"
                            Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredDeliveries) { delivery ->
                                DriverDeliveryCard(
                                    delivery = delivery,
                                    onAccept = {
                                        viewModel.acceptDelivery(token, delivery.id) {
                                            Toast.makeText(context, "Mission #${delivery.id} acceptée ! Nouveau statut: ASSIGNÉ", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    onClick = { onDeliveryClick(delivery.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DriverDeliveryCard(delivery: Delivery, onAccept: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Colis #${delivery.id}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                val statusInfo = when (delivery.status) {
                    "pending" -> Color(0xFFFEF3C7) to "EN ATTENTE"
                    "assigned" -> MaterialTheme.colorScheme.primaryContainer to "ASSIGNÉ"
                    "picked_up" -> Color(0xFFDBEAFE) to "RÉCUPÉRÉ"
                    "in_transit" -> Color(0xFFD1FAE5) to "EN TRANSIT"
                    else -> MaterialTheme.colorScheme.surfaceVariant to delivery.status.uppercase()
                }
                
                val textColor = when (delivery.status) {
                    "pending" -> Color(0xFF92400E)
                    "assigned" -> MaterialTheme.colorScheme.onPrimaryContainer
                    "picked_up" -> Color(0xFF1E40AF)
                    "in_transit" -> Color(0xFF065F46)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    color = statusInfo.first,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusInfo.second,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(delivery.order?.recipient_address ?: "Adresse inconnue", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (delivery.status == "pending") {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Accepter la mission", color = MaterialTheme.colorScheme.onPrimary)
                }
            } else {
                val buttonText = when(delivery.status) {
                    "assigned" -> "Démarrer la mission"
                    "picked_up", "in_transit" -> "Continuer la mission"
                    else -> "Voir les détails"
                }
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}
