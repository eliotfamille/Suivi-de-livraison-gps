package com.nenykely.front_kotlin.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nenykely.front_kotlin.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: AuthViewModel, onLogout: () -> Unit) {
    val user by viewModel.user.collectAsState()
    val primaryBlue = Color(0xFF0052CC)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Logistics Pro", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black, 
                        color = primaryBlue
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = Color(0xFF1A1C1E))
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Color(0xFFDBEAFE)) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(4.dp), tint = Color(0xFF3B82F6))
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FE))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Header
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = Color(0xFFE2E8F0)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(20.dp), tint = Color(0xFF94A3B8))
                }
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = primaryBlue,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(6.dp), tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(user?.name ?: "Jean Dupont", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Client Premium", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Sections
            ProfileSection(title = "INFORMATIONS PERSONNELLES") {
                ProfileItem(icon = Icons.Default.Email, label = "Email", value = user?.email ?: "jean.dupont@email.com")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF1F5F9))
                ProfileItem(icon = Icons.Default.Phone, label = "Téléphone", value = user?.phone ?: "+33 6 12 34 56 78")
            }
            
            ProfileSection(title = "ADRESSES ENREGISTRÉES", actionText = "Ajouter") {
                AddressItem(icon = Icons.Default.Home, label = "Domicile", address = "123 Rue de la Paix, 75002 Paris")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF1F5F9))
                AddressItem(icon = Icons.Default.Work, label = "Bureau", address = "45 Avenue des Champs-Élysées, 75008 Paris")
            }
            
            ProfileSection(title = "PRÉFÉRENCES") {
                PreferenceItem(icon = Icons.Default.Notifications, label = "Notifications Push", isChecked = true)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF1F5F9))
                PreferenceItem(icon = Icons.Default.AlternateEmail, label = "Emails de suivi", isChecked = false)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF1F5F9))
                PreferenceItem(icon = Icons.Default.DarkMode, label = "Mode Sombre", isChecked = false)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedButton(
                onClick = { 
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Déconnexion", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Version 2.4.0 (Build 108)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileSection(title: String, actionText: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
            if (actionText != null) {
                TextButton(onClick = {}) {
                    Text(actionText, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun ProfileItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFFEFF6FF)) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp), tint = Color(0xFF3B82F6))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(20.dp))
    }
}

@Composable
fun AddressItem(icon: ImageVector, label: String, address: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9)) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp), tint = Color(0xFF3B82F6))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Text(address, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
        }
        Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color(0xFF94A3B8))
    }
}

@Composable
fun PreferenceItem(icon: ImageVector, label: String, isChecked: Boolean) {
    var checked by remember { mutableStateOf(isChecked) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF1E293B), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Color(0xFF1E293B))
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB))
        )
    }
}
