package com.nenykely.front_kotlin.ui.screens.driver

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nenykely.front_kotlin.viewmodel.DeliveryViewModel
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranSignature(modeleDeVue: DeliveryViewModel, lorsSignatureCapturee: (String?) -> Unit, lorsRetour: () -> Unit) {
    val traces = remember { mutableStateListOf<androidx.compose.ui.graphics.Path>() }
    var traceActuelle by remember { mutableStateOf<androidx.compose.ui.graphics.Path?>(null) }
    val densite = LocalDensity.current
    val estEnChargement by modeleDeVue.estEnChargement.collectAsState()
    val contexte = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Signature Électronique", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = lorsRetour, enabled = !estEnChargement) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { espacement ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(espacement)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Veuillez signer ci-dessous",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp)
            ) {
                val largeurBoite = constraints.maxWidth
                val hauteurBoite = constraints.maxHeight

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                if (estEnChargement) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { position ->
                                        traceActuelle = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(position.x, position.y)
                                        }
                                    },
                                    onDrag = { changement, _ ->
                                        traceActuelle?.lineTo(changement.position.x, changement.position.y)
                                        val t = traceActuelle
                                        traceActuelle = null
                                        traceActuelle = t
                                    },
                                    onDragEnd = {
                                        traceActuelle?.let { traces.add(it) }
                                        traceActuelle = null
                                    }
                                )
                            }
                    ) {
                        traces.forEach { trace ->
                            drawPath(
                                path = trace,
                                color = Color.Black,
                                style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                        traceActuelle?.let { trace ->
                            drawPath(
                                path = trace,
                                color = Color.Black,
                                style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { traces.clear() },
                        enabled = !estEnChargement,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("Effacer")
                    }

                    Button(
                        onClick = {
                            if (traces.isNotEmpty()) {
                                // Capture à la taille réelle pour une fidélité maximale
                                val imageBitmap = Bitmap.createBitmap(largeurBoite, hauteurBoite, Bitmap.Config.ARGB_8888)
                                val canevas = android.graphics.Canvas(imageBitmap)
                                canevas.drawColor(android.graphics.Color.WHITE)
                                
                                val peinture = android.graphics.Paint().apply {
                                    color = android.graphics.Color.BLACK
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeWidth = 12f
                                    strokeCap = android.graphics.Paint.Cap.ROUND
                                    strokeJoin = android.graphics.Paint.Join.ROUND
                                    isAntiAlias = true
                                }
                                
                                traces.forEach { trace ->
                                    canevas.drawPath(trace.asAndroidPath(), peinture)
                                }
                                
                                val fluxSortie = ByteArrayOutputStream()
                                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fluxSortie)
                                val signatureBase64 = "data:image/jpeg;base64," + Base64.encodeToString(fluxSortie.toByteArray(), Base64.NO_WRAP)
                                
                                lorsSignatureCapturee(signatureBase64)
                                lorsRetour()
                            }
                        },
                        enabled = !estEnChargement && traces.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (estEnChargement) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Confirmer")
                        }
                    }
                }
            }
        }
    }
}
