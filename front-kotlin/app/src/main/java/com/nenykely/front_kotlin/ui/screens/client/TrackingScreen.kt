package com.nenykely.front_kotlin.ui.screens.client

import android.widget.Toast
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel
import com.nenykely.front_kotlin.viewmodel.AuthViewModel
import androidx.compose.ui.viewinterop.AndroidView
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
fun TrackingScreen(token: String, deliveryId: Int, viewModel: DeliveryViewModel, authViewModel: AuthViewModel, onBack: () -> Unit, onNavigateToDriver: (Int) -> Unit) {
    val delivery by viewModel.currentDelivery.collectAsState()
    val isDarkMode by authViewModel.isDarkMode.collectAsState()
    val primaryBlue = MaterialTheme.colorScheme.primary

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val formatTime: (String?) -> String = { isoString ->
        try {
            if (isoString.isNullOrBlank()) "--:--"
            else if (isoString.length <= 5) isoString 
            else ZonedDateTime.parse(isoString).format(timeFormatter)
        } catch (e: Exception) {
            isoString?.take(16)?.replace("T", " ") ?: "--:--"
        }
    }
    
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var breadcrumbPoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var isDataFresh by remember { mutableStateOf(false) }

    LaunchedEffect(deliveryId) {
        viewModel.fetchTracking(token, deliveryId)
    }

    val senderPoint = delivery?.order?.let { if (it.sender_lat != null && it.sender_lat != 0.0) GeoPoint(it.sender_lat, it.sender_lng ?: 0.0) else null }
    val destPoint = delivery?.order?.let { if (it.recipient_lat != null && it.recipient_lat != 0.0) GeoPoint(it.recipient_lat, it.recipient_lng ?: 0.0) else null }
    
    var driverPoint by remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(delivery) {
        delivery?.let { del ->
            val currentLat = del.driver?.current_lat
            val currentLng = del.driver?.current_lng

            if (currentLat != null && currentLng != null && currentLat != 0.0) {
                driverPoint = GeoPoint(currentLat, currentLng)
                isDataFresh = true
            } else {
                // Si pas de position temps réel, on prend la dernière position connue dans l'historique
                val lastKnown = del.statuses?.filter { it.lat != null && it.lat != 0.0 }?.maxByOrNull { it.created_at ?: "" }
                if (lastKnown != null) {
                    driverPoint = GeoPoint(lastKnown.lat!!, lastKnown.lng!!)
                }
            }

            breadcrumbPoints = del.statuses
                ?.filter { it.lat != null && it.lng != null && it.lat != 0.0 }
                ?.sortedBy { it.created_at }
                ?.map { GeoPoint(it.lat!!, it.lng!!) } ?: emptyList()
        }
    }

    // Fetch optimal route from OSRM
    LaunchedEffect(driverPoint, senderPoint, destPoint, delivery?.status) {
        if (delivery == null || driverPoint == null || destPoint == null) return@LaunchedEffect

        val status = delivery?.status
        val points = mutableListOf<String>()

        points.add("${driverPoint!!.longitude},${driverPoint!!.latitude}")

        // Si pas encore ramassé, on passe d'abord chez l'expéditeur
        if ((status == "assigned" || status == "pending") && senderPoint != null) {
            points.add("${senderPoint.longitude},${senderPoint.latitude}")
        }

        points.add("${destPoint.longitude},${destPoint.latitude}")

        val coordinates = points.joinToString(";")
        
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
        } catch (e: Exception) {
            Log.e("Routing", "Error", e)
        }
    }

    LaunchedEffect(Unit) {
        while(true) {
            viewModel.fetchTracking(token, deliveryId)
            kotlinx.coroutines.delay(3000) // Every 3s for tracking
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", 0))
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(14.0)
                    controller.setCenter(driverPoint)
                }
            },
            update = { mapView ->
                if (isDarkMode) {
                    mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                } else {
                    mapView.overlayManager.tilesOverlay.setColorFilter(null)
                }

                mapView.overlays.removeAll { it is Marker || it is Polyline }
                
                // Destination Marker
                destPoint?.let {
                    val destMarker = Marker(mapView)
                    destMarker.position = it
                    destMarker.title = "Destination"
                    destMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(destMarker)
                }

                // Sender Marker (if not picked up)
                if (delivery?.status == "assigned" || delivery?.status == "pending") {
                    senderPoint?.let {
                        val sMarker = Marker(mapView)
                        sMarker.position = it
                        sMarker.title = "Point de retrait"
                        sMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        mapView.overlays.add(sMarker)
                    }
                }

                // Driver Marker
                driverPoint?.let {
                    val driverMarker = Marker(mapView)
                    driverMarker.position = it
                    driverMarker.title = "Livreur"
                    driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    mapView.overlays.add(driverMarker)
                }

                // Breadcrumb (Past route) - Gray line
                if (breadcrumbPoints.size > 1) {
                    val line = Polyline()
                    line.setPoints(breadcrumbPoints)
                    line.outlinePaint.color = android.graphics.Color.GRAY
                    line.outlinePaint.strokeWidth = 5f
                    line.outlinePaint.alpha = 150
                    mapView.overlays.add(line)
                }

                // Planned Route (Future) - Blue line
                if (routePoints.isNotEmpty()) {
                    val line = Polyline()
                    line.setPoints(routePoints)
                    line.outlinePaint.color = android.graphics.Color.parseColor("#2563EB")
                    line.outlinePaint.strokeWidth = 10f
                    mapView.overlays.add(line)
                }
                
                if (isDataFresh) {
                    mapView.controller.animateTo(driverPoint)
                }

                mapView.invalidate()
            }
        )

        // Top Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                onClick = onBack
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
            
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(if (isDataFresh) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isDataFresh) "Synchronisé" else "En attente GPS...",
                        style = MaterialTheme.typography.labelLarge, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Bottom Sheet
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 24.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Box(
                    modifier = Modifier.padding(bottom = 24.dp).size(width = 48.dp, height = 4.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape).align(Alignment.CenterHorizontally)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text("Arrivée estimée", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(formatTime(delivery?.estimated_arrival), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = primaryBlue)
                    }
                    Surface(
                        color = primaryBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(primaryBlue, CircleShape))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(delivery?.status?.uppercase() ?: "EN ROUTE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = primaryBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { delivery?.driver?.id?.let { onNavigateToDriver(it) } }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(delivery?.driver?.user?.name ?: "Chargement...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Livreur", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("•", color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp), tint = Color(0xFFF59E0B))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(delivery?.driver?.rating?.toString() ?: "0.0", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = primaryBlue, onClick = {}) {
                            Icon(Icons.Default.Call, null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column {
                    TimelineRow(time = formatTime(delivery?.picked_up_at), text = "Pris en charge", isCompleted = delivery?.picked_up_at != null, primaryBlue = primaryBlue)
                    TimelineRow(time = "Actuellement", text = "Prochaine étape : Votre adresse", isCompleted = false, subtitle = delivery?.order?.recipient_address ?: "Adresse de destination", primaryBlue = primaryBlue)
                }
            }
        }
    }
}

@Composable
fun TimelineRow(time: String, text: String, isCompleted: Boolean, primaryBlue: Color, subtitle: String? = null) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
            Box(
                modifier = Modifier
                    .size(if (isCompleted) 24.dp else 28.dp)
                    .background(if (isCompleted) Color(0xFF059669) else Color.Transparent, CircleShape)
                    .border(2.dp, if (isCompleted) Color.Transparent else primaryBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = Color.White)
                else Box(modifier = Modifier.size(10.dp).background(primaryBlue, CircleShape))
            }
            if (subtitle == null) {
                Box(modifier = Modifier.weight(1f).width(2.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Text(time, style = MaterialTheme.typography.labelMedium, color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else primaryBlue, fontWeight = FontWeight.Bold)
            Text(text, style = if (isCompleted) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.titleMedium, color = if (isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
