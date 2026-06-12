package com.nenykely.front_kotlin.ui.screens.common

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nenykely.front_kotlin.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranMotDePasseOublie(
    modeleDeVue: AuthViewModel,
    lorsRetour: () -> Unit,
) {
    var courriel by remember { mutableStateOf("") }
    var nouveauMotDePasse by remember { mutableStateOf("") }

    val erreur by modeleDeVue.erreur.collectAsState()
    val chargement by modeleDeVue.estEnChargement.collectAsState()
    val contexte = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réinitialisation") },
                navigationIcon = {
                    IconButton(onClick = lorsRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Nouveau mot de passe",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Entrez votre adresse email et votre nouveau mot de passe pour mettre à jour votre compte.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = courriel,
                onValueChange = { courriel = it },
                label = { Text("Adresse Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = nouveauMotDePasse,
                onValueChange = { nouveauMotDePasse = it },
                label = { Text("Nouveau mot de passe") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (erreur != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(erreur!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (courriel.isNotBlank() && nouveauMotDePasse.isNotBlank()) {
                        modeleDeVue.reinitialiserMotDePasse(courriel, nouveauMotDePasse) {
                            Toast.makeText(contexte, "Mot de passe mis à jour !", Toast.LENGTH_LONG).show()
                            lorsRetour()
                        }
                    } else {
                        Toast.makeText(contexte, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !chargement
            ) {
                if (chargement) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Mettre à jour le mot de passe")
                }
            }
        }
    }
}
