package com.nenykely.front_kotlin.ui.screens.client

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.nenykely.front_kotlin.data.models.Delivery
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ClientDeliveriesScreen(token: String, user: com.nenykely.front_kotlin.data.models.User?, viewModel: DeliveryViewModel, onDeliveryClick: (Int) -> Unit) {
    val deliveries by viewModel.deliveries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showSubmitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchDeliveries(token)
    }
    
    if (showSubmitDialog) {
        val context = LocalContext.current
        SubmitDeliveryDialog(
            onDismiss = { showSubmitDialog = false },
            onSubmit = { data ->
                viewModel.submitDelivery(token, data) { result ->
                    Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                }
                showSubmitDialog = false
            }
        )
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSubmitDialog = true },
                containerColor = Color(0xFF0052CC),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouvelle livraison")
            }
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
                if (deliveries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucune livraison trouvée", color = Color.Gray)
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
fun SubmitDeliveryDialog(onDismiss: () -> Unit, onSubmit: (Map<String, Any?>) -> Unit) {
    var description by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var recipientAddress by remember { mutableStateOf("") }
    var senderName by remember { mutableStateOf("") }
    var senderPhone by remember { mutableStateOf("") }
    var senderAddress by remember { mutableStateOf("") }
    
    var senderGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var recipientGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    
    var showMapFor by remember { mutableStateOf<String?>(null) } // "sender" or "recipient"

    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    if (showMapFor != null) {
        val title = if (showMapFor == "sender") "Position de départ" else "Position de destination"
        val initialPos = if (showMapFor == "sender") senderGeoPoint else recipientGeoPoint
        
        AlertDialog(
            onDismissRequest = { showMapFor = null },
            title = { Text(title) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", 0))
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(12.0)
                                controller.setCenter(initialPos ?: GeoPoint(-18.8792, 47.5079))
                                
                                // Position actuelle (Option A)
                                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                                locationOverlay.enableMyLocation()
                                overlays.add(locationOverlay)

                                // Gestion du clic sur la carte (Option B)
                                val overlay = MapEventsOverlay(object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                        if (showMapFor == "sender") {
                                            senderGeoPoint = p
                                            senderAddress = "Coordonnées : ${p.latitude}, ${p.longitude}"
                                        } else {
                                            recipientGeoPoint = p
                                            recipientAddress = "Coordonnées : ${p.latitude}, ${p.longitude}"
                                        }
                                        invalidate()
                                        return true
                                    }
                                    override fun longPressHelper(p: GeoPoint): Boolean = false
                                })
                                overlays.add(overlay)
                            }
                        },
                        update = { mapView ->
                            mapView.overlays.removeAll { it is Marker }
                            val markerPos = if (showMapFor == "sender") senderGeoPoint else recipientGeoPoint
                            markerPos?.let {
                                val marker = Marker(mapView)
                                marker.position = it
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                mapView.overlays.add(marker)
                            }
                            mapView.invalidate()
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showMapFor = null }) { Text("Valider") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle livraison", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Expéditeur", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = senderName, onValueChange = { senderName = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = senderPhone, onValueChange = { senderPhone = it }, label = { Text("Téléphone") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(
                        value = senderAddress,
                        onValueChange = { senderAddress = it },
                        label = { Text("Adresse de départ (cliquez icône map)") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            Row {
                                IconButton(onClick = {
                                    if (locationPermissionState.status.isGranted) {
                                        // Option A : GPS position actuelle
                                        Toast.makeText(context, "Récupération position...", Toast.LENGTH_SHORT).show()
                                        showMapFor = "sender" // Ouvre la map pour confirmer
                                    } else {
                                        locationPermissionState.launchPermissionRequest()
                                    }
                                }) {
                                    Icon(Icons.Default.MyLocation, contentDescription = "Ma position")
                                }
                                IconButton(onClick = { showMapFor = "sender" }) {
                                    Icon(Icons.Default.Map, contentDescription = "Choisir sur la carte")
                                }
                            }
                        }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Destinataire", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = recipientName, onValueChange = { recipientName = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = recipientPhone, onValueChange = { recipientPhone = it }, label = { Text("Téléphone") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(
                        value = recipientAddress,
                        onValueChange = { recipientAddress = it },
                        label = { Text("Adresse de destination (cliquez icône map)") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showMapFor = "recipient" }) {
                                Icon(Icons.Default.Map, contentDescription = "Choisir sur la carte")
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Détails du colis", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Poids (kg)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val data = mapOf(
                        "description" to description,
                        "weight_kg" to (weight.toDoubleOrNull() ?: 0.0),
                        "recipient_name" to recipientName,
                        "recipient_phone" to recipientPhone,
                        "recipient_address" to recipientAddress,
                        "recipient_lat" to (recipientGeoPoint?.latitude ?: 0.0),
                        "recipient_lng" to (recipientGeoPoint?.longitude ?: 0.0),
                        "sender_name" to senderName,
                        "sender_phone" to senderPhone,
                        "sender_address" to senderAddress,
                        "sender_lat" to (senderGeoPoint?.latitude ?: 0.0),
                        "sender_lng" to (senderGeoPoint?.longitude ?: 0.0)
                    )
                    onSubmit(data)
                },
                enabled = senderGeoPoint != null && recipientGeoPoint != null && description.isNotBlank()
            ) {
                Text("Soumettre")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
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
                            "FR-${8000+delivery.id}-X", 
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
                        delivery.order?.recipient_address ?: "Adresse non renseignée", 
                        style = MaterialTheme.typography.bodyLarge, 
                        color = Color(0xFF475569)
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Dernière mise à jour : Récemment",
                        style = MaterialTheme.typography.bodyLarge, 
                        color = Color(0xFF475569)
                    )
                }
            }
        }
    }
}
