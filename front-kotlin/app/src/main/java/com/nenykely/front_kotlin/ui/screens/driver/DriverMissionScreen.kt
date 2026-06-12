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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EcranMissionLivreur(jeton: String, livraisonId: Int, modeleDeVue: DeliveryViewModel, modeleDeVueAuth: AuthViewModel, lorsRetour: () -> Unit, lorsSignature: () -> Unit) {
    val contexte = LocalContext.current
    val livraison by modeleDeVue.livraisonActuelle.collectAsState()
    val estModeSombre by modeleDeVueAuth.estModeSombre.collectAsState()
    val erreur by modeleDeVue.erreur.collectAsState()
    val bleuPrimaire = MaterialTheme.colorScheme.primary

    val etatPermissionLocalisation = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(erreur) {
        erreur?.let {
            Toast.makeText(contexte, it, Toast.LENGTH_LONG).show()
        }
    }
    
    var localisationActuelle by remember { mutableStateOf<GeoPoint?>(null) }
    var pointsItineraire by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var chargementItineraire by remember { mutableStateOf(false) }

    // SUIVI GPS EN TEMPS RÉEL
    val gestionnaireLocalisation = remember { contexte.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val ecouteurLocalisation = remember {
        LocationListener { localisation ->
            val point = GeoPoint(localisation.latitude, localisation.longitude)
            localisationActuelle = point
            modeleDeVue.mettreAJourLocalisation(jeton, localisation.latitude, localisation.longitude)
        }
    }

    LaunchedEffect(etatPermissionLocalisation.status) {
        if (!etatPermissionLocalisation.status.isGranted) {
            etatPermissionLocalisation.launchPermissionRequest()
        }
    }

    DisposableEffect(etatPermissionLocalisation.status.isGranted) {
        if (etatPermissionLocalisation.status.isGranted) {
            try {
                gestionnaireLocalisation.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L, // 5 secondes
                    2f,    // 2 mètres
                    ecouteurLocalisation
                )
            } catch (e: SecurityException) {
                Log.e("GPS", "Erreur de permission", e)
            }
        }
        onDispose {
            gestionnaireLocalisation.removeUpdates(ecouteurLocalisation)
        }
    }

    val lanceurCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { imageBitmap ->
        if (imageBitmap != null) {
            try {
                val fichier = File(contexte.cacheDir, "preuve_${livraisonId}.jpg")
                val fluxSortie = FileOutputStream(fichier)
                imageBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fluxSortie)
                fluxSortie.flush()
                fluxSortie.close()
                modeleDeVue.definirPhotoPreuve(fichier)
                Toast.makeText(contexte, "Photo de preuve enregistrée !", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("Camera", "Erreur lors de l'enregistrement de la photo", e)
                Toast.makeText(contexte, "Erreur lors de l'enregistrement de la photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(livraisonId) {
        modeleDeVue.recupererSuivi(jeton, livraisonId)
    }

    val estRecupere = livraison?.status != "assigned" && livraison?.status != "pending"
    
    val latitudeCible = if (estRecupere) livraison?.order?.recipient_lat else livraison?.order?.sender_lat
    val longitudeCible = if (estRecupere) livraison?.order?.recipient_lng else livraison?.order?.sender_lng
    val nomCible = if (estRecupere) livraison?.order?.recipient_name else livraison?.order?.sender_name
    val adresseCible = if (estRecupere) livraison?.order?.recipient_address else livraison?.order?.sender_address
    val libelleCible = if (estRecupere) "Destinataire" else "Point de retrait"

    val pointDestination = GeoPoint(latitudeCible ?: -18.8792, longitudeCible ?: 47.5079)

    LaunchedEffect(localisationActuelle, pointDestination) {
        localisationActuelle?.let { depart ->
            chargementItineraire = true
            try {
                val urlString = "https://router.project-osrm.org/route/v1/driving/${depart.longitude},${depart.latitude};${pointDestination.longitude},${pointDestination.latitude}?overview=full&geometries=geojson"
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
                            val coordonnees = geometrie.getJSONArray("coordinates")
                            val points = mutableListOf<GeoPoint>()
                            for (i in 0 until coordonnees.length()) {
                                val coord = coordonnees.getJSONArray(i)
                                points.add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                            }
                            pointsItineraire = points
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Routing", "Erreur", e)
            } finally {
                chargementItineraire = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mission #${livraison?.order?.id ?: ""}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = lorsRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = MaterialTheme.colorScheme.onSurface)
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
                        controller.setZoom(14.0)
                        controller.setCenter(localisationActuelle ?: pointDestination)
                    }
                },
                update = { vueCarte ->
                    if (estModeSombre) {
                        vueCarte.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                    } else {
                        vueCarte.overlayManager.tilesOverlay.setColorFilter(null)
                    }

                    vueCarte.overlays.removeAll { it is Marker || it is Polyline }
                    
                    val marqueurDestination = Marker(vueCarte)
                    marqueurDestination.position = pointDestination
                    marqueurDestination.title = "Destination"
                    vueCarte.overlays.add(marqueurDestination)

                    localisationActuelle?.let {
                        val marqueurLivreur = Marker(vueCarte)
                        marqueurLivreur.position = it
                        marqueurLivreur.title = "Ma position"
                        marqueurLivreur.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        vueCarte.overlays.add(marqueurLivreur)
                    }

                    if (pointsItineraire.isNotEmpty()) {
                        val ligne = Polyline()
                        ligne.setPoints(pointsItineraire)
                        ligne.outlinePaint.color = android.graphics.Color.BLUE
                        ligne.outlinePaint.strokeWidth = 10f
                        vueCarte.overlays.add(ligne)
                    }
                    vueCarte.invalidate()
                }
            )

            if (chargementItineraire) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.TopCenter).padding(16.dp))
            }

            // Carte des détails de la mission
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
                    Text(libelleCible, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(nomCible ?: "Chargement...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = bleuPrimaire, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(adresseCible ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val statut = livraison?.status
                        
                        if (statut == "assigned") {
                            Button(
                                onClick = {
                                    modeleDeVue.mettreAJourStatut(jeton, livraisonId, "picked_up") {
                                        Toast.makeText(contexte, "Colis récupéré !", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = bleuPrimaire),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Récupérer le colis")
                            }
                        } else if (statut == "picked_up" || statut == "in_transit") {
                            val aUnePhoto by modeleDeVue.photoPreuve.collectAsState()
                            Column(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { lanceurCamera.launch(null) },
                                    modifier = Modifier.fillMaxWidth().height(45.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (aUnePhoto != null) Color(0xFF059669) else Color.Gray
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(if (aUnePhoto != null) Icons.Default.Check else Icons.Default.PhotoCamera, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (aUnePhoto != null) "Photo prise" else "Preuve Photo")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = lorsSignature,
                                    enabled = aUnePhoto != null,
                                    modifier = Modifier.fillMaxWidth().height(45.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (aUnePhoto != null) Color(0xFF10B981) else Color.Gray
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Confirmer Livraison")
                                }
                            }
                        } else if (statut == "delivered") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    color = Color(0xFFECFDF5),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("LIVRÉ", fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val nomFichier = "Bon_Livraison_${livraison?.order?.order_number ?: livraisonId}.pdf"
                                        modeleDeVue.telechargerRecu(contexte, jeton, livraisonId, nomFichier)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.FileDownload, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Télécharger le PDF")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
