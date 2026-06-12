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
fun ProfileScreen(viewModel: AuthViewModel, onLogout: () -> Unit) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val primaryBlue = MaterialTheme.colorScheme.primary

    val getImageUrl: (String?) -> String? = { path ->
        if (path.isNullOrBlank()) null
        else if (path.startsWith("http") || path.startsWith("data:image")) path
        else {
            val cleanPath = path.removePrefix("/")
            val finalPath = if (cleanPath.startsWith("storage/")) cleanPath else "storage/$cleanPath"
            "${RetrofitClient.BASE_URL.removeSuffix("/")}/$finalPath"
        }
    }
    
    var showAddressDialog by remember { mutableStateOf<String?>(null) } // "domicile" or "bureau"
    var addressText by remember { mutableStateOf("") }
    var selectedGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var showMap by remember { mutableStateOf(false) }
    
    var showPersonalDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }

    var showVehicleDialog by remember { mutableStateOf(false) }
    var editVehicleType by remember { mutableStateOf("") }
    var editVehicleModel by remember { mutableStateOf("") }
    var editVehiclePlate by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            if (file != null) {
                viewModel.updateProfile(imageFile = file)
            }
        }
    }

    if (showMap) {
        AlertDialog(
            onDismissRequest = { showMap = false },
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
                                controller.setCenter(selectedGeoPoint ?: GeoPoint(-18.8792, 47.5079))
                                
                                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                                locationOverlay.enableMyLocation()
                                overlays.add(locationOverlay)

                                val overlay = MapEventsOverlay(object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                        selectedGeoPoint = p
                                        addressText = "Position choisie (${String.format("%.4f", p.latitude)})"
                                        invalidate()
                                        return true
                                    }
                                    override fun longPressHelper(p: GeoPoint): Boolean = false
                                })
                                overlays.add(overlay)
                            }
                        },
                        update = { mapView ->
                            mapView.overlays.removeAll { it is Marker }
                            selectedGeoPoint?.let {
                                val marker = Marker(mapView)
                                marker.position = it
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                mapView.overlays.add(marker)
                            }
                        }
                    )
                }
            },
            confirmButton = { Button(onClick = { showMap = false }) { Text("Valider") } }
        )
    }

    if (showPersonalDialog) {
        AlertDialog(
            onDismissRequest = { showPersonalDialog = false },
            title = { Text("Modifier mes infos") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it.replace("\n", "") },
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it.replace("\n", "") },
                        label = { Text("Téléphone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateProfile(name = editName, phone = editPhone)
                    showPersonalDialog = false
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { showPersonalDialog = false }) { Text("Annuler") } }
        )
    }

    if (showVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showVehicleDialog = false },
            title = { Text("Modifier le véhicule") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editVehicleType,
                        onValueChange = { editVehicleType = it },
                        label = { Text("Type de véhicule") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editVehicleModel,
                        onValueChange = { editVehicleModel = it },
                        label = { Text("Modèle") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editVehiclePlate,
                        onValueChange = { editVehiclePlate = it },
                        label = { Text("Immatriculation") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateProfile(
                        vehicle_type = editVehicleType,
                        vehicle_model = editVehicleModel,
                        vehicle_plate = editVehiclePlate
                    )
                    showVehicleDialog = false
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { showVehicleDialog = false }) { Text("Annuler") } }
        )
    }

    if (showAddressDialog != null) {
        AlertDialog(
            onDismissRequest = { showAddressDialog = null },
            title = { Text("Modifier l'adresse ${if (showAddressDialog == "domicile") "domicile" else "bureau"}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = addressText,
                        onValueChange = { addressText = it.replace("\n", "").replace("\r", "") },
                        label = { Text("Adresse") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showMap = true }) {
                                Icon(Icons.Default.Map, contentDescription = "Carte")
                            }
                        }
                    )
                    if (selectedGeoPoint != null) {
                        Text(
                            "Coordonnées : ${String.format("%.4f", selectedGeoPoint!!.latitude)}, ${String.format("%.4f", selectedGeoPoint!!.longitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (showAddressDialog == "domicile") {
                        viewModel.updateProfile(domicile = addressText, domicile_lat = selectedGeoPoint?.latitude, domicile_lng = selectedGeoPoint?.longitude)
                    } else {
                        viewModel.updateProfile(bureau = addressText, bureau_lat = selectedGeoPoint?.latitude, bureau_lng = selectedGeoPoint?.longitude)
                    }
                    showAddressDialog = null
                }) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddressDialog = null }) {
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
            isRefreshing = isLoading,
            onRefresh = { viewModel.fetchProfile() },
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
                // Profile Header
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        modifier = Modifier.size(100.dp).clickable { imagePickerLauncher.launch("image/*") },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (!user?.avatar.isNullOrEmpty()) {
                            AsyncImage(
                                model = getImageUrl(user?.avatar),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        modifier = Modifier.size(28.dp).clickable { imagePickerLauncher.launch("image/*") },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(6.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(user?.name ?: "Jean Dupont", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                if (user?.isDriver() == true && user?.driver != null) {
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
                            text = String.format("%.1f", user?.driver?.rating ?: 0.0),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " (${user?.driver?.rating_count ?: 0} avis)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                val isClient = user?.role?.equals("client", ignoreCase = true) == true || 
                               user?.roles?.any { it.equals("client", ignoreCase = true) } == true
                val roleLabel = if (user?.isDriver() == true) "Livreur" else if (isClient) "Client" else null
                
                Text(
                    text = roleLabel ?: "Chargement...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Sections
                ProfileSection(title = "INFORMATIONS PERSONNELLES", actionText = "Modifier", onAction = {
                    editName = user?.name ?: ""
                    editPhone = user?.phone ?: ""
                    showPersonalDialog = true
                }) {
                    ProfileItem(icon = Icons.Default.Email, label = "Email", value = user?.email ?: "jean.dupont@email.com")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileItem(icon = Icons.Default.Phone, label = "Téléphone", value = user?.phone ?: "+33 6 12 34 56 78")
                }
                
                ProfileSection(title = "ADRESSES ENREGISTRÉES") {
                    AddressItem(
                        icon = Icons.Default.Home, 
                        label = "Domicile", 
                        address = user?.domicile ?: "Non renseigné",
                        onClick = {
                            addressText = user?.domicile ?: ""
                            selectedGeoPoint = if (user?.domicile_lat != null) GeoPoint(user!!.domicile_lat!!, user!!.domicile_lng!!) else null
                            showAddressDialog = "domicile"
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    AddressItem(
                        icon = Icons.Default.Work, 
                        label = "Bureau", 
                        address = user?.bureau ?: "Non renseigné",
                        onClick = {
                            addressText = user?.bureau ?: ""
                            selectedGeoPoint = if (user?.bureau_lat != null) GeoPoint(user!!.bureau_lat!!, user!!.bureau_lng!!) else null
                            showAddressDialog = "bureau"
                        }
                    )
                }

                if (user?.isDriver() == true && user?.driver != null) {
                    ProfileSection(title = "DÉTAILS DU VÉHICULE", actionText = "Modifier", onAction = {
                        editVehicleType = user?.driver?.vehicle_type ?: ""
                        editVehicleModel = user?.driver?.vehicle_model ?: ""
                        editVehiclePlate = user?.driver?.vehicle_plate ?: ""
                        showVehicleDialog = true
                    }) {
                        ProfileItem(icon = Icons.Default.Category, label = "Type", value = user?.driver?.vehicle_type ?: "Non renseigné")
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileItem(icon = Icons.Default.LocalShipping, label = "Modèle", value = user?.driver?.vehicle_model ?: "Non renseigné")
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileItem(icon = Icons.Default.Badge, label = "Immatriculation", value = user?.driver?.vehicle_plate ?: "Non renseigné")
                    }
                }
                
                ProfileSection(title = "PRÉFÉRENCES") {
                    val isDarkMode by viewModel.isDarkMode.collectAsState()
                    PreferenceItem(icon = Icons.Default.Notifications, label = "Notifications Push", isChecked = true)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    PreferenceItem(icon = Icons.Default.AlternateEmail, label = "Emails de suivi", isChecked = false)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    PreferenceItem(
                        icon = Icons.Default.DarkMode, 
                        label = "Mode Sombre", 
                        isChecked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedButton(
                    onClick = { 
                        viewModel.logout()
                        onLogout()
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
fun ProfileSection(title: String, actionText: String? = null, onAction: () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
            if (actionText != null) {
                TextButton(onClick = onAction) {
                    Text(actionText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun AddressItem(icon: ImageVector, label: String, address: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PreferenceItem(icon: ImageVector, label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = MaterialTheme.colorScheme.primary)
        )
    }
}

private fun uriToFile(context: android.content.Context, uri: Uri): File? {
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    cursor.moveToFirst()
    val name = cursor.getString(nameIndex)
    cursor.close()
    
    val file = File(context.cacheDir, name)
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
    val outputStream = FileOutputStream(file)
    inputStream.copyTo(outputStream)
    inputStream.close()
    outputStream.close()
    return file
}
