package com.nenykely.front_kotlin.ui.screens.client

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.nenykely.front_kotlin.data.models.Delivery
import com.nenykely.front_kotlin.data.models.User
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
fun ClientDeliveriesScreen(token: String, user: User?, viewModel: DeliveryViewModel, onDeliveryClick: (Delivery) -> Unit) {
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
            token = token,
            viewModel = viewModel,
            currentUser = user,
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
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Profile */ }) {
                        Surface(modifier = Modifier.size(32.dp), shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                            Icon(Icons.Default.Person, contentDescription = "Profil", modifier = Modifier.padding(4.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSubmitDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouvelle livraison")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Text(
                text = "Mes livraisons",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Tabs Styling
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
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

            if (isLoading && deliveries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { viewModel.fetchDeliveries(token) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (deliveries.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aucune livraison trouvée", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    onDeliveryClick(delivery)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onUserSelected: (User) -> Unit,
    viewModel: DeliveryViewModel,
    token: String
) {
    var expanded by remember { mutableStateOf(false) }
    val users by viewModel.users.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                val sanitized = it.replace("\n", "").replace("\r", "")
                onValueChange(sanitized)
                if (sanitized.length >= 2) {
                    viewModel.searchUsers(token, sanitized)
                    expanded = true
                } else {
                    expanded = false
                }
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = { Icon(Icons.Default.Search, null) }
        )
        if (expanded && users.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 8.dp,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
            ) {
                LazyColumn {
                    items(users) { user ->
                        ListItem(
                            headlineContent = { Text(user.name) },
                            supportingContent = { Text(user.email) },
                            modifier = Modifier.clickable {
                                onUserSelected(user)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
fun SubmitDeliveryDialog(
    token: String,
    viewModel: DeliveryViewModel,
    currentUser: User?,
    onDismiss: () -> Unit,
    onSubmit: (Map<String, Any?>) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var recipientAddress by remember { mutableStateOf("") }
    var senderName by remember { mutableStateOf(currentUser?.name ?: "") }
    var senderPhone by remember { mutableStateOf(currentUser?.phone ?: "") }
    var senderAddress by remember { mutableStateOf(currentUser?.domicile ?: "") }
    
    var senderGeoPoint by remember { mutableStateOf<GeoPoint?>(
        if (currentUser?.domicile_lat != null) GeoPoint(currentUser.domicile_lat, currentUser.domicile_lng!!) else null
    ) }
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
                            Configuration.getInstance().userAgentValue = ctx.packageName
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(12.0)
                                controller.setCenter(initialPos ?: GeoPoint(-18.8792, 47.5079))
                                
                                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                                locationOverlay.enableMyLocation()
                                overlays.add(locationOverlay)

                                val overlay = MapEventsOverlay(object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                        if (showMapFor == "sender") {
                                            senderGeoPoint = p
                                            senderAddress = "Position choisie"
                                        } else {
                                            recipientGeoPoint = p
                                            recipientAddress = "Position choisie"
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
                    UserSearchField(
                        label = "Nom ou Email",
                        value = senderName,
                        onValueChange = { senderName = it },
                        onUserSelected = { u ->
                            senderName = u.name
                            senderPhone = u.phone ?: ""
                            senderAddress = u.domicile ?: ""
                            if (u.domicile_lat != null) senderGeoPoint = GeoPoint(u.domicile_lat, u.domicile_lng!!)
                        },
                        viewModel = viewModel,
                        token = token
                    )
                    OutlinedTextField(
                        value = senderPhone, 
                        onValueChange = { senderPhone = it.replace("\n", "").replace("\r", "") }, 
                        label = { Text("Téléphone") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    
                    Text("Position de départ :", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                if (locationPermissionState.status.isGranted) {
                                    Toast.makeText(context, "Récupération...", Toast.LENGTH_SHORT).show()
                                    showMapFor = "sender"
                                } else locationPermissionState.launchPermissionRequest()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("GPS", fontSize = 12.sp)
                        }
                        FilledTonalButton(
                            onClick = {
                                currentUser?.domicile_lat?.let { lat ->
                                    senderGeoPoint = GeoPoint(lat, currentUser.domicile_lng!!)
                                    senderAddress = currentUser.domicile ?: "Domicile"
                                    Toast.makeText(context, "Domicile sélectionné", Toast.LENGTH_SHORT).show()
                                } ?: Toast.makeText(context, "Aucun domicile enregistré", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Domicile", fontSize = 12.sp)
                        }
                        FilledTonalButton(
                            onClick = { showMapFor = "sender" },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Carte", fontSize = 12.sp)
                        }
                    }
                    if (senderGeoPoint != null) {
                        Text("Coordonnées : ${String.format("%.4f", senderGeoPoint!!.latitude)}, ${String.format("%.4f", senderGeoPoint!!.longitude)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Destinataire", fontWeight = FontWeight.Bold)
                    UserSearchField(
                        label = "Nom ou Email",
                        value = recipientName,
                        onValueChange = { recipientName = it },
                        onUserSelected = { u ->
                            recipientName = u.name
                            recipientPhone = u.phone ?: ""
                            recipientAddress = u.domicile ?: ""
                            if (u.domicile_lat != null) recipientGeoPoint = GeoPoint(u.domicile_lat, u.domicile_lng!!)
                        },
                        viewModel = viewModel,
                        token = token
                    )
                    OutlinedTextField(
                        value = recipientPhone, 
                        onValueChange = { recipientPhone = it.replace("\n", "").replace("\r", "") }, 
                        label = { Text("Téléphone") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    
                    Text("Position de destination :", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { showMapFor = "recipient" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Map, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Carte")
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Détails du colis", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = description, 
                        onValueChange = { description = it.replace("\n", "").replace("\r", "") }, 
                        label = { Text("Description") }, 
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it.replace("\n", "").replace("\r", "") },
                        label = { Text("Poids (kg)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
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
fun TabButton(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DeliveryCard(delivery: Delivery, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            val (accentColor, statusLabel, statusIcon) = when (delivery.status) {
                "in_transit", "picked_up", "assigned" -> Triple(MaterialTheme.colorScheme.primary, "En route", Icons.Default.LocalShipping)
                "pending" -> Triple(Color(0xFFD97706), "En attente", Icons.Default.HourglassEmpty)
                "delivered" -> Triple(Color(0xFF059669), "Livré", Icons.Default.CheckCircle)
                else -> Triple(MaterialTheme.colorScheme.error, "Retardé", Icons.Default.ErrorOutline)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            "FR-${8000+delivery.id}-X", 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        delivery.order?.recipient_address ?: "Adresse non renseignée", 
                        style = MaterialTheme.typography.bodyLarge, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Dernière mise à jour : Récemment",
                        style = MaterialTheme.typography.bodyLarge, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (delivery.driver != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Livreur : ${delivery.driver?.user?.name ?: "Assigné"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
