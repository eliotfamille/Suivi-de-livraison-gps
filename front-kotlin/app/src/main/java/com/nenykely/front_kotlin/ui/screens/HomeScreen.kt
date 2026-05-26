package com.nenykely.front_kotlin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToDeliveries: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }

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
                navigationIcon = {
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Search Section
                Text(
                    text = "Suivre un colis",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Entrez le numéro de suivi...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        IconButton(
                            onClick = { /* Search action */ },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            item {
                // Active Delivery Summary
                Text(
                    text = "En cours de livraison",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Ambient pulse background element
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 40.dp, y = (-40).dp)
                                .size(160.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f), CircleShape)
                        )
                        
                        Column(modifier = Modifier.padding(16.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("En transit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Colis #FR-849201X", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                                // Package placeholder image
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.align(Alignment.Center), tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text("LIVRAISON ESTIMÉE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            Text("Aujourd'hui, 14:30", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                            Text("Signature requise à la réception.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column {
                        // Map Preview Placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            // Simple map illustration
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.fillMaxSize().alpha(0.1f), tint = MaterialTheme.colorScheme.primary)
                            
                            Box(modifier = Modifier.align(Alignment.Center)) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    shadowElevation = 4.dp,
                                    border = BorderStroke(2.dp, Color.White)
                                ) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                        
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("DERNIÈRE POSITION", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Centre de tri IDF, 75013", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = onNavigateToDeliveries,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Text("Voir sur la carte", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
            
            item {
                Text(
                    text = "Historique de livraison",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TimelineItem(title = "Pris en charge", time = "08:15", description = "Le colis a quitté l'entrepôt local.", isActive = true, isLast = false)
                        TimelineItem(title = "En transit", time = "Hier, 22:40", description = "Arrivée au centre de tri régional IDF.", isActive = true, isLast = false)
                        TimelineItem(title = "Expédié", time = "12 Mai, 09:00", description = "L'expéditeur a remis le colis au transporteur.", isActive = false, isLast = true)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun TimelineItem(title: String, time: String, description: String, isActive: Boolean, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(2.dp, if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isActive) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(MaterialTheme.colorScheme.outlineVariant, Color.Transparent)
                            )
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier
                .weight(1f)
                .background(if (isActive) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent, RoundedCornerShape(12.dp))
                .padding(if (isActive) 12.dp else 0.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Spacer(modifier = Modifier.height(if (isActive) 16.dp else 8.dp))
}
