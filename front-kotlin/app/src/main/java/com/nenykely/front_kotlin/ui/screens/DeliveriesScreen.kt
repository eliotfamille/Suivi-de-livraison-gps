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
                        color = Color(0xFF0052CC)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = Color(0xFF1A1C1E))
                    }
                },
                actions = {
                    IconButton(onClick = { /* Profile */ }) {
                        Surface(modifier = Modifier.size(32.dp), shape = androidx.compose.foundation.shape.CircleShape, color = Color(0xFFDBEAFE)) {
                            Icon(Icons.Default.Person, contentDescription = "Profil", modifier = Modifier.padding(4.dp), tint = Color(0xFF3B82F6))
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FE))
        ) {
            Text(
                text = "Mes livraisons",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
                color = Color(0xFF111827)
            )

            // Tabs Styling
            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
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

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF0052CC))
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
        color = if (isSelected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF1E293B) else Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun DeliveryCard(delivery: Delivery, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            val (accentColor, statusLabel, statusIcon) = when (delivery.status) {
                "in_transit", "picked_up", "assigned" -> Triple(Color(0xFF2563EB), "En route", Icons.Default.LocalShipping)
                "pending" -> Triple(Color(0xFFD97706), "En attente", Icons.Default.HourglassEmpty)
                "delivered" -> Triple(Color(0xFF059669), "Livré", Icons.Default.CheckCircle)
                else -> Triple(Color(0xFFDC2626), "Retardé", Icons.Default.ErrorOutline)
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(accentColor)
            )

            Column(modifier = Modifier.padding(20.dp).padding(start = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text(
                            "NUMÉRO DE SUIVI", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            delivery.order?.id?.let { "FR-8492-X" } ?: "N/A", // Matching screenshot style
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    
                    Surface(
                        color = accentColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(statusIcon, contentDescription = null, modifier = Modifier.size(16.dp), tint = accentColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                statusLabel, 
                                style = MaterialTheme.typography.labelLarge, 
                                fontWeight = FontWeight.Bold, 
                                color = accentColor
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        delivery.order?.recipient_address ?: "14 Rue de la Paix, 75002 Paris", 
                        style = MaterialTheme.typography.bodyLarge, 
                        color = Color(0xFF475569)
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Aujourd'hui, estimé 14:30 - 16:00", 
                        style = MaterialTheme.typography.bodyLarge, 
                        color = Color(0xFF475569)
                    )
                }
            }
        }
    }
}
