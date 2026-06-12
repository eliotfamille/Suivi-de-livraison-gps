package com.nenykely.front_kotlin.ui.screens.client

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.nenykely.front_kotlin.data.models.Delivery
import com.nenykely.front_kotlin.data.models.User
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EcranLivraisonsClient(jeton: String, utilisateur: User?, modeleDeVue: DeliveryViewModel, lorsClicLivraison: (Delivery) -> Unit) {
    val livraisons by modeleDeVue.livraisons.collectAsState()
    val estEnChargement by modeleDeVue.estEnChargement.collectAsState()
    var ongletSelectionne by remember { mutableStateOf(0) }
    var afficherDialogueCreation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        modeleDeVue.recupererLivraisons(jeton)
    }
    
    if (afficherDialogueCreation) {
        val contexte = LocalContext.current
        DialogueCreationLivraison(
            jeton = jeton,
            modeleDeVue = modeleDeVue,
            utilisateurActuel = utilisateur,
            lorsFermeture = { afficherDialogueCreation = false },
            lorsSoumission = { donnees ->
                modeleDeVue.soumettreLivraison(jeton, donnees) { resultat ->
                    Toast.makeText(contexte, resultat, Toast.LENGTH_LONG).show()
                }
                afficherDialogueCreation = false
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Logistics Pro", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black, 
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { afficherDialogueCreation = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouvelle livraison")
            }
        }
    ) { espacement ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(espacement)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Text(
                text = "Mes livraisons",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Style des onglets
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    BoutonOnglet(
                        texte = "En cours", 
                        estSelectionne = ongletSelectionne == 0, 
                        modifier = Modifier.weight(1f),
                        lorsClic = { ongletSelectionne = 0 }
                    )
                    BoutonOnglet(
                        texte = "Historique", 
                        estSelectionne = ongletSelectionne == 1, 
                        modifier = Modifier.weight(1f),
                        lorsClic = { ongletSelectionne = 1 }
                    )
                }
            }

            if (estEnChargement && livraisons.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = estEnChargement,
                    onRefresh = { modeleDeVue.recupererLivraisons(jeton) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (livraisons.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aucune livraison trouvée", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val livraisonsFiltrees = if (ongletSelectionne == 0) {
                                livraisons.filter { it.status != "delivered" && it.status != "failed" }
                            } else {
                                livraisons.filter { it.status == "delivered" || it.status == "failed" }
                            }

                            items(livraisonsFiltrees) { livraison ->
                                CarteLivraison(livraison) {
                                    lorsClicLivraison(livraison)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChampRechercheUtilisateur(
    libelle: String,
    valeur: String,
    lorsChangementValeur: (String) -> Unit,
    lorsUtilisateurSelectionne: (User) -> Unit,
    modeleDeVue: DeliveryViewModel,
    jeton: String
) {
    var estEtendu by remember { mutableStateOf(false) }
    val utilisateurs by modeleDeVue.utilisateurs.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valeur,
            onValueChange = {
                val nettoye = it.replace("\n", "").replace("\r", "")
                lorsChangementValeur(nettoye)
                if (nettoye.length >= 2) {
                    modeleDeVue.rechercherUtilisateurs(jeton, nettoye)
                    estEtendu = true
                } else {
                    estEtendu = false
                }
            },
            label = { Text(libelle) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = { Icon(Icons.Default.Search, null) }
        )
        if (estEtendu && utilisateurs.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 8.dp,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
            ) {
                LazyColumn {
                    items(utilisateurs) { utilisateur ->
                        ListItem(
                            headlineContent = { Text(utilisateur.name) },
                            supportingContent = { Text(utilisateur.email) },
                            modifier = Modifier.clickable {
                                lorsUtilisateurSelectionne(utilisateur)
                                estEtendu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
fun DialogueCreationLivraison(
    jeton: String,
    modeleDeVue: DeliveryViewModel,
    utilisateurActuel: User?,
    lorsFermeture: () -> Unit,
    lorsSoumission: (Map<String, Any?>) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var poids by remember { mutableStateOf("") }
    var nomDestinataire by remember { mutableStateOf("") }
    var telephoneDestinataire by remember { mutableStateOf("") }
    var adresseDestinataire by remember { mutableStateOf("") }
    var nomExpediteur by remember { mutableStateOf(utilisateurActuel?.name ?: "") }
    var telephoneExpediteur by remember { mutableStateOf(utilisateurActuel?.phone ?: "") }
    var adresseExpediteur by remember { mutableStateOf(utilisateurActuel?.domicile ?: "") }
    
    var pointGeoExpediteur by remember { mutableStateOf<GeoPoint?>(
        if (utilisateurActuel?.domicile_lat != null) GeoPoint(utilisateurActuel.domicile_lat, utilisateurActuel.domicile_lng!!) else null
    ) }
    var pointGeoDestinataire by remember { mutableStateOf<GeoPoint?>(null) }
    
    var afficherCartePour by remember { mutableStateOf<String?>(null) } // "expediteur" ou "destinataire"

    val contexte = LocalContext.current
    val etatPermissionLocalisation = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    if (afficherCartePour != null) {
        val titre = if (afficherCartePour == "expediteur") "Position de départ" else "Position de destination"
        val positionInitiale = if (afficherCartePour == "expediteur") pointGeoExpediteur else pointGeoDestinataire
        
        AlertDialog(
            onDismissRequest = { afficherCartePour = null },
            title = { Text(titre) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", 0))
                            Configuration.getInstance().userAgentValue = ctx.packageName
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(12.0)
                                controller.setCenter(positionInitiale ?: GeoPoint(-18.8792, 47.5079))
                                
                                val coucheLocalisation = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                                coucheLocalisation.enableMyLocation()
                                overlays.add(coucheLocalisation)

                                val overlayEvenements = MapEventsOverlay(object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                        if (afficherCartePour == "expediteur") {
                                            pointGeoExpediteur = p
                                            adresseExpediteur = "Position choisie"
                                        } else {
                                            pointGeoDestinataire = p
                                            adresseDestinataire = "Position choisie"
                                        }
                                        invalidate()
                                        return true
                                    }
                                    override fun longPressHelper(p: GeoPoint): Boolean = false
                                })
                                overlays.add(overlayEvenements)
                            }
                        },
                        update = { vueCarte ->
                            vueCarte.overlays.removeAll { it is Marker }
                            val positionMarqueur = if (afficherCartePour == "expediteur") pointGeoExpediteur else pointGeoDestinataire
                            positionMarqueur?.let {
                                val marqueur = Marker(vueCarte)
                                marqueur.position = it
                                marqueur.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                vueCarte.overlays.add(marqueur)
                            }
                            vueCarte.invalidate()
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = { afficherCartePour = null }) { Text("Valider") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = lorsFermeture,
        title = { Text("Nouvelle livraison", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Expéditeur", fontWeight = FontWeight.Bold)
                    ChampRechercheUtilisateur(
                        libelle = "Nom ou Email",
                        valeur = nomExpediteur,
                        lorsChangementValeur = { nomExpediteur = it },
                        lorsUtilisateurSelectionne = { u ->
                            nomExpediteur = u.name
                            telephoneExpediteur = u.phone ?: ""
                            adresseExpediteur = u.domicile ?: ""
                            if (u.domicile_lat != null) pointGeoExpediteur = GeoPoint(u.domicile_lat, u.domicile_lng!!)
                        },
                        modeleDeVue = modeleDeVue,
                        jeton = jeton
                    )
                    OutlinedTextField(
                        value = telephoneExpediteur, 
                        onValueChange = { telephoneExpediteur = it.replace("\n", "").replace("\r", "") }, 
                        label = { Text("Téléphone") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    
                    Text("Position de départ :", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                if (etatPermissionLocalisation.status.isGranted) {
                                    Toast.makeText(contexte, "Récupération...", Toast.LENGTH_SHORT).show()
                                    afficherCartePour = "expediteur"
                                } else etatPermissionLocalisation.launchPermissionRequest()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("GPS", fontSize = 12.sp)
                        }
                        FilledTonalButton(
                            onClick = {
                                utilisateurActuel?.domicile_lat?.let { lat ->
                                    pointGeoExpediteur = GeoPoint(lat, utilisateurActuel.domicile_lng!!)
                                    adresseExpediteur = utilisateurActuel.domicile ?: "Domicile"
                                    Toast.makeText(contexte, "Domicile sélectionné", Toast.LENGTH_SHORT).show()
                                } ?: Toast.makeText(contexte, "Aucun domicile enregistré", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Domicile", fontSize = 12.sp)
                        }
                        FilledTonalButton(
                            onClick = { afficherCartePour = "expediteur" },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Carte", fontSize = 12.sp)
                        }
                    }
                    if (pointGeoExpediteur != null) {
                        Text("Coordonnées : ${String.format("%.4f", pointGeoExpediteur!!.latitude)}, ${String.format("%.4f", pointGeoExpediteur!!.longitude)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Destinataire", fontWeight = FontWeight.Bold)
                    ChampRechercheUtilisateur(
                        libelle = "Nom ou Email",
                        valeur = nomDestinataire,
                        lorsChangementValeur = { nomDestinataire = it },
                        lorsUtilisateurSelectionne = { u ->
                            nomDestinataire = u.name
                            telephoneDestinataire = u.phone ?: ""
                            adresseDestinataire = u.domicile ?: ""
                            if (u.domicile_lat != null) pointGeoDestinataire = GeoPoint(u.domicile_lat, u.domicile_lng!!)
                        },
                        modeleDeVue = modeleDeVue,
                        jeton = jeton
                    )
                    OutlinedTextField(
                        value = telephoneDestinataire, 
                        onValueChange = { telephoneDestinataire = it.replace("\n", "").replace("\r", "") }, 
                        label = { Text("Téléphone") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    
                    Text("Position de destination :", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { afficherCartePour = "destinataire" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Map, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Carte")
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Détails du colis", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = description, 
                        onValueChange = { description = it.replace("\n", "").replace("\r", "") }, 
                        label = { Text("Description") }, 
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = poids,
                        onValueChange = { poids = it.replace("\n", "").replace("\r", "") },
                        label = { Text("Poids (kg)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val donnees = mapOf(
                        "description" to description,
                        "weight_kg" to (poids.toDoubleOrNull() ?: 0.0),
                        "recipient_name" to nomDestinataire,
                        "recipient_phone" to telephoneDestinataire,
                        "recipient_address" to adresseDestinataire,
                        "recipient_lat" to (pointGeoDestinataire?.latitude ?: 0.0),
                        "recipient_lng" to (pointGeoDestinataire?.longitude ?: 0.0),
                        "sender_name" to nomExpediteur,
                        "sender_phone" to telephoneExpediteur,
                        "sender_address" to adresseExpediteur,
                        "sender_lat" to (pointGeoExpediteur?.latitude ?: 0.0),
                        "sender_lng" to (pointGeoExpediteur?.longitude ?: 0.0)
                    )
                    lorsSoumission(donnees)
                },
                enabled = pointGeoExpediteur != null && pointGeoDestinataire != null && description.isNotBlank()
            ) {
                Text("Soumettre")
            }
        },
        dismissButton = {
            TextButton(onClick = lorsFermeture) { Text("Annuler") }
        }
    )
}

@Composable
fun CarteLivraison(livraison: Delivery, lorsClic: () -> Unit) {
    Card(
        onClick = lorsClic,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            val (couleurAccent, libelleStatut, iconeStatut) = when (livraison.status) {
                "in_transit", "picked_up", "assigned" -> Triple(MaterialTheme.colorScheme.primary, "En route", Icons.Default.LocalShipping)
                "pending" -> Triple(Color(0xFFD97706), "En attente", Icons.Default.HourglassEmpty)
                "delivered" -> Triple(Color(0xFF059669), "Livré", Icons.Default.CheckCircle)
                else -> Triple(MaterialTheme.colorScheme.error, "Retardé", Icons.Default.ErrorOutline)
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(couleurAccent)
            )

            Column(modifier = Modifier.padding(20.dp).padding(start = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text(
                            "NUMÉRO DE SUIVI", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            "FR-${8000+livraison.id}-X", 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Surface(
                        color = couleurAccent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(iconeStatut, contentDescription = null, modifier = Modifier.size(16.dp), tint = couleurAccent)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                libelleStatut, 
                                style = MaterialTheme.typography.labelLarge, 
                                fontWeight = FontWeight.Bold, 
                                color = couleurAccent
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        livraison.order?.recipient_address ?: "Adresse non renseignée", 
                        style = MaterialTheme.typography.bodyLarge, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Dernière mise à jour : Récemment",
                        style = MaterialTheme.typography.bodyLarge, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (livraison.driver != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Livreur : ${livraison.driver?.user?.name ?: "Assigné"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
