package com.nenykely.front_kotlin.ui.screens.client

import android.widget.Toast
import android.Manifest
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import org.json.JSONObject
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
    
    // State for routing
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var isFetchingRoute by remember { mutableStateOf(false) }

    // Initialisation OSMDroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
        viewModel.fetchDeliveries(token)
    }

    // Effect to fetch route when a delivery is selected
    LaunchedEffect(selectedDeliveryForInfo) {
        selectedDeliveryForInfo?.let { delivery ->
            val startLat = delivery.order?.sender_lat
            val startLng = delivery.order?.sender_lng
            val endLat = delivery.order?.recipient_lat
            val endLng = delivery.order?.recipient_lng

            if (startLat != null && startLng != null && endLat != null && endLng != null && startLat != 0.0) {
                isFetchingRoute = true
                try {
                    // OSRM expects [longitude,latitude]
                    val url = "https://router.project-osrm.org/route/v1/driving/$startLng,$startLat;$endLng,$endLat?overview=full&geometries=geojson"
                    Log.d("Routing", "Fetching route: $url")
                    
                    val response = withContext(Dispatchers.IO) {
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.connectTimeout = 8000
                        connection.readTimeout = 8000
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                        
                        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                            val text = connection.inputStream.bufferedReader().readText()
                            Log.d("Routing", "Response received: ${text.take(100)}...")
                            text
                        } else {
                            val errorText = connection.errorStream?.bufferedReader()?.readText() ?: "No error stream"
                            Log.e("Routing", "HTTP Error ${connection.responseCode}: $errorText")
                            null
                        }
                    }
                    
                    if (response != null) {
                        val json = JSONObject(response)
                        if (json.getString("code") == "Ok") {
                            val routes = json.getJSONArray("routes")
                            if (routes.length() > 0) {
                                val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                                val coords = geometry.getJSONArray("coordinates")
                                val points = mutableListOf<GeoPoint>()
                                for (i in 0 until coords.length()) {
                                    val coord = coords.getJSONArray(i)
                                    // GeoJSON is [lng, lat]
                                    points.add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                                }
                                routePoints = points
                                Log.d("Routing", "Successfully parsed ${points.size} points")
                            }
                        } else {
                            val code = json.getString("code")
                            Log.e("Routing", "OSRM Error: $code")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Erreur itinéraire: $code", Toast.LENGTH_SHORT).show()
                            }
                            routePoints = listOf(GeoPoint(startLat, startLng), GeoPoint(endLat, endLng))
                        }
                    } else {
                        routePoints = listOf(GeoPoint(startLat, startLng), GeoPoint(endLat, endLng))
                    }
                } catch (e: Exception) {
                    Log.e("Routing", "Error fetching route", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Erreur réseau itinéraire", Toast.LENGTH_SHORT).show()
                    }
                    // Fallback to straight line
                    routePoints = listOf(GeoPoint(startLat, startLng), GeoPoint(endLat, endLng))
                } finally {
                    isFetchingRoute = false
                }
            } else {
                routePoints = emptyList()
            }
        } ?: run {
            routePoints = emptyList()
        }
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
                        if (routePoints.isNotEmpty()) {
                            val line = Polyline()
                            line.setPoints(routePoints)
                            line.outlinePaint.color = android.graphics.Color.BLUE
                            line.outlinePaint.strokeWidth = 10f
                            mapView.overlays.add(line)
                        } else {
                            // Fallback to straight line if no route fetched yet
                            val startLat = delivery.order?.sender_lat
                            val startLng = delivery.order?.sender_lng
                            val endLat = delivery.order?.recipient_lat
                            val endLng = delivery.order?.recipient_lng

                            if (startLat != null && startLng != null && endLat != null && endLng != null) {
                                val line = Polyline()
                                line.setPoints(listOf(GeoPoint(startLat, startLng), GeoPoint(endLat, endLng)))
                                line.outlinePaint.color = android.graphics.Color.LTGRAY
                                line.outlinePaint.strokeWidth = 5f
                                mapView.overlays.add(line)
                            }
                        }
                    }
                    
                    mapView.invalidate()
                }
            )
            
            if (isFetchingRoute) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                    color = primaryBlue
                )
            }

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
                        
                        // Restriction : Seuls les livreurs peuvent accepter
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
                                Text("Accepter la livraison (Livreur)")
                            }
                        } else if (user?.isDriver() == false) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Consultation client uniquement",
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
