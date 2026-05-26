package com.nenykely.front_kotlin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nenykely.front_kotlin.data.models.Delivery
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveriesScreen(token: String, viewModel: DeliveryViewModel, onDeliveryClick: (Int) -> Unit) {
    val deliveries by viewModel.deliveries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.fetchDeliveries(token)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Logistics Pro", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black, 
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                actions = {
                    IconButton(onClick = { /* Profile */ }) {
                        Icon(Icons.Default.Person, contentDescription = "Profil", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Section
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Mes livraisons",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Segmented Control (Tabs)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        TabButton(
                            text = "En cours", 
                            isSelected = selectedTab == 0, 
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = 0 }
                        )
                        TabButton(
                            text = "Historique", 
                            isSelected = selectedTab == 1, 
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = 1 }
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val filteredDeliveries = if (selectedTab == 0) {
                        deliveries.filter { it.status != "delivered" && it.status != "failed" }
                    } else {
                        deliveries.filter { it.status == "delivered" || it.status == "failed" }
                    }

                    items(filteredDeliveries) { delivery ->
                        DeliveryCard(delivery) {
                            onDeliveryClick(delivery.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DeliveryCard(delivery: Delivery, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Status indicator accent bar
            val accentColor = when (delivery.status) {
                "in_transit", "picked_up", "assigned" -> MaterialTheme.colorScheme.primary
                "delivered" -> MaterialTheme.colorScheme.secondary
                "pending" -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(accentColor)
            )

            Column(modifier = Modifier.padding(16.dp).padding(start = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text(
                            "NUMÉRO DE SUIVI", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            delivery.order?.id?.let { "FR-$it-X" } ?: "N/A", 
                            style = MaterialTheme.typography.titleLarge, 
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Status Badge
                    Surface(
                        color = accentColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = when (delivery.status) {
                                "in_transit" -> Icons.Default.LocalShipping
                                "delivered" -> Icons.Default.CheckCircle
                                "pending" -> Icons.Default.HourglassTop
                                else -> Icons.Default.Error
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = accentColor)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                delivery.status.replace("_", " ").uppercase(), 
                                style = MaterialTheme.typography.labelMedium, 
                                fontWeight = FontWeight.Bold, 
                                color = accentColor
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        delivery.order?.recipient_address ?: "Adresse non spécifiée", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (delivery.status == "delivered") "Livré le ${delivery.delivered_at ?: "..."}" else "Aujourd'hui, estimé 14:30 - 16:00", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
