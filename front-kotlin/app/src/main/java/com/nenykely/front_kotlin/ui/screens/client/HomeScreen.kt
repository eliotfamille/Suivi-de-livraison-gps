package com.nenykely.front_kotlin.ui.screens.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranAccueil(jeton: String, modeleDeVue: DeliveryViewModel, lorsNavigationVersLivraisons: () -> Unit, lorsNavigationVersLivraison: (Int, String) -> Unit) {
    val livraisons by modeleDeVue.livraisons.collectAsState()
    val estEnChargement by modeleDeVue.estEnChargement.collectAsState()
    var requeteRecherche by remember { mutableStateOf("") }
    val bleuPrimaire = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        modeleDeVue.recupererLivraisons(jeton)
    }

    val livraisonActive = livraisons.firstOrNull { it.status != "delivered" && it.status != "failed" }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Logistics Pro", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black, 
                        color = bleuPrimaire
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { espacement ->
        PullToRefreshBox(
            isRefreshing = estEnChargement,
            onRefresh = { modeleDeVue.recupererLivraisons(jeton) },
            modifier = Modifier.fillMaxSize().padding(espacement)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    // Section Recherche
                    Text(
                        text = "Suivre un colis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = requeteRecherche,
                        onValueChange = { requeteRecherche = it.replace("\n", "").replace("\r", "") },
                        placeholder = { Text("Entrez le numéro de suivi...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = { 
                                    // Extraction de l'ID du format FR-XXXX-X ou direct
                                    val id = if (requeteRecherche.contains("-")) {
                                        requeteRecherche.split("-").getOrNull(1)?.toIntOrNull()?.minus(8000)
                                    } else {
                                        requeteRecherche.replace(Regex("[^0-9]"), "").toIntOrNull()
                                    }
                                    
                                    if (id != null) {
                                        // On cherche le statut dans la liste locale pour décider de l'écran
                                        val existant = livraisons.find { it.id == id }
                                        lorsNavigationVersLivraison(id, existant?.status ?: "pending")
                                    }
                                },
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(40.dp)
                                    .background(bleuPrimaire, RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                item {
                    // Résumé de la livraison active
                    Text(
                        text = "En cours de livraison",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (livraisonActive != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(20.dp),
                            onClick = { lorsNavigationVersLivraison(livraisonActive.id, livraisonActive.status) }
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val libelleStatut = when(livraisonActive.status) {
                                                "pending" -> "EN ATTENTE"
                                                "assigned" -> "ASSIGNÉ"
                                                "picked_up" -> "RÉCUPÉRÉ"
                                                "in_transit" -> "EN TRANSIT"
                                                "delivered" -> "LIVRÉ"
                                                else -> livraisonActive.status.uppercase()
                                            }
                                            Text(libelleStatut, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                                    }
                                }
                                
                                Text(text = "Colis #FR-${livraisonActive.id}X", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                Text("LIVRAISON ESTIMÉE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
                                Text(livraisonActive.estimated_arrival ?: "Prochainement", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = bleuPrimaire)
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(modifier = Modifier.padding(40.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Aucune livraison en cours", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.fillMaxSize().alpha(0.1f), tint = bleuPrimaire)
                                
                                Box(modifier = Modifier.align(Alignment.Center)) {
                                    Box(modifier = Modifier.align(Alignment.TopCenter).offset(y = (-20).dp)) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(60.dp))
                                    }
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = CircleShape,
                                        color = bleuPrimaire,
                                        shadowElevation = 4.dp,
                                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                                    ) {
                                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(6.dp))
                                    }
                                }
                            }
                            
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("VOS LIVRAISONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.List, contentDescription = null, tint = bleuPrimaire, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Historique et suivi complet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                OutlinedButton(
                                    onClick = lorsNavigationVersLivraisons,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                ) {
                                    Text("Gérer mes colis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun BoutonOnglet(texte: String, estSelectionne: Boolean, modifier: Modifier = Modifier, lorsClic: () -> Unit) {
    Surface(
        onClick = lorsClic,
        color = if (estSelectionne) MaterialTheme.colorScheme.surface else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (estSelectionne) 2.dp else 0.dp,
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                text = texte,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (estSelectionne) FontWeight.Bold else FontWeight.Medium,
                color = if (estSelectionne) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
