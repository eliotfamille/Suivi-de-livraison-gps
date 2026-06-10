package com.nenykely.front_kotlin.ui.screens.client

import android.Manifest
import android.widget.Toast
import android.util.Log
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.viewinterop.AndroidView
import com.nenykely.front_kotlin.data.models.Delivery
import com.nenykely.front_kotlin.viewmodel.AuthViewModel
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SuiviScreen(token: String, user: com.nenykely.front_kotlin.data.models.User?, viewModel: DeliveryViewModel, authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val deliveries by viewModel.deliveries.collectAsState()
    val isDarkMode by authViewModel.isDarkMode.collectAsState()
    val primaryBlue = MaterialTheme.colorScheme.primary
    
    var showPickupPoints by remember { mutableStateOf(true) }
    var selectedDeliveryForInfo by remember { mutableStateOf<Delivery?>(null) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
        while(true) {
            viewModel.fetchDeliveries(token)
            delay(5000)
        }
    }

    LaunchedEffect(selectedDeliveryForInfo) {
        if (selectedDeliveryForInfo == null) {
            routePoints = emptyList()
            return@LaunchedEffect
        }
        
        val delivery = selectedDeliveryForInfo!!
        val dLat = delivery.driver?.current_lat ?: 0.0
        val dLng = delivery.driver?.current_lng ?: 0.0
        
        // Ignorer le point fantôme du centre ville d'Antananarivo
        if (dLat == 0.0 || (dLat > -18.885 && dLat < -18.878 && dLng > 47.505 && dLng < 47.510)) {
            routePoints = emptyList()
            return@LaunchedEffect
        }

        val dPoint = "${dLng},${dLat}"
        val rPoint = "${delivery.order?.recipient_lng},${delivery.order?.recipient_lat}"
        val coordinates = if (delivery.status == "pending" || delivery.status == "assigned") {
            val sPoint = "${delivery.order?.sender_lng},${delivery.order?.sender_lat}"
            "$dPoint;$sPoint;$rPoint"
        } else {
            "$dPoint;$rPoint"
        }

        try {
            val url = "https://router.project-osrm.org/route/v1/driving/$coordinates?overview=full&geometries=geojson"
            val response = withContext(Dispatchers.IO) {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.bufferedReader().readText()
                } else null
            }
            
            response?.let {
                val json = JSONObject(it)
                if (json.getString("code") == "Ok") {
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                        val coords = geometry.getJSONArray("coordinates")
                        val newPoints = mutableListOf<GeoPoint>()
                        for (i in 0 until coords.length()) {
                            val coord = coords.getJSONArray(i)
                            newPoints.add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                        }
                        routePoints = newPoints
                    }
                }
            }
        } catch (e: Exception) { Log.e("OSRM", "Error", e) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Suivi des Livraisons", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = primaryBlue) },
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", 0))
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(12.0)
                        controller.setCenter(GeoPoint(-18.8792, 47.5079))
                    }
                },
                update = { mapView ->
                    if (isDarkMode) {
                        mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                    } else {
                        mapView.overlayManager.tilesOverlay.setColorFilter(null)
                    }

                    mapView.overlays.removeAll { it is Marker || it is Polyline }
                    
                    deliveries.filter { it.status != "delivered" }.forEach { delivery ->
                        val lat = if (showPickupPoints) delivery.order?.sender_lat else delivery.order?.recipient_lat
                        val lng = if (showPickupPoints) delivery.order?.sender_lng else delivery.order?.recipient_lng
                        
                        if (lat != null && lng != null && lat != 0.0) {
                            val marker = Marker(mapView)
                            marker.position = GeoPoint(lat, lng)
                            marker.title = "Colis #${delivery.id}"
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.setOnMarkerClickListener { _, _ ->
                                selectedDeliveryForInfo = delivery
                                true
                            }
                            mapView.overlays.add(marker)
                        }
                        
                        val dLat = delivery.driver?.current_lat ?: 0.0
                        val dLng = delivery.driver?.current_lng ?: 0.0
                        val isPhantom = dLat > -18.885 && dLat < -18.878 && dLng > 47.505 && dLng < 47.510
                        
                        if (dLat != 0.0 && !isPhantom) {
                            val dMarker = Marker(mapView)
                            dMarker.position = GeoPoint(dLat, dLng)
                            dMarker.title = "Livreur #${delivery.id}"
                            mapView.overlays.add(dMarker)
                        }
                    }

                    if (routePoints.isNotEmpty()) {
                        val line = Polyline()
                        line.setPoints(routePoints)
                        line.outlinePaint.color = android.graphics.Color.parseColor("#2563EB")
                        line.outlinePaint.strokeWidth = 8f
                        mapView.overlays.add(line)
                    }
                    mapView.invalidate()
                }
            )
            
            selectedDeliveryForInfo?.let { delivery ->
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Détails Livraison #${delivery.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Destinataire: ${delivery.order?.recipient_name ?: "N/A"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Adresse: ${delivery.order?.recipient_address ?: "N/A"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Statut: ${delivery.status}", color = primaryBlue, fontWeight = FontWeight.Bold)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (user?.isDriver() == true && (delivery.status == "pending" || delivery.status == "assigned")) {
                            Button(
                                onClick = {
                                    viewModel.acceptDelivery(token, delivery.id) {
                                        selectedDeliveryForInfo = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                            ) {
                                Text("Accepter cette mission")
                            }
                        }
                        
                        TextButton(
                            onClick = { selectedDeliveryForInfo = null },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Fermer")
                        }
                    }
                }
            }
        }
    }
}
