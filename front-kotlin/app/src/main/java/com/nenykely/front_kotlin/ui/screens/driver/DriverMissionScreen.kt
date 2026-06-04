package com.nenykely.front_kotlin.ui.screens.driver

import android.widget.Toast
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverMissionScreen(token: String, deliveryId: Int, viewModel: DeliveryViewModel, onBack: () -> Unit, onSignature: () -> Unit) {
    val context = LocalContext.current
    val delivery by viewModel.currentDelivery.collectAsState()
    val primaryBlue = Color(0xFF2563EB)
    
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var isFetchingRoute by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            Toast.makeText(context, "Photo de preuve enregistrée !", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(deliveryId) {
        viewModel.fetchTracking(token, deliveryId)
    }

    val destLat = delivery?.order?.recipient_lat ?: -18.8792
    val destLng = delivery?.order?.recipient_lng ?: 47.5079
    val destPoint = GeoPoint(destLat, destLng)

    LaunchedEffect(currentLocation, destPoint) {
        currentLocation?.let { start ->
            isFetchingRoute = true
            try {
                val url = "https://router.project-osrm.org/route/v1/driving/${start.longitude},${start.latitude};${destPoint.longitude},${destPoint.latitude}?overview=full&geometries=geojson"
                val response = withContext(Dispatchers.IO) {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
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
                Log.e("Routing", "Error", e)
            } finally {
                isFetchingRoute = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mission #${delivery?.order?.id ?: ""}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                        controller.setZoom(14.0)
                        controller.setCenter(destPoint)

                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        locationOverlay.enableMyLocation()
                        locationOverlay.runOnFirstFix {
                            currentLocation = locationOverlay.myLocation
                        }
                        overlays.add(locationOverlay)
                    }
                },
                update = { mapView ->
                    mapView.overlays.removeAll { it is Marker || it is Polyline }
                    
                    val destMarker = Marker(mapView)
                    destMarker.position = destPoint
                    destMarker.title = "Destination"
                    mapView.overlays.add(destMarker)

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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.TopCenter).padding(16.dp))
            }

            // Mission Details Card
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Destinataire", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                    Text(delivery?.order?.recipient_name ?: "Chargement...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = primaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(delivery?.order?.recipient_address ?: "", style = MaterialTheme.typography.bodyMedium)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val status = delivery?.status
                        
                        if (status == "assigned") {
                            Button(
                                onClick = { 
                                    viewModel.updateStatus(token, deliveryId, "picked_up") {
                                        Toast.makeText(context, "Colis récupéré !", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Récupérer le colis")
                            }
                        } else if (status == "picked_up" || status == "in_transit") {
                            Column(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { cameraLauncher.launch() },
                                    modifier = Modifier.fillMaxWidth().height(45.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Preuve Photo")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onSignature,
                                    modifier = Modifier.fillMaxWidth().height(45.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Confirmer Livraison")
                                }
                            }
                        } else if (status == "delivered") {
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("LIVRÉ", fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
