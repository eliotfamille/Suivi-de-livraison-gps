package com.nenykely.front_kotlin.ui.screens.driver

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nenykely.front_kotlin.data.models.Delivery
import com.nenykely.front_kotlin.ui.screens.client.BoutonOnglet
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranLivraisonsLivreur(jeton: String, modeleDeVue: DeliveryViewModel, lorsClicLivraison: (Int) -> Unit) {
    val livraisons by modeleDeVue.livraisons.collectAsState()
    val estEnChargement by modeleDeVue.estEnChargement.collectAsState()
    val contexte = LocalContext.current
    var ongletSelectionne by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        modeleDeVue.recupererLivraisons(jeton)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mes Missions", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { espacement ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(espacement)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Onglets pour le Livreur
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    BoutonOnglet(
                        texte = "Disponibles", 
                        estSelectionne = ongletSelectionne == 0, 
                        modifier = Modifier.weight(1f),
                        lorsClic = { ongletSelectionne = 0 }
                    )
                    BoutonOnglet(
                        texte = "En cours", 
                        estSelectionne = ongletSelectionne == 1, 
                        modifier = Modifier.weight(1f),
                        lorsClic = { ongletSelectionne = 1 }
                    )
                }
            }

            val livraisonsFiltrees = if (ongletSelectionne == 0) {
                livraisons.filter { it.status == "pending" || it.status == "assigned" }
            } else {
                livraisons.filter { it.status == "picked_up" || it.status == "in_transit" }
            }

            if (estEnChargement && livraisonsFiltrees.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = estEnChargement,
                    onRefresh = { modeleDeVue.recupererLivraisons(jeton) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (livraisonsFiltrees.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            val messageVide = if (ongletSelectionne == 0) "Aucune mission disponible" else "Aucune mission en cours"
                            Text(messageVide, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(livraisonsFiltrees) { livraison ->
                                CarteLivraisonLivreur(
                                    livraison = livraison,
                                    lorsAcceptation = {
                                        modeleDeVue.accepterLivraison(jeton, livraison.id) {
                                            Toast.makeText(contexte, "Mission #${livraison.id} acceptée ! Nouveau statut: ASSIGNÉ", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    lorsClic = { lorsClicLivraison(livraison.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CarteLivraisonLivreur(livraison: Delivery, lorsAcceptation: () -> Unit, lorsClic: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = lorsClic
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Colis #${livraison.id}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                val infosStatut = when (livraison.status) {
                    "pending" -> Color(0xFFFEF3C7) to "EN ATTENTE"
                    "assigned" -> MaterialTheme.colorScheme.primaryContainer to "ASSIGNÉ"
                    "picked_up" -> Color(0xFFDBEAFE) to "RÉCUPÉRÉ"
                    "in_transit" -> Color(0xFFD1FAE5) to "EN TRANSIT"
                    else -> MaterialTheme.colorScheme.surfaceVariant to livraison.status.uppercase()
                }
                
                val couleurTexte = when (livraison.status) {
                    "pending" -> Color(0xFF92400E)
                    "assigned" -> MaterialTheme.colorScheme.onPrimaryContainer
                    "picked_up" -> Color(0xFF1E40AF)
                    "in_transit" -> Color(0xFF065F46)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    color = infosStatut.first,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = infosStatut.second,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = couleurTexte
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(livraison.order?.recipient_address ?: "Adresse inconnue", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (livraison.status == "pending") {
                Button(
                    onClick = lorsAcceptation,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Accepter la mission", color = MaterialTheme.colorScheme.onPrimary)
                }
            } else {
                val texteBouton = when(livraison.status) {
                    "assigned" -> "Démarrer la mission"
                    "picked_up", "in_transit" -> "Continuer la mission"
                    else -> "Voir les détails"
                }
                OutlinedButton(
                    onClick = lorsClic,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text(texteBouton)
                }
            }
        }
    }
}
