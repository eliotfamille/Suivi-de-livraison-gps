package com.nenykely.front_kotlin.ui.screens.driver

import android.widget.Toast
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranProfilLivreur(jeton: String, livreurId: Int, modeleDeVue: DeliveryViewModel, lorsRetour: () -> Unit) {
    val livraison by modeleDeVue.livraisonActuelle.collectAsState()
    val livreur = livraison?.driver
    val estEnChargement by modeleDeVue.estEnChargement.collectAsState()
    val contexte = LocalContext.current
    
    var noteUtilisateur by remember { mutableStateOf(livraison?.rating ?: 0) }
    val bleuPrimaire = MaterialTheme.colorScheme.primary

    LaunchedEffect(livraison?.rating) {
        livraison?.rating?.let { noteUtilisateur = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Livreur", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = lorsRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { espacement ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(espacement)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // En-tête du profil livreur
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (livreur?.user?.avatar != null) {
                            AsyncImage(
                                model = livreur.user.avatar,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(livreur?.user?.name ?: "Chargement...", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (livreur?.rating != null && livreur.rating >= 4.5) "Livreur Senior" else "Livreur", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        val libelleStatut = when(livreur?.status) {
                            "available" -> "Disponible"
                            "busy" -> "En mission"
                            "offline" -> "Hors ligne"
                            else -> "Inconnu"
                        }
                        val couleurStatut = when(livreur?.status) {
                            "available" -> Color(0xFF10B981)
                            "busy" -> Color(0xFFF59E0B)
                            else -> Color(0xFF64748B)
                        }
                        Surface(
                            color = couleurStatut.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(couleurStatut, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(libelleStatut, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = couleurStatut)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Ligne des statistiques
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CarteStatistique(
                    modifier = Modifier.weight(1f), 
                    icone = Icons.Default.Inventory2, 
                    couleurIcone = Color(0xFF2563EB), 
                    valeur = (livreur?.total_deliveries ?: 0).toString(), 
                    libelle = "Livraisons"
                )
                CarteStatistique(
                    modifier = Modifier.weight(1f), 
                    icone = Icons.Default.Star, 
                    couleurIcone = Color(0xFFF59E0B), 
                    valeur = String.format(java.util.Locale.US, "%.1f", livreur?.rating ?: 0.0), 
                    libelle = "Note moyenne"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section de notation (Visible pour les clients après la livraison)
            if (livraison?.status == "delivered") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (livraison?.rating == null) "Noter ce livreur" else "Modifier votre note", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text("Comment s'est passée votre livraison ?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..5).forEach { index ->
                                IconButton(onClick = { noteUtilisateur = index }) {
                                    Icon(
                                        imageVector = if (index <= noteUtilisateur) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = if (index <= noteUtilisateur) Color(0xFFF59E0B) else Color(0xFFD1D5DB),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { 
                                modeleDeVue.noterLivraison(jeton, livraison!!.id, noteUtilisateur) {
                                    Toast.makeText(contexte, "Note mise à jour !", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = noteUtilisateur > 0 && !estEnChargement && noteUtilisateur != livraison?.rating,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (estEnChargement) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text(if (livraison?.rating == null) "Envoyer la note" else "Mettre à jour", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Détails du véhicule
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.padding(8.dp), tint = bleuPrimaire)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Détails du véhicule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    LigneDetailVehicule("Modèle", livreur?.vehicle_model ?: "Non renseigné")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    LigneDetailVehicule("Type", livreur?.vehicle_type ?: "Non renseigné")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Immatriculation", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            color = bleuPrimaire.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                livreur?.vehicle_plate ?: "N/A",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = bleuPrimaire,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CarteStatistique(modifier: Modifier = Modifier, icone: ImageVector, couleurIcone: Color, valeur: String, libelle: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = couleurIcone.copy(alpha = 0.1f)) {
                Icon(icone, contentDescription = null, modifier = Modifier.padding(10.dp), tint = couleurIcone)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(valeur, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(libelle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LigneDetailVehicule(libelle: String, valeur: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(libelle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valeur, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
