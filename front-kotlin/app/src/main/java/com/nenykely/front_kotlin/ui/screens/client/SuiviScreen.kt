package com.nenykely.front_kotlin.ui.screens.client

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.nenykely.front_kotlin.data.models.Delivery
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SuiviScreen(token: String, viewModel: DeliveryViewModel) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val deliveries by viewModel.deliveries.collectAsState()
    val primaryBlue = Color(0xFF0052CC)
    
    var showPickupPoints by remember { mutableStateOf(true) }
    var selectedDeliveryForInfo by remember { mutableStateOf<Delivery?>(null) }

    // Initialisation OSMDroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
        viewModel.fetchDeliveries(token)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Logistics Pro (OSM)", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black, 
                        color = primaryBlue
                    ) 
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (showPickupPoints) "Départ" else "Dest.", style = MaterialTheme.typography.labelSmall)
                        Switch(
                            checked = !showPickupPoints,
                            onCheckedChange = { showPickupPoints = !it },
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(12.0)
                        controller.setCenter(GeoPoint(-18.8792, 47.5079)) // Tana par défaut
                        
                        // Ajout de la position actuelle
                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        locationOverlay.enableMyLocation()
                        locationOverlay.enableFollowLocation() // Optionnel : suit l'utilisateur
                        overlays.add(locationOverlay)
                    }
                },
                update = { mapView ->
                    // On ne touche pas à l'overlay de position, on nettoie les autres
                    mapView.overlays.removeAll { it is Marker || it is Polyline }
                    
                    deliveries.filter { it.status != "delivered" }.forEach { delivery ->
                        val lat = if (showPickupPoints) delivery.order?.sender_lat else delivery.order?.recipient_lat
                        val lng = if (showPickupPoints) delivery.order?.sender_lng else delivery.order?.recipient_lng
                        
                        if (lat != null && lng != null && lat != 0.0) {
                            val marker = Marker(mapView)
                            marker.position = GeoPoint(lat, lng)
                            marker.title = "Colis #${delivery.order?.id ?: delivery.id}"
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.setOnMarkerClickListener { m, _ ->
                                selectedDeliveryForInfo = delivery
                                m.showInfoWindow()
                                true
                            }
                            mapView.overlays.add(marker)
                        }
                    }

                    selectedDeliveryForInfo?.let { delivery ->
                        val startLat = delivery.order?.sender_lat
                        val startLng = delivery.order?.sender_lng
                        val endLat = delivery.order?.recipient_lat
                        val endLng = delivery.order?.recipient_lng

                        if (startLat != null && startLng != null && endLat != null && endLng != null) {
                            val line = Polyline()
                            line.setPoints(listOf(GeoPoint(startLat, startLng), GeoPoint(endLat, endLng)))
                            line.outlinePaint.color = android.graphics.Color.BLUE
                            line.outlinePaint.strokeWidth = 5f
                            mapView.overlays.add(line)
                        }
                    }
                    
                    mapView.invalidate()
                }
            )
            
            selectedDeliveryForInfo?.let { delivery ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Détails (OSM)", fontWeight = FontWeight.Bold)
                        Text("Client: ${delivery.order?.recipient_name ?: "N/A"}")
                        delivery.order?.`package`?.let { pkg ->
                            Text("Colis: ${pkg.description}")
                            Text("Poids: ${pkg.weight_kg} kg")
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (delivery.status == "pending" || delivery.status == "assigned") {
                            Button(
                                onClick = {
                                    viewModel.acceptDelivery(token, delivery.id) {
                                        selectedDeliveryForInfo = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Accepter la livraison")
                            }
                        }
                    }
                }
            }
        }
    }
}
