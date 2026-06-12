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
fun EcranSuivi(jeton: String, livraisonId: Int, modeleDeVue: DeliveryViewModel, authViewModel: AuthViewModel, lorsRetour: () -> Unit, lorsNavigationVersLivreur: (Int) -> Unit) {
    val contexte = LocalContext.current
    val livraison by modeleDeVue.livraisonActuelle.collectAsState()
    val estModeSombre by authViewModel.estModeSombre.collectAsState()
    val bleuPrimaire = MaterialTheme.colorScheme.primary

    val formateurHeure = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val formaterHeure: (String?) -> String = { chaineIso ->
        try {
            if (chaineIso.isNullOrBlank()) "--:--"
            else if (chaineIso.length <= 5) chaineIso 
            else ZonedDateTime.parse(chaineIso).format(formateurHeure)
        } catch (e: Exception) {
            chaineIso?.take(16)?.replace("T", " ") ?: "--:--"
        }
    }
    
    var pointsItineraire by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var pointsHistorique by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var donneesFraiches by remember { mutableStateOf(false) }

    LaunchedEffect(livraisonId) {
        modeleDeVue.recupererSuivi(jeton, livraisonId)
    }

    val pointExpediteur = livraison?.order?.let { if (it.sender_lat != null && it.sender_lat != 0.0) GeoPoint(it.sender_lat, it.sender_lng ?: 0.0) else null }
    val pointDestination = livraison?.order?.let { if (it.recipient_lat != null && it.recipient_lat != 0.0) GeoPoint(it.recipient_lat, it.recipient_lng ?: 0.0) else null }
    
    var pointLivreur by remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(livraison) {
        livraison?.let { liv ->
            val latActuelle = liv.driver?.current_lat
            val lngActuelle = liv.driver?.current_lng

            if (latActuelle != null && lngActuelle != null && latActuelle != 0.0) {
                pointLivreur = GeoPoint(latActuelle, lngActuelle)
                donneesFraiches = true
            } else {
                donneesFraiches = false
                // Si pas de position temps réel, on prend la dernière position connue dans l'historique
                val dernierePositionConnue = liv.statuses?.filter { it.lat != null && it.lat != 0.0 }?.maxByOrNull { it.created_at ?: "" }
                if (dernierePositionConnue != null) {
                    pointLivreur = GeoPoint(dernierePositionConnue.lat!!, dernierePositionConnue.lng!!)
                }
            }

            pointsHistorique = liv.statuses
                ?.filter { it.lat != null && it.lng != null && it.lat != 0.0 }
                ?.sortedBy { it.created_at }
                ?.map { GeoPoint(it.lat!!, it.lng!!) } ?: emptyList()
        }
    }

    // Récupérer l'itinéraire optimal via OSRM
    LaunchedEffect(pointLivreur, pointExpediteur, pointDestination, livraison?.status) {
        if (livraison == null || pointLivreur == null || pointDestination == null) return@LaunchedEffect

        val statut = livraison?.status
        val points = mutableListOf<String>()

        points.add("${pointLivreur!!.longitude},${pointLivreur!!.latitude}")

        // Si pas encore ramassé, on passe d'abord chez l'expéditeur
        if ((statut == "assigned" || statut == "pending") && pointExpediteur != null) {
            points.add("${pointExpediteur.longitude},${pointExpediteur.latitude}")
        }

        points.add("${pointDestination.longitude},${pointDestination.latitude}")

        val coordonnees = points.joinToString(";")
        
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
        } catch (e: Exception) {
            Log.e("Routing", "Erreur", e)
        }
    }

    LaunchedEffect(Unit) {
        while(true) {
            modeleDeVue.recupererSuivi(jeton, livraisonId)
            kotlinx.coroutines.delay(3000) // Toutes les 3s pour le suivi
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
                    controller.setCenter(pointLivreur)
                }
            },
            update = { vueCarte ->
                if (estModeSombre) {
                    vueCarte.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                } else {
                    vueCarte.overlayManager.tilesOverlay.setColorFilter(null)
                }

                vueCarte.overlays.removeAll { it is Marker || it is Polyline }
                
                // Marqueur Destination
                pointDestination?.let {
                    val marqueurDestination = Marker(vueCarte)
                    marqueurDestination.position = it
                    marqueurDestination.title = "Destination"
                    marqueurDestination.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    vueCarte.overlays.add(marqueurDestination)
                }

                // Marqueur Expéditeur (si pas encore récupéré)
                if (livraison?.status == "assigned" || livraison?.status == "pending") {
                    pointExpediteur?.let {
                        val marqueurExp = Marker(vueCarte)
                        marqueurExp.position = it
                        marqueurExp.title = "Point de retrait"
                        marqueurExp.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        vueCarte.overlays.add(marqueurExp)
                    }
                }

                // Marqueur Livreur
                pointLivreur?.let {
                    val marqueurLivreur = Marker(vueCarte)
                    marqueurLivreur.position = it
                    marqueurLivreur.title = "Livreur"
                    marqueurLivreur.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    vueCarte.overlays.add(marqueurLivreur)
                }

                // Historique (Itinéraire passé) - Ligne grise
                if (pointsHistorique.size > 1) {
                    val ligne = Polyline()
                    ligne.setPoints(pointsHistorique)
                    ligne.outlinePaint.color = android.graphics.Color.GRAY
                    ligne.outlinePaint.strokeWidth = 5f
                    ligne.outlinePaint.alpha = 150
                    vueCarte.overlays.add(ligne)
                }

                // Itinéraire prévu (Futur) - Ligne bleue
                if (pointsItineraire.isNotEmpty()) {
                    val ligne = Polyline()
                    ligne.setPoints(pointsItineraire)
                    ligne.outlinePaint.color = android.graphics.Color.parseColor("#2563EB")
                    ligne.outlinePaint.strokeWidth = 10f
                    vueCarte.overlays.add(ligne)
                }
                
                if (donneesFraiches) {
                    vueCarte.controller.animateTo(pointLivreur)
                }

                vueCarte.invalidate()
            }
        )

        // Commandes en haut
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
                onClick = lorsRetour
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
                    Box(modifier = Modifier.size(8.dp).background(if (donneesFraiches) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (donneesFraiches) "Synchronisé" else "En attente GPS...",
                        style = MaterialTheme.typography.labelLarge, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Panneau du bas
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
                        Text(formaterHeure(livraison?.estimated_arrival), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = bleuPrimaire)
                    }
                    Surface(
                        color = bleuPrimaire.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(bleuPrimaire, CircleShape))
                            Spacer(modifier = Modifier.width(10.dp))
                            val libelleStatut = when(livraison?.status) {
                                "pending" -> "EN ATTENTE"
                                "assigned" -> "ASSIGNÉ"
                                "picked_up" -> "RÉCUPÉRÉ"
                                "in_transit" -> "EN ROUTE"
                                "delivered" -> "LIVRÉ"
                                else -> livraison?.status?.uppercase() ?: "CHARGEMENT"
                            }
                            Text(libelleStatut, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = bleuPrimaire)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { livraison?.driver?.id?.let { lorsNavigationVersLivreur(it) } }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(livraison?.driver?.user?.name ?: "Chargement...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Livreur", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("•", color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp), tint = Color(0xFFF59E0B))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(livraison?.driver?.rating?.toString() ?: "0.0", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = bleuPrimaire,
                            onClick = {
                                livraison?.driver?.user?.phone?.let { tel ->
                                    val intention = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$tel"))
                                    contexte.startActivity(intention)
                                } ?: run {
                                    Toast.makeText(contexte, "Numéro de téléphone indisponible", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Call, null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column {
                    LigneTimeline(heure = formaterHeure(livraison?.picked_up_at), texte = "Pris en charge", estTermine = livraison?.picked_up_at != null, bleuPrimaire = bleuPrimaire)
                    LigneTimeline(heure = "Actuellement", texte = "Prochaine étape : Votre adresse", estTermine = false, sousTitre = livraison?.order?.recipient_address ?: "Adresse de destination", bleuPrimaire = bleuPrimaire)
                }
            }
        }
    }
}

@Composable
fun LigneTimeline(heure: String, texte: String, estTermine: Boolean, bleuPrimaire: Color, sousTitre: String? = null) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
            Box(
                modifier = Modifier
                    .size(if (estTermine) 24.dp else 28.dp)
                    .background(if (estTermine) Color(0xFF059669) else Color.Transparent, CircleShape)
                    .border(2.dp, if (estTermine) Color.Transparent else bleuPrimaire, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (estTermine) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = Color.White)
                else Box(modifier = Modifier.size(10.dp).background(bleuPrimaire, CircleShape))
            }
            if (sousTitre == null) {
                Box(modifier = Modifier.weight(1f).width(2.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Text(heure, style = MaterialTheme.typography.labelMedium, color = if (estTermine) MaterialTheme.colorScheme.onSurfaceVariant else bleuPrimaire, fontWeight = FontWeight.Bold)
            Text(texte, style = if (estTermine) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.titleMedium, color = if (estTermine) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
            if (sousTitre != null) {
                Text(sousTitre, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
