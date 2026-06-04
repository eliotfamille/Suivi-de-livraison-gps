package com.nenykely.front_kotlin.ui.screens.client

import android.Manifest
import android.widget.Toast
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
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SuiviScreen(token: String, user: com.nenykely.front_kotlin.data.models.User?, viewModel: DeliveryViewModel) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val deliveries by viewModel.deliveries.collectAsState()
    val primaryBlue = Color(0xFF0052CC)
    
    var showPickupPoints by remember { mutableStateOf(true) }
    var selectedDeliveryForInfo by remember { mutableStateOf<Delivery?>(null) }
    
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var isFetchingRoute by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
        viewModel.fetchDeliveries(token)
    }

    LaunchedEffect(selectedDeliveryForInfo) {
        selectedDeliveryForInfo?.let { delivery ->
            val startLat = delivery.order?.sender_lat
            val startLng = delivery.order?.sender_lng
            val endLat = delivery.order?.recipient_lat
            val endLng = delivery.order?.recipient_lng

            if (startLat != null && startLng != null && endLat != null && endLng != null && startLat != 0.0) {
                isFetchingRoute = true
                try {
                    val url = "https://router.project-osrm.org/route/v1/driving/$startLng,$startLat;$endLng,$endLat?overview=full&geometries=geojson"
                    val response = withContext(Dispatchers.IO) {
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                        connection.connectTimeout = 8000
                        connection.readTimeout = 8000
                        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                            connection.inputStream.bufferedReader().readText()
                        } else null
                    }
                    
                    response?.let {
                        val json = JSONObject(it)
                        if (json.getString("code") == "Ok") {
                            val routes = json.getJSONArray("routes")
                            if (routes.length() > 0) {
                                val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                                val coords = geometry.getJSONArray("coordinates")
                                val points = mutableListOf<GeoPoint>()
                                for (i in 0 until coords.length()) {
                                    val coord = coords.getJSONArray(i)
                                    points.add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                                }
                                routePoints = points
                            }
                        }
                    }
                } catch (e: Exception) {
                    routePoints = listOf(GeoPoint(startLat, startLng), GeoPoint(endLat, endLng))
                } finally {
                    isFetchingRoute = false
                }
            }
        } ?: run { routePoints = emptyList() }
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", 0))
                    Configuration.getInstance().userAgentValue = ctx.packageName
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(12.0)
                        controller.setCenter(GeoPoint(-18.8792, 47.5079))
                        
                        if (locationPermissionState.status.isGranted) {
                            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                            locationOverlay.enableMyLocation()
                            overlays.add(locationOverlay)
                        }
                    }
                },
                update = { mapView ->
                    mapView.overlays.removeAll { it is Marker || it is Polyline }
                    
                    // Display all deliveries
                    deliveries.filter { it.status != "delivered" }.forEach { delivery ->
                        val lat = if (showPickupPoints) delivery.order?.sender_lat else delivery.order?.recipient_lat
                        val lng = if (showPickupPoints) delivery.order?.sender_lng else delivery.order?.recipient_lng
                        
                        if (lat != null && lng != null && lat != 0.0) {
                            val marker = Marker(mapView)
                            marker.position = GeoPoint(lat, lng)
                            marker.title = "Colis #${delivery.id}"
                            marker.setOnMarkerClickListener { _, _ ->
                                selectedDeliveryForInfo = delivery
                                true
                            }
                            mapView.overlays.add(marker)
                        }
                    }

                    // Display route if exists
                    if (routePoints.isNotEmpty()) {
                        val line = Polyline()
                        line.setPoints(routePoints)
                        line.outlinePaint.color = android.graphics.Color.BLUE
                        line.outlinePaint.strokeWidth = 10f
                        mapView.overlays.add(line)
                    }
                    
                    mapView.invalidate()
                }
            )
            
            if (isFetchingRoute) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp), color = primaryBlue)
            }
            
            selectedDeliveryForInfo?.let { delivery ->
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Détails Livraison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Client: ${delivery.order?.recipient_name ?: "N/A"}")
                        Text("Adresse: ${delivery.order?.recipient_address ?: "N/A"}")
                        
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
                                Text("Accepter la livraison")
                            }
                        }
                    }
                }
            }
        }
    }
}
