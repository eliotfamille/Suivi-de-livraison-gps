package com.nenykely.front_kotlin.ui.screens.common

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nenykely.front_kotlin.viewmodel.AuthViewModel

@Composable
fun EcranInscription(modeleDeVue: AuthViewModel, lorsSuccesInscription: () -> Unit, lorsRetourConnexion: () -> Unit) {
    var nom by remember { mutableStateOf("") }
    var courriel by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var motDePasseVisible by remember { mutableStateOf(false) }
    var roleSelectionne by remember { mutableStateOf("client") }
    
    // Infos véhicule pour les livreurs
    var typeVehicule by remember { mutableStateOf("") }
    var modeleVehicule by remember { mutableStateOf("") }
    var plaqueVehicule by remember { mutableStateOf("") }
    
    val contexte = LocalContext.current
    val utilisateur by modeleDeVue.utilisateur.collectAsState()
    val estEnChargement by modeleDeVue.estEnChargement.collectAsState()
    val erreur by modeleDeVue.erreur.collectAsState()

    LaunchedEffect(utilisateur) {
        utilisateur?.let {
            Toast.makeText(contexte, "Inscription réussie : ${it.name} (${it.role ?: it.roles?.firstOrNull() ?: "Utilisateur"})", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Créer un compte",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Nom
                    Text("Nom complet", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = nom,
                        onValueChange = { nom = it.replace("\n", "").replace("\r", "") },
                        placeholder = { Text("Jean Dupont") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Email
                    Text("Adresse Email", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = courriel,
                        onValueChange = { courriel = it.replace("\n", "").replace("\r", "") },
                        placeholder = { Text("nom@exemple.com") },
                        leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Téléphone
                    Text("Téléphone (Optionnel)", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = telephone,
                        onValueChange = { telephone = it.replace("\n", "").replace("\r", "") },
                        placeholder = { Text("+261 34 00 000 00") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Mot de passe
                    Text("Mot de passe", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = motDePasse,
                        onValueChange = { motDePasse = it.replace("\n", "").replace("\r", "") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { motDePasseVisible = !motDePasseVisible }) {
                                Icon(
                                    if (motDePasseVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        visualTransformation = if (motDePasseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sélection du rôle
                    Text("Vous êtes un :", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = roleSelectionne == "client",
                            onClick = { roleSelectionne = "client" }
                        )
                        Text("Client", modifier = Modifier.padding(start = 4.dp))
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        RadioButton(
                            selected = roleSelectionne == "driver",
                            onClick = { roleSelectionne = "driver" }
                        )
                        Text("Livreur", modifier = Modifier.padding(start = 4.dp))
                    }

                    if (roleSelectionne == "driver") {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Informations du véhicule", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Type de véhicule", style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = typeVehicule,
                            onValueChange = { typeVehicule = it },
                            placeholder = { Text("Moto, Voiture, Camion...") },
                            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Modèle du véhicule", style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = modeleVehicule,
                            onValueChange = { modeleVehicule = it },
                            placeholder = { Text("Peugeot Partner, Honda CB...") },
                            leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Numéro d'immatriculation", style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = plaqueVehicule,
                            onValueChange = { plaqueVehicule = it },
                            placeholder = { Text("1234 TAB") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                    
                    if (erreur != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = erreur!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { 
                            modeleDeVue.sinscrire(
                                nom, courriel, motDePasse, telephone, roleSelectionne,
                                if (roleSelectionne == "driver") typeVehicule else null,
                                if (roleSelectionne == "driver") modeleVehicule else null,
                                if (roleSelectionne == "driver") plaqueVehicule else null,
                                lorsSucces = lorsSuccesInscription
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        enabled = !estEnChargement
                    ) {
                        if (estEnChargement) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("S'inscrire", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = lorsRetourConnexion,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Déjà un compte ? Se connecter", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
