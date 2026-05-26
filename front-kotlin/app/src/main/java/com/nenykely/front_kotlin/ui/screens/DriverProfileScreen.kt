package com.nenykely.front_kotlin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverProfileScreen(driverId: Int, viewModel: DeliveryViewModel, onBack: () -> Unit) {
    val delivery by viewModel.currentDelivery.collectAsState()
    val driver = delivery?.driver
    val primaryBlue = Color(0xFF1E293B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Livreur", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color(0xFF0F172A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FE))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Driver Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = Color(0xFFE2E8F0)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(16.dp), tint = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(driver?.user?.name ?: "Alexandre Dubois", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF64748B))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Livreur Senior", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFFD1FAE5),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("En service", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.Inventory2, iconColor = Color(0xFF2563EB), value = "2,845", label = "Livraisons")
                StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.Star, iconColor = Color(0xFFF59E0B), value = "4.9", label = "Note moyenne")
                StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.WorkHistory, iconColor = Color(0xFF6366F1), value = "4 ans", label = "Expérience")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Vehicle Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = Color(0xFFEFF6FF)) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.padding(8.dp), tint = Color(0xFF3B82F6))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Détails du véhicule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    VehicleDetailRow("Modèle affecté", driver?.vehicle_model ?: "Peugeot e-Expert")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                    VehicleDetailRow("Type", driver?.vehicle_type ?: "Fourgon 100% Électrique")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Immatriculation", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF64748B))
                        Surface(
                            color = Color(0xFFE0E7FF),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                driver?.vehicle_plate ?: "AB-123-CD", 
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1B4B),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, icon: ImageVector, iconColor: Color, value: String, label: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = iconColor.copy(alpha = 0.1f)) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp), tint = iconColor)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun VehicleDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF64748B))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
    }
}
