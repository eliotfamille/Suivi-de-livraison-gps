package com.nenykely.front_kotlin.ui.screens.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(token: String, viewModel: DeliveryViewModel, onNavigateToDeliveries: () -> Unit, onNavigateToTracking: (Int) -> Unit) {
    val deliveries by viewModel.deliveries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val primaryBlue = Color(0xFF0052CC)

    LaunchedEffect(Unit) {
        viewModel.fetchDeliveries(token)
    }

    val activeDelivery = deliveries.firstOrNull { it.status != "delivered" && it.status != "failed" }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Logistics Pro", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black, 
                        color = primaryBlue
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.fetchDeliveries(token) },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F9FE))
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    // Search Section
                    Text(
                        text = "Suivre un colis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1C1E)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it.replace("\n", "").replace("\r", "") },
                        placeholder = { Text("Entrez le numéro de suivi...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.DarkGray) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = { 
                                    // Extraction de l'ID du format FR-XXXX-X ou direct
                                    val id = if (searchQuery.contains("-")) {
                                        searchQuery.split("-").getOrNull(1)?.toIntOrNull()?.minus(8000)
                                    } else {
                                        searchQuery.replace(Regex("[^0-9]"), "").toIntOrNull()
                                    }
                                    if (id != null) onNavigateToTracking(id)
                                },
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(40.dp)
                                    .background(primaryBlue, RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFFD1D5DB),
                            unfocusedBorderColor = Color(0xFFD1D5DB)
                        )
                    )
                }

                item {
                    // Active Delivery Summary
                    Text(
                        text = "En cours de livraison",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4B5563)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (activeDelivery != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(20.dp),
                            onClick = { onNavigateToTracking(activeDelivery.id) }
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Surface(
                                        color = Color(0xFFD1FAE5),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF059669))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(activeDelivery.status, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF3F4F6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(32.dp))
                                    }
                                }
                                
                                Text(text = "Colis #FR-${activeDelivery.id}X", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                Text("LIVRAISON ESTIMÉE", style = MaterialTheme.typography.labelMedium, color = Color(0xFF6B7280), letterSpacing = 0.5.sp)
                                Text(activeDelivery.estimated_arrival ?: "Prochainement", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = primaryBlue)
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Box(modifier = Modifier.padding(40.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Aucune livraison en cours", color = Color.Gray)
                            }
                        }
                    }
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(Color(0xFFE0F2FE))
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.fillMaxSize().alpha(0.05f), tint = primaryBlue)
                                
                                Box(modifier = Modifier.align(Alignment.Center)) {
                                    Box(modifier = Modifier.align(Alignment.TopCenter).offset(y = (-20).dp)) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(60.dp))
                                    }
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = CircleShape,
                                        color = primaryBlue,
                                        shadowElevation = 4.dp,
                                        border = BorderStroke(2.dp, Color.White)
                                    ) {
                                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.padding(6.dp))
                                    }
                                }
                            }
                            
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("VOS LIVRAISONS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280), letterSpacing = 0.5.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.List, contentDescription = null, tint = primaryBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Historique et suivi complet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                OutlinedButton(
                                    onClick = onNavigateToDeliveries,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF374151))
                                ) {
                                    Text("Gérer mes colis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
