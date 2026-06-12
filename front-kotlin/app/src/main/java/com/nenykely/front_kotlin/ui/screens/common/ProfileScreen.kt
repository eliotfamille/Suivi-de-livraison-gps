package com.nenykely.front_kotlin.ui.screens.common

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.nenykely.front_kotlin.data.api.RetrofitClient
import com.nenykely.front_kotlin.viewmodel.AuthViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranProfil(modeleDeVue: AuthViewModel, lorsDeconnexion: () -> Unit) {
    val utilisateur by modeleDeVue.utilisateur.collectAsState()
    val estEnChargement by modeleDeVue.estEnChargement.collectAsState()
    val contexte = LocalContext.current

    val obtenirUrlImage: (String?) -> String? = { chemin ->
        if (chemin.isNullOrBlank()) null
        else if (chemin.startsWith("http") || chemin.startsWith("data:image")) chemin
        else {
            val cheminPropre = chemin.removePrefix("/")
            val cheminFinal = if (cheminPropre.startsWith("storage/")) cheminPropre else "storage/$cheminPropre"
            "${RetrofitClient.BASE_URL.removeSuffix("/")}/$cheminFinal"
        }
    }
    
    var dialogueAdressePar by remember { mutableStateOf<String?>(null) } // "domicile" ou "bureau"
    var texteAdresse by remember { mutableStateOf("") }
    var pointGeoSelectionne by remember { mutableStateOf<GeoPoint?>(null) }
    var afficherCarte by remember { mutableStateOf(false) }
    
    var dialoguePersonnelActif by remember { mutableStateOf(false) }
    var nomEdition by remember { mutableStateOf("") }
    var telephoneEdition by remember { mutableStateOf("") }

    var dialogueVehiculeActif by remember { mutableStateOf(false) }
    var typeVehiculeEdition by remember { mutableStateOf("") }
    var modeleVehiculeEdition by remember { mutableStateOf("") }
    var plaqueVehiculeEdition by remember { mutableStateOf("") }

    val lanceurSelectionImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fichier = uriVersFichier(contexte, it)
            if (fichier != null) {
                modeleDeVue.mettreAJourProfil(fichierImage = fichier)
            }
        }
    }

    if (afficherCarte) {
        AlertDialog(
            onDismissRequest = { afficherCarte = false },
            title = { Text("Choisir sur la carte") },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", 0))
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(12.0)
                                controller.setCenter(pointGeoSelectionne ?: GeoPoint(-18.8792, 47.5079))
                                
                                val coucheLocalisation = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                                coucheLocalisation.enableMyLocation()
                                overlays.add(coucheLocalisation)

                                val overlayEvenements = MapEventsOverlay(object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                        pointGeoSelectionne = p
                                        texteAdresse = "Position choisie"
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
                            pointGeoSelectionne?.let {
                                val marqueur = Marker(vueCarte)
                                marqueur.position = it
                                marqueur.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                vueCarte.overlays.add(marqueur)
                            }
                        }
                    )
                }
            },
            confirmButton = { Button(onClick = { afficherCarte = false }) { Text("Valider") } }
        )
    }

    if (dialoguePersonnelActif) {
        AlertDialog(
            onDismissRequest = { dialoguePersonnelActif = false },
            title = { Text("Modifier mes infos") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nomEdition,
                        onValueChange = { nomEdition = it.replace("\n", "") },
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = telephoneEdition,
                        onValueChange = { telephoneEdition = it.replace("\n", "") },
                        label = { Text("Téléphone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    modeleDeVue.mettreAJourProfil(nom = nomEdition, telephone = telephoneEdition)
                    dialoguePersonnelActif = false
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { dialoguePersonnelActif = false }) { Text("Annuler") } }
        )
    }

    if (dialogueVehiculeActif) {
        AlertDialog(
            onDismissRequest = { dialogueVehiculeActif = false },
            title = { Text("Modifier le véhicule") },
            text = {
                Column {
                    OutlinedTextField(
                        value = typeVehiculeEdition,
                        onValueChange = { typeVehiculeEdition = it },
                        label = { Text("Type de véhicule") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modeleVehiculeEdition,
                        onValueChange = { modeleVehiculeEdition = it },
                        label = { Text("Modèle") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = plaqueVehiculeEdition,
                        onValueChange = { plaqueVehiculeEdition = it },
                        label = { Text("Immatriculation") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    modeleDeVue.mettreAJourProfil(
                        type_vehicule = typeVehiculeEdition,
                        modele_vehicule = modeleVehiculeEdition,
                        plaque_vehicule = plaqueVehiculeEdition
                    )
                    dialogueVehiculeActif = false
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { dialogueVehiculeActif = false }) { Text("Annuler") } }
        )
    }

    if (dialogueAdressePar != null) {
        AlertDialog(
            onDismissRequest = { dialogueAdressePar = null },
            title = { Text("Modifier l'adresse ${if (dialogueAdressePar == "domicile") "domicile" else "bureau"}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = texteAdresse,
                        onValueChange = { texteAdresse = it.replace("\n", "").replace("\r", "") },
                        label = { Text("Adresse") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { afficherCarte = true }) {
                                Icon(Icons.Default.Map, contentDescription = "Carte")
                            }
                        }
                    )
                    if (pointGeoSelectionne != null) {
                        Text(
                            "Coordonnées : ${String.format("%.4f", pointGeoSelectionne!!.latitude)}, ${String.format("%.4f", pointGeoSelectionne!!.longitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (dialogueAdressePar == "domicile") {
                        modeleDeVue.mettreAJourProfil(domicile = texteAdresse, domicile_lat = pointGeoSelectionne?.latitude, domicile_lng = pointGeoSelectionne?.longitude)
                    } else {
                        modeleDeVue.mettreAJourProfil(bureau = texteAdresse, bureau_lat = pointGeoSelectionne?.latitude, bureau_lng = pointGeoSelectionne?.longitude)
                    }
                    dialogueAdressePar = null
                }) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogueAdressePar = null }) {
                    Text("Annuler")
                }
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
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = estEnChargement,
            onRefresh = { modeleDeVue.recupererProfil() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // En-tête du profil
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        modifier = Modifier.size(100.dp).clickable { lanceurSelectionImage.launch("image/*") },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (!utilisateur?.avatar.isNullOrEmpty()) {
                            AsyncImage(
                                model = obtenirUrlImage(utilisateur?.avatar),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        modifier = Modifier.size(28.dp).clickable { lanceurSelectionImage.launch("image/*") },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(6.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(utilisateur?.name ?: "Jean Dupont", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                if (utilisateur?.isDriver() == true && utilisateur?.driver != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", utilisateur?.driver?.rating ?: 0.0),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " (${utilisateur?.driver?.rating_count ?: 0} avis)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                val estClient = utilisateur?.role?.equals("client", ignoreCase = true) == true || 
                                utilisateur?.roles?.any { it.equals("client", ignoreCase = true) } == true
                val libelleRole = if (utilisateur?.isDriver() == true) "Livreur" else if (estClient) "Client" else null
                
                Text(
                    text = libelleRole ?: "Chargement...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Sections
                SectionProfil(titre = "INFORMATIONS PERSONNELLES", texteAction = "Modifier", lorsAction = {
                    nomEdition = utilisateur?.name ?: ""
                    telephoneEdition = utilisateur?.phone ?: ""
                    dialoguePersonnelActif = true
                }) {
                    ItemProfil(icone = Icons.Default.Email, libelle = "Email", valeur = utilisateur?.email ?: "jean.dupont@email.com")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ItemProfil(icone = Icons.Default.Phone, libelle = "Téléphone", valeur = utilisateur?.phone ?: "+33 6 12 34 56 78")
                }
                
                SectionProfil(titre = "ADRESSES ENREGISTRÉES") {
                    ItemAdresse(
                        icone = Icons.Default.Home, 
                        libelle = "Domicile", 
                        adresse = utilisateur?.domicile ?: "Non renseigné",
                        lorsClic = {
                            texteAdresse = utilisateur?.domicile ?: ""
                            pointGeoSelectionne = if (utilisateur?.domicile_lat != null) GeoPoint(utilisateur!!.domicile_lat!!, utilisateur!!.domicile_lng!!) else null
                            dialogueAdressePar = "domicile"
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ItemAdresse(
                        icone = Icons.Default.Work, 
                        libelle = "Bureau", 
                        adresse = utilisateur?.bureau ?: "Non renseigné",
                        lorsClic = {
                            texteAdresse = utilisateur?.bureau ?: ""
                            pointGeoSelectionne = if (utilisateur?.bureau_lat != null) GeoPoint(utilisateur!!.bureau_lat!!, utilisateur!!.bureau_lng!!) else null
                            dialogueAdressePar = "bureau"
                        }
                    )
                }

                if (utilisateur?.isDriver() == true && utilisateur?.driver != null) {
                    SectionProfil(titre = "DÉTAILS DU VÉHICULE", texteAction = "Modifier", lorsAction = {
                        typeVehiculeEdition = utilisateur?.driver?.vehicle_type ?: ""
                        modeleVehiculeEdition = utilisateur?.driver?.vehicle_model ?: ""
                        plaqueVehiculeEdition = utilisateur?.driver?.vehicle_plate ?: ""
                        dialogueVehiculeActif = true
                    }) {
                        ItemProfil(icone = Icons.Default.Category, libelle = "Type", valeur = utilisateur?.driver?.vehicle_type ?: "Non renseigné")
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        ItemProfil(icone = Icons.Default.LocalShipping, libelle = "Modèle", valeur = utilisateur?.driver?.vehicle_model ?: "Non renseigné")
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        ItemProfil(icone = Icons.Default.Badge, libelle = "Immatriculation", valeur = utilisateur?.driver?.vehicle_plate ?: "Non renseigné")
                    }
                }
                
                SectionProfil(titre = "PRÉFÉRENCES") {
                    val estModeSombre by modeleDeVue.estModeSombre.collectAsState()
                    ItemPreference(icone = Icons.Default.Notifications, libelle = "Notifications Push", estCoche = true)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ItemPreference(icone = Icons.Default.AlternateEmail, libelle = "Emails de suivi", estCoche = false)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ItemPreference(
                        icone = Icons.Default.DarkMode, 
                        libelle = "Mode Sombre", 
                        estCoche = estModeSombre,
                        lorsChangementCoche = { modeleDeVue.basculerModeSombre() }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedButton(
                    onClick = { 
                        modeleDeVue.seDeconnecter()
                        lorsDeconnexion()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Déconnexion", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Version 2.4.0 (Build 108)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SectionProfil(titre: String, texteAction: String? = null, lorsAction: () -> Unit = {}, contenu: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(titre, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
            if (texteAction != null) {
                TextButton(onClick = lorsAction) {
                    Text(texteAction, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(content = contenu)
        }
    }
}

@Composable
fun ItemProfil(icone: ImageVector, libelle: String, valeur: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(icone, contentDescription = null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(libelle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valeur, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ItemAdresse(icone: ImageVector, libelle: String, adresse: String, lorsClic: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { lorsClic() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(icone, contentDescription = null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(libelle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(adresse, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ItemPreference(icone: ImageVector, libelle: String, estCoche: Boolean, lorsChangementCoche: (Boolean) -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(libelle, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Switch(
            checked = estCoche,
            onCheckedChange = lorsChangementCoche,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = MaterialTheme.colorScheme.primary)
        )
    }
}

private fun uriVersFichier(contexte: android.content.Context, uri: Uri): File? {
    val curseur = contexte.contentResolver.query(uri, null, null, null, null) ?: return null
    val indexNom = curseur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    curseur.moveToFirst()
    val nom = curseur.getString(indexNom)
    curseur.close()
    
    val fichier = File(contexte.cacheDir, nom)
    val fluxEntree = contexte.contentResolver.openInputStream(uri) ?: return null
    val fluxSortie = FileOutputStream(fichier)
    fluxEntree.copyTo(fluxSortie)
    fluxEntree.close()
    fluxSortie.close()
    return fichier
}
