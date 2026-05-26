package com.nenykely.front_kotlin.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(token: String, deliveryId: Int, viewModel: DeliveryViewModel, onBack: () -> Unit, onNavigateToDriver: (Int) -> Unit) {
    val delivery by viewModel.currentDelivery.collectAsState()

    LaunchedEffect(deliveryId) {
        viewModel.fetchTracking(token, deliveryId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map Area
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            // Simulated Map
            Icon(
                Icons.Default.Map, 
                contentDescription = null, 
                modifier = Modifier.fillMaxSize().alpha(0.1f),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // Destination Marker
            Box(modifier = Modifier.align(Alignment.Center).offset(y = (-40).dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 2.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            "Destination", 
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape).border(2.dp, Color.White, CircleShape))
                }
            }

            // Driver Marker (Moving)
            Box(modifier = Modifier.align(Alignment.Center).offset(x = (-60).dp, y = 20.dp)) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                ) {
                    Icon(
                        Icons.Default.LocalShipping, 
                        contentDescription = null, 
                        tint = Color.White, 
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Top Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                onClick = onBack
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, 
                    contentDescription = "Retour", 
                    modifier = Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mise à jour en direct", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Map Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 380.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapControlButton(Icons.Default.MyLocation)
            Column(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface).width(44.dp)
            ) {
                IconButton(onClick = {}) { Icon(Icons.Default.Add, null) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                IconButton(onClick = {}) { Icon(Icons.Default.Remove, null) }
            }
        }

        // Bottom Sheet
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(360.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 48.dp, height = 6.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .align(Alignment.CenterHorizontally)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Arrivée estimée", style = MaterialTheme.typography.titleLarge)
                        Text("14:30 - 14:45", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("EN ROUTE", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Driver Info
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        delivery?.driver?.id?.let { onNavigateToDriver(it) }
                    }
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(delivery?.driver?.user?.name ?: "Thomas Dubois", style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Livreur", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("•", color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp), tint = Color(0xFFF59E0B))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("4.9", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary, onClick = {}) {
                            Icon(Icons.Default.Call, null, modifier = Modifier.padding(10.dp), tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timeline
                Column {
                    TimelineRow(time = "10:15", text = "Colis pris en charge", isCompleted = true)
                    TimelineRow(time = "Actuellement", text = "Prochaine étape : Votre adresse", isCompleted = false, subtitle = delivery?.order?.recipient_address ?: "123 Avenue des Champs-Élysées, Paris")
                }
            }
        }
    }
}

@Composable
fun MapControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Icon(icon, null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun TimelineRow(time: String, text: String, isCompleted: Boolean, subtitle: String? = null) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            Box(
                modifier = Modifier
                    .size(if (isCompleted) 20.dp else 24.dp)
                    .background(if (isCompleted) MaterialTheme.colorScheme.secondary else Color.Transparent, CircleShape)
                    .border(2.dp, if (isCompleted) Color.Transparent else MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = Color.White)
                else Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            }
            if (subtitle == null) {
                Box(modifier = Modifier.weight(1f).width(2.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(time, style = MaterialTheme.typography.labelSmall, color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary, fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.Bold)
            Text(text, style = if (isCompleted) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.titleMedium, color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
