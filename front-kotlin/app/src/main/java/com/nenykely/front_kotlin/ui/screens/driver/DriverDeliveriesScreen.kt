package com.nenykely.front_kotlin.ui.screens.driver

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nenykely.front_kotlin.data.models.Delivery
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDeliveriesScreen(token: String, viewModel: DeliveryViewModel, onDeliveryClick: (Int) -> Unit) {
    val deliveries by viewModel.deliveries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchDeliveries(token)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Missions Disponibles", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FE)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(deliveries.filter { it.status == "pending" || it.status == "assigned" }) { delivery ->
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

@Composable
fun DriverDeliveryCard(delivery: Delivery, onAccept: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Colis #${delivery.id}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Surface(
                    color = if (delivery.status == "pending") Color(0xFFFEF3C7) else Color(0xFFDBEAFE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (delivery.status == "pending") "EN ATTENTE" else "ASSIGNÉ",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (delivery.status == "pending") Color(0xFF92400E) else Color(0xFF1E40AF)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(delivery.order?.recipient_address ?: "Adresse inconnue", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (delivery.status == "pending") {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0052CC))
                ) {
                    Text("Accepter la mission")
                }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Voir les détails")
                }
            }
        }
    }
}
