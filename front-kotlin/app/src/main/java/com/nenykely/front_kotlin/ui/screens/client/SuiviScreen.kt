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
fun EcranSuiviGlobal(jeton: String, utilisateur: com.nenykely.front_kotlin.data.models.User?, modeleDeVue: DeliveryViewModel, authViewModel: AuthViewModel) {
    val contexte = LocalContext.current
    val etatPermissionLocalisation = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val livraisons by modeleDeVue.livraisons.collectAsState()
    val estModeSombre by authViewModel.estModeSombre.collectAsState()
    val bleuPrimaire = MaterialTheme.colorScheme.primary
    
    var afficherPointsRetrait by remember { mutableStateOf(true) }
    var livraisonSelectionneePourInfos by remember { mutableStateOf<Delivery?>(null) }
    var pointsItineraire by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (!etatPermissionLocalisation.status.isGranted) {
            etatPermissionLocalisation.launchPermissionRequest()
        }
        while(true) {
            modeleDeVue.recupererLivraisons(jeton)
            delay(5000)
        }
    }

    LaunchedEffect(livraisonSelectionneePourInfos) {
        if (livraisonSelectionneePourInfos == null) {
            pointsItineraire = emptyList()
            return@LaunchedEffect
        }
        
        val livraison = livraisonSelectionneePourInfos!!
        val latLivreur = livraison.driver?.current_lat ?: 0.0
        val lngLivreur = livraison.driver?.current_lng ?: 0.0
        
        // Ignorer les points fantômes
        if (latLivreur == 0.0 || (latLivreur > -18.885 && latLivreur < -18.878 && lngLivreur > 47.505 && lngLivreur < 47.510)) {
            pointsItineraire = emptyList()
            return@LaunchedEffect
        }

        val pointLivreur = "${lngLivreur},${latLivreur}"
        val pointDestinataire = "${livraison.order?.recipient_lng},${livraison.order?.recipient_lat}"
        val coordonnees = if (livraison.status == "pending" || livraison.status == "assigned") {
            val pointExpediteur = "${livraison.order?.sender_lng},${livraison.order?.sender_lat}"
            "$pointLivreur;$pointExpediteur;$pointDestinataire"
        } else {
            "$pointLivreur;$pointDestinataire"
        }

        try {
            val urlString = "https://router.project-osrm.org/route/v1/driving/$coordonnees?overview=full&geometries=geojson"
            val reponse = withContext(Dispatchers.IO) {
                val connexion = URL(urlString).openConnection() as HttpURLConnection
                connexion.setRequestProperty("User-Agent", "Mozilla/5.0")
                if (connexion.responseCode == HttpURLConnection.HTTP_OK) {
                    connexion.inputStream.bufferedReader().readText()
                } else null
            }
            
            reponse?.let {
                val json = JSONObject(it)
                if (json.getString("code") == "Ok") {
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val geometrie = routes.getJSONObject(0).getJSONObject("geometry")
                        val coord = geometrie.getJSONArray("coordinates")
                        val nouveauxPoints = mutableListOf<GeoPoint>()
                        for (i in 0 until coord.length()) {
                            val c = coord.getJSONArray(i)
                            nouveauxPoints.add(GeoPoint(c.getDouble(1), c.getDouble(0)))
                        }
                        pointsItineraire = nouveauxPoints
                    }
                }
            }
        } catch (e: Exception) { Log.e("OSRM", "Erreur", e) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Suivi des Livraisons", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = bleuPrimaire) },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (afficherPointsRetrait) "Départ" else "Dest.", style = MaterialTheme.typography.labelSmall)
                        Switch(
                            checked = !afficherPointsRetrait,
                            onCheckedChange = { afficherPointsRetrait = !it },
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { espacement ->
        Box(modifier = Modifier.fillMaxSize().padding(espacement)) {
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
                update = { vueCarte ->
                    if (estModeSombre) {
                        vueCarte.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                    } else {
                        vueCarte.overlayManager.tilesOverlay.setColorFilter(null)
                    }

                    vueCarte.overlays.removeAll { it is Marker || it is Polyline }
                    
                    livraisons.filter { it.status != "delivered" }.forEach { livraison ->
                        val lat = if (afficherPointsRetrait) livraison.order?.sender_lat else livraison.order?.recipient_lat
                        val lng = if (afficherPointsRetrait) livraison.order?.sender_lng else livraison.order?.recipient_lng
                        
                        if (lat != null && lng != null && lat != 0.0) {
                            val marqueur = Marker(vueCarte)
                            marqueur.position = GeoPoint(lat, lng)
                            marqueur.title = "Colis #${livraison.id}"
                            marqueur.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marqueur.setOnMarkerClickListener { _, _ ->
                                livraisonSelectionneePourInfos = livraison
                                true
                            }
                            vueCarte.overlays.add(marqueur)
                        }
                        
                        val latLiv = livraison.driver?.current_lat ?: 0.0
                        val lngLiv = livraison.driver?.current_lng ?: 0.0
                        val estFantome = latLiv > -18.885 && latLiv < -18.878 && lngLiv > 47.505 && lngLiv < 47.510
                        
                        if (latLiv != 0.0 && !estFantome) {
                            val marqueurLivreur = Marker(vueCarte)
                            marqueurLivreur.position = GeoPoint(latLiv, lngLiv)
                            marqueurLivreur.title = "Livreur #${livraison.id}"
                            vueCarte.overlays.add(marqueurLivreur)
                        }
                    }

                    if (pointsItineraire.isNotEmpty()) {
                        val ligne = Polyline()
                        ligne.setPoints(pointsItineraire)
                        ligne.outlinePaint.color = android.graphics.Color.parseColor("#2563EB")
                        ligne.outlinePaint.strokeWidth = 8f
                        vueCarte.overlays.add(ligne)
                    }
                    vueCarte.invalidate()
                }
            )
            
            livraisonSelectionneePourInfos?.let { livraison ->
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Détails Livraison #${livraison.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Destinataire: ${livraison.order?.recipient_name ?: "N/A"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Adresse: ${livraison.order?.recipient_address ?: "N/A"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Statut: ${livraison.status}", color = bleuPrimaire, fontWeight = FontWeight.Bold)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (utilisateur?.isDriver() == true && (livraison.status == "pending" || livraison.status == "assigned")) {
                            Button(
                                onClick = {
                                    modeleDeVue.accepterLivraison(jeton, livraison.id) {
                                        livraisonSelectionneePourInfos = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = bleuPrimaire)
                            ) {
                                Text("Accepter cette mission")
                            }
                        }
                        
                        TextButton(
                            onClick = { livraisonSelectionneePourInfos = null },
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
