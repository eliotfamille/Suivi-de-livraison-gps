package com.nenykely.front_kotlin.ui.screens.client

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(token: String, deliveryId: Int, viewModel: DeliveryViewModel, onBack: () -> Unit, onNavigateToDriver: (Int) -> Unit) {
    val context = LocalContext.current
    val delivery by viewModel.currentDelivery.collectAsState()
    val primaryBlue = Color(0xFF2563EB)
    
    LaunchedEffect(deliveryId) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
        viewModel.fetchTracking(token, deliveryId)
    }

    val destLat = delivery?.order?.recipient_lat ?: -18.8792
    val destLng = delivery?.order?.recipient_lng ?: 47.5079
    val destPoint = GeoPoint(destLat, destLng)
    
    val driverStatus = delivery?.statuses?.firstOrNull { it.lat != null && it.lng != null }
    val driverPoint = if (driverStatus != null) GeoPoint(driverStatus.lat!!, driverStatus.lng!!) else GeoPoint(destLat - 0.01, destLng - 0.01)

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(13.0)
                    controller.setCenter(destPoint)
                    
                    // Ajout de la position actuelle
                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    locationOverlay.enableMyLocation()
                    overlays.add(locationOverlay)
                }
            },
            update = { mapView ->
                mapView.overlays.removeAll { it is Marker || it is Polyline }
                
                // Marqueur Destination
                val destMarker = Marker(mapView)
                destMarker.position = destPoint
                destMarker.title = "Destination"
                destMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(destMarker)
                
                // Marqueur Livreur
                val driverMarker = Marker(mapView)
                driverMarker.position = driverPoint
                driverMarker.title = "Livreur"
                driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(driverMarker)

                // Ligne de trajet
                val line = Polyline()
                line.setPoints(listOf(driverPoint, destPoint))
                line.outlinePaint.color = android.graphics.Color.BLUE
                line.outlinePaint.strokeWidth = 5f
                mapView.overlays.add(line)
                
                mapView.invalidate()
            }
        )

        // Top Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
                onClick = onBack
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", modifier = Modifier.padding(10.dp), tint = Color(0xFF1E293B))
            }
            
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Mise à jour en direct", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }
            }
        }

        // Bottom Sheet
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = Color.White,
            shadowElevation = 24.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .size(width = 48.dp, height = 4.dp)
                        .background(Color(0xFFE2E8F0), CircleShape)
                        .align(Alignment.CenterHorizontally)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text("Arrivée estimée", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text("14:30 - 14:45", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = primaryBlue)
                    }
                    Surface(
                        color = Color(0xFFDBEAFE),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF3B82F6), CircleShape))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("EN ROUTE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color(0xFF1E3A8A))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Driver Info Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { delivery?.driver?.id?.let { onNavigateToDriver(it) } }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = Color(0xFFE2E8F0)) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.padding(12.dp), tint = Color(0xFF94A3B8))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(delivery?.driver?.user?.name ?: "Thomas Dubois", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Livreur", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("•", color = Color(0xFFCBD5E1))
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp), tint = Color(0xFFF59E0B))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("4.9", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            }
                        }
                        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = primaryBlue, onClick = {}) {
                            Icon(Icons.Default.Call, null, modifier = Modifier.padding(12.dp), tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Timeline
                Column {
                    TimelineRow(time = "10:15", text = "Colis pris en charge", isCompleted = true)
                    TimelineRow(time = "Actuellement", text = "Prochaine étape : Votre adresse", isCompleted = false, subtitle = delivery?.order?.recipient_address ?: "123 Avenue des Champs-Élysées, Paris")
                }
            }
        }
    }
}

@Composable
fun TimelineRow(time: String, text: String, isCompleted: Boolean, subtitle: String? = null) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
            Box(
                modifier = Modifier
                    .size(if (isCompleted) 24.dp else 28.dp)
                    .background(if (isCompleted) Color(0xFF059669) else Color.Transparent, CircleShape)
                    .border(2.dp, if (isCompleted) Color.Transparent else Color(0xFF2563EB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = Color.White)
                else Box(modifier = Modifier.size(10.dp).background(Color(0xFF2563EB), CircleShape))
            }
            if (subtitle == null) {
                Box(modifier = Modifier.weight(1f).width(2.dp).background(Color(0xFFE2E8F0)))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Text(time, style = MaterialTheme.typography.labelMedium, color = if (isCompleted) Color(0xFF64748B) else Color(0xFF2563EB), fontWeight = FontWeight.Bold)
            Text(text, style = if (isCompleted) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.titleMedium, color = if (isCompleted) Color(0xFF94A3B8) else Color(0xFF0F172A))
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
            }
        }
    }
}
