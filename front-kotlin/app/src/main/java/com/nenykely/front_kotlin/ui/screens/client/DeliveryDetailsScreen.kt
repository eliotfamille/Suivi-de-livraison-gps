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
fun DeliveryDetailsScreen(token: String, deliveryId: Int, viewModel: DeliveryViewModel, onBack: () -> Unit, onNavigateToDriver: (Int) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val delivery by viewModel.currentDelivery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val timeFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy à HH:mm") }
    val formatFullDate: (String?) -> String = { isoString ->
        try {
            if (isoString.isNullOrBlank()) "N/A"
            else ZonedDateTime.parse(isoString).format(timeFormatter)
        } catch (e: Exception) {
            isoString?.take(16)?.replace("T", " ") ?: "N/A"
        }
    }

    val getImageUrl: (String?) -> Any? = { path ->
        if (path.isNullOrBlank()) null
        else {
            val normalizedPath = path.replace("\\/", "/")
            if (normalizedPath.startsWith("http")) normalizedPath
            else if (normalizedPath.startsWith("data:image")) {
                try {
                    val base64Data = normalizedPath.substringAfter("base64,")
                    val imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } catch (e: Exception) {
                    normalizedPath
                }
            } else {
                val cleanPath = normalizedPath.removePrefix("/")
                val finalPath = if (cleanPath.startsWith("storage/")) cleanPath else "storage/$cleanPath"
                "${RetrofitClient.BASE_URL.removeSuffix("/")}/$finalPath"
            }
        }
    }

    LaunchedEffect(deliveryId) {
        viewModel.fetchTracking(token, deliveryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails de la livraison", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (isLoading && delivery == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (color, label) = when(delivery?.status) {
                                "delivered" -> Color(0xFF059669) to "LIVRÉ"
                                "failed" -> MaterialTheme.colorScheme.error to "ÉCHEC"
                                else -> MaterialTheme.colorScheme.primary to (delivery?.status?.uppercase() ?: "CHARGEMENT")
                            }
                            Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, fontWeight = FontWeight.Black, color = color, style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Commande #${delivery?.id}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Créée le ${formatFullDate(delivery?.created_at)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Driver Info Card
                if (delivery?.driver != null) {
                    Text("LIVREUR ASSIGNÉ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        onClick = { delivery?.driver?.id?.let { onNavigateToDriver(it) } }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(delivery?.driver?.user?.name ?: "Livreur", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Voir le profil", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }

                // Proof of Delivery (Photo & Signature)
                if (delivery?.status == "delivered") {
                    val deliveredAtDate = delivery?.delivered_at ?: delivery?.statuses?.find { it.status == "delivered" }?.created_at
                    
                    Text("PREUVES DE LIVRAISON", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (delivery?.proof_photo != null) {
                                Text("Photo de preuve", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                AsyncImage(
                                    model = getImageUrl(delivery?.proof_photo),
                                    contentDescription = "Photo de preuve",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            if (delivery?.signature != null) {
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
                                    val signatureModel = getImageUrl(delivery?.signature)
                                    
                                    AsyncImage(
                                        model = signatureModel,
                                        contentDescription = "Signature",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit,
                                        onError = { error -> 
                                            android.util.Log.e("SignatureLoad", "Error loading signature: ${error.result.throwable.message}")
                                        }
                                    )
                                }
                            }
                            
                            Text("Livré le ${formatFullDate(deliveredAtDate)}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF059669), fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                            onClick = {
                                val fileName = "Bon_Livraison_${delivery?.order?.order_number ?: deliveryId}.pdf"
                                viewModel.downloadReceipt(context, token, deliveryId, fileName)
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

                // Journey Details
                Text("DÉTAILS DU PARCOURS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        JourneyRow(Icons.Default.Outbound, "Expéditeur", delivery?.order?.sender_name ?: "N/A", delivery?.order?.sender_address ?: "")
                        Spacer(modifier = Modifier.height(16.dp))
                        JourneyRow(Icons.Default.LocationOn, "Destinataire", delivery?.order?.recipient_name ?: "N/A", delivery?.order?.recipient_address ?: "")
                    }
                }

                // Timeline
                Text("HISTORIQUE DES ÉTAPES", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        delivery?.statuses?.sortedByDescending { it.created_at }?.forEach { status ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                                    Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                    Box(modifier = Modifier.width(2.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(status.label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(formatFullDate(status.created_at), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (!status.note.isNullOrBlank()) {
                                        Text(status.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun JourneyRow(icon: androidx.compose.ui.graphics.vector.ImageVector, type: String, name: String, address: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(icon, null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
