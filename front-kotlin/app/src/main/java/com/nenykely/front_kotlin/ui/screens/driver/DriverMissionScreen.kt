package com.nenykely.front_kotlin.ui.screens.driver

import android.Manifest
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.widget.Toast
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.nenykely.front_kotlin.viewmodel.AuthViewModel
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.io.FileOutputStream
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverMissionScreen(token: String, deliveryId: Int, viewModel: DeliveryViewModel, authViewModel: AuthViewModel, onBack: () -> Unit, onSignature: () -> Unit) {
    val context = LocalContext.current
    val delivery by viewModel.currentDelivery.collectAsState()
    val isDarkMode by authViewModel.isDarkMode.collectAsState()
    val error by viewModel.error.collectAsState()
    val primaryBlue = MaterialTheme.colorScheme.primary
    
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }
    
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var isFetchingRoute by remember { mutableStateOf(false) }

    // REAL-TIME GPS TRACKING (FREE OPTION)
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val locationListener = remember {
        LocationListener { location ->
            val point = GeoPoint(location.latitude, location.longitude)
            currentLocation = point
            viewModel.updateLocation(token, location.latitude, location.longitude)
        }
    }

    DisposableEffect(Unit) {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L, // 2 seconds
                1f,    // 1 meter movement
                locationListener
            )
        } catch (e: SecurityException) {
            Log.e("GPS", "Permission error", e)
        }
        onDispose {
            locationManager.removeUpdates(locationListener)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            try {
                val file = File(context.cacheDir, "proof_${deliveryId}.jpg")
                val out = FileOutputStream(file)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
                viewModel.setProofPhoto(file)
                Toast.makeText(context, "Photo de preuve enregistrée !", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("Camera", "Error saving photo", e)
                Toast.makeText(context, "Erreur lors de l'enregistrement de la photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(deliveryId) {
        viewModel.fetchTracking(token, deliveryId)
    }

    val isPickedUp = delivery?.status != "assigned" && delivery?.status != "pending"
    
    val targetLat = if (isPickedUp) delivery?.order?.recipient_lat else delivery?.order?.sender_lat
    val targetLng = if (isPickedUp) delivery?.order?.recipient_lng else delivery?.order?.sender_lng
    val targetName = if (isPickedUp) delivery?.order?.recipient_name else delivery?.order?.sender_name
    val targetAddress = if (isPickedUp) delivery?.order?.recipient_address else delivery?.order?.sender_address
    val targetLabel = if (isPickedUp) "Destinataire" else "Point de retrait"

    val destPoint = GeoPoint(targetLat ?: -18.8792, targetLng ?: 47.5079)

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
                title = { Text("Mission #${delivery?.order?.id ?: ""}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = MaterialTheme.colorScheme.onSurface)
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
                        controller.setZoom(14.0)
                        controller.setCenter(currentLocation ?: destPoint)
                    }
                },
                update = { mapView ->
                    if (isDarkMode) {
                        mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                    } else {
                        mapView.overlayManager.tilesOverlay.setColorFilter(null)
                    }

                    mapView.overlays.removeAll { it is Marker || it is Polyline }
                    
                    val destMarker = Marker(mapView)
                    destMarker.position = destPoint
                    destMarker.title = "Destination"
                    mapView.overlays.add(destMarker)

                    currentLocation?.let {
                        val driverMarker = Marker(mapView)
                        driverMarker.position = it
                        driverMarker.title = "Ma position"
                        driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        mapView.overlays.add(driverMarker)
                    }

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
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(targetLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(targetName ?: "Chargement...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = primaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(targetAddress ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

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
                            val hasPhoto by viewModel.proofPhoto.collectAsState()
                            Column(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { cameraLauncher.launch(null) },
                                    modifier = Modifier.fillMaxWidth().height(45.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (hasPhoto != null) Color(0xFF059669) else Color.Gray
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(if (hasPhoto != null) Icons.Default.Check else Icons.Default.PhotoCamera, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (hasPhoto != null) "Photo prise" else "Preuve Photo")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onSignature,
                                    enabled = hasPhoto != null,
                                    modifier = Modifier.fillMaxWidth().height(45.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (hasPhoto != null) Color(0xFF10B981) else Color.Gray
                                    ),
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
