package com.nenykely.front_kotlin.ui.screens.client

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nenykely.front_kotlin.data.api.RetrofitClient
import com.nenykely.front_kotlin.data.models.Delivery
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranDetailsLivraison(jeton: String, livraisonId: Int, modeleDeVue: DeliveryViewModel, lorsRetour: () -> Unit, lorsNavigationVersLivreur: (Int) -> Unit) {
    val contexte = androidx.compose.ui.platform.LocalContext.current
    val livraison by modeleDeVue.livraisonActuelle.collectAsState()
    val estEnChargement by modeleDeVue.estEnChargement.collectAsState()
    
    val formateurDate = remember { DateTimeFormatter.ofPattern("dd MMM yyyy à HH:mm") }
    val formaterDateComplete: (String?) -> String = { chaineIso ->
        try {
            if (chaineIso.isNullOrBlank()) "N/A"
            else ZonedDateTime.parse(chaineIso).format(formateurDate)
        } catch (e: Exception) {
            chaineIso?.take(16)?.replace("T", " ") ?: "N/A"
        }
    }

    val obtenirUrlImage: (String?) -> Any? = { chemin ->
        if (chemin.isNullOrBlank()) null
        else {
            val cheminNormalise = chemin.replace("\\/", "/")
            if (cheminNormalise.startsWith("http")) cheminNormalise
            else if (cheminNormalise.startsWith("data:image")) {
                try {
                    val donneesBase64 = cheminNormalise.substringAfter("base64,")
                    val octetsImage = android.util.Base64.decode(donneesBase64, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(octetsImage, 0, octetsImage.size)
                } catch (e: Exception) {
                    cheminNormalise
                }
            } else {
                val cheminPropre = cheminNormalise.removePrefix("/")
                val cheminFinal = if (cheminPropre.startsWith("storage/")) cheminPropre else "storage/$cheminPropre"
                "${RetrofitClient.BASE_URL.removeSuffix("/")}/$cheminFinal"
            }
        }
    }

    LaunchedEffect(livraisonId) {
        modeleDeVue.recupererSuivi(jeton, livraisonId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails de la livraison", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = lorsRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { espacement ->
        if (estEnChargement && livraison == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(espacement)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // En-tête du statut
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (couleur, libelle) = when(livraison?.status) {
                                "delivered" -> Color(0xFF059669) to "LIVRÉ"
                                "failed" -> MaterialTheme.colorScheme.error to "ÉCHEC"
                                "pending" -> Color(0xFFD97706) to "EN ATTENTE"
                                "assigned" -> MaterialTheme.colorScheme.primary to "ASSIGNÉ"
                                "picked_up" -> MaterialTheme.colorScheme.primary to "RÉCUPÉRÉ"
                                "in_transit" -> MaterialTheme.colorScheme.primary to "EN COURS"
                                else -> MaterialTheme.colorScheme.primary to (livraison?.status?.uppercase() ?: "CHARGEMENT")
                            }
                            Box(modifier = Modifier.size(12.dp).background(couleur, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(libelle, fontWeight = FontWeight.Black, color = couleur, style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Commande #${livraison?.id}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Créée le ${formaterDateComplete(livraison?.created_at)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Carte des informations du livreur
                if (livraison?.driver != null) {
                    Text("LIVREUR ASSIGNÉ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        onClick = { livraison?.driver?.id?.let { lorsNavigationVersLivreur(it) } }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(livraison?.driver?.user?.name ?: "Livreur", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Voir le profil", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }

                // Preuves de livraison (Photo & Signature)
                if (livraison?.status == "delivered") {
                    val dateLivraison = livraison?.delivered_at ?: livraison?.statuses?.find { it.status == "delivered" }?.created_at
                    
                    Text("PREUVES DE LIVRAISON", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (livraison?.proof_photo != null) {
                                Text("Photo de preuve", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                AsyncImage(
                                    model = obtenirUrlImage(livraison?.proof_photo),
                                    contentDescription = "Photo de preuve",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            if (livraison?.signature != null) {
                                Text("Signature du destinataire", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .background(Color.White, RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val modeleSignature = obtenirUrlImage(livraison?.signature)
                                    
                                    AsyncImage(
                                        model = modeleSignature,
                                        contentDescription = "Signature",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit,
                                        onError = { erreur -> 
                                            android.util.Log.e("SignatureLoad", "Erreur lors du chargement de la signature : ${erreur.result.throwable.message}")
                                        }
                                    )
                                }
                            }
                            
                            Text("Livré le ${formaterDateComplete(dateLivraison)}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF059669), fontWeight = FontWeight.Bold)

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
                                Text("Télécharger le Bon de Livraison (PDF)")
                            }
                        }
                    }
                }

                // Détails du trajet
                Text("DÉTAILS DU PARCOURS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        LigneTrajet(Icons.Default.Outbound, "Expéditeur", livraison?.order?.sender_name ?: "N/A", livraison?.order?.sender_address ?: "")
                        Spacer(modifier = Modifier.height(16.dp))
                        LigneTrajet(Icons.Default.LocationOn, "Destinataire", livraison?.order?.recipient_name ?: "N/A", livraison?.order?.recipient_address ?: "")
                    }
                }

                // Historique des étapes
                Text("HISTORIQUE DES ÉTAPES", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        livraison?.statuses?.sortedByDescending { it.created_at }?.forEach { statut ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                                    Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                    Box(modifier = Modifier.width(2.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(statut.label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(formaterDateComplete(statut.created_at), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (!statut.note.isNullOrBlank()) {
                                        Text(statut.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
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
fun LigneTrajet(icone: androidx.compose.ui.graphics.vector.ImageVector, type: String, nom: String, adresse: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(icone, null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(nom, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(adresse, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
