package com.example.dontchaoda.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.example.dontchaoda.R
import com.example.dontchaoda.navigation.Screen
import com.example.dontchaoda.viewmodel.AppViewModel

@Composable
fun ARScanScreen(
    appViewModel: AppViewModel,
    navController: NavHostController
) {
    var scanQuality by remember { mutableStateOf(ScanQuality.GOOD) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasPermission) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Camera permission required")
        }
        return
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val hazardDefs = remember {
        listOf(
            HazardDef(HazardType.FIRE, "Fire", Risk.HIGH, R.drawable.fire_ref, 0.24),
            HazardDef(HazardType.FRAYED_WIRE, "Frayed wire", Risk.HIGH, R.drawable.frayed_wire_ref, 0.19),
            HazardDef(HazardType.FLAMMABLES, "Flammable materials", Risk.HIGH, R.drawable.flammables_ref, 0.24),
            HazardDef(HazardType.STORAGE_OBSTRUCTION, "Storage obstruction", Risk.MEDIUM, R.drawable.storage_obstruction_ref, 0.26)
        )
    }

    var hazardBoxes by remember { mutableStateOf<List<HazardBox>>(emptyList()) }
    var debugScores by remember { mutableStateOf<Map<HazardType, Double>>(emptyMap()) }

    val bestRaw = debugScores.maxByOrNull { it.value }
    val bestRawType = bestRaw?.key
    val bestRawScore = bestRaw?.value ?: 0.0

    val activeHazard = hazardBoxes.firstOrNull()
    var allDetectedHazards by remember { mutableStateOf<List<HazardBox>>(emptyList()) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        if (scanQuality != ScanQuality.GOOD) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                color = Color(0xD93B3127),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = when (scanQuality) {
                        ScanQuality.TOO_DARK -> "Too dark. Turn on more light."
                        ScanQuality.BLURRY -> "Scan blurry. Hold your phone steady."
                        else -> ""
                    },
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 84.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 320.dp),
                color = Color(0xCC10151D),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (activeHazard == null) "No hazard detected" else activeHazard.label,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Surface(
                modifier = Modifier.widthIn(max = 320.dp),
                color = Color(0xCC122131),
                shape = RoundedCornerShape(18.dp)
            ) {
                val text = buildString {
                    if (activeHazard == null) {
                        append("Active: None\n")
                    } else {
                        append("Active: ${activeHazard.label}\n")
                    }

                    if (bestRawType == null) {
                        append("Best raw: None\n")
                    } else {
                        val rawLabel = hazardDefs.firstOrNull { it.type == bestRawType }?.label ?: "Unknown"
                        val rawThreshold = hazardDefs.firstOrNull { it.type == bestRawType }?.threshold ?: 0.0
                        append("Best raw: $rawLabel\n")
                        append("Raw score: ${"%.3f".format(bestRawScore)} / Th: ${"%.2f".format(rawThreshold)}\n")
                    }

                    append("Fire: ${"%.2f".format(debugScores[HazardType.FIRE] ?: 0.0)}  ")
                    append("Wire: ${"%.2f".format(debugScores[HazardType.FRAYED_WIRE] ?: 0.0)}\n")
                    append("Flam: ${"%.2f".format(debugScores[HazardType.FLAMMABLES] ?: 0.0)}  ")
                    append("Obs: ${"%.2f".format(debugScores[HazardType.STORAGE_OBSTRUCTION] ?: 0.0)}")
                }

                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    LaunchedEffect(previewView) {
        startCamera(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            hazardDefs = hazardDefs,
            onHazardsUpdated = { updatedBoxes: List<HazardBox> ->
                hazardBoxes = updatedBoxes
                val newOnes = updatedBoxes.filter { hb: HazardBox ->
                    allDetectedHazards.none { it.label == hb.label }
                }
                if (newOnes.isNotEmpty()) {
                    allDetectedHazards = allDetectedHazards + newOnes
                }
            },
            onDebugScoresUpdated = { updatedScores: Map<HazardType, Double> ->
                debugScores = updatedScores
            },
            onQualityUpdated = { quality: ScanQuality ->
                scanQuality = quality
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = {
                appViewModel.currentScanResults.clear()
                appViewModel.currentScanResults.addAll(allDetectedHazards)

                val totalScore = allDetectedHazards.sumOf { it.risk.score }
                appViewModel.overallRiskScore = totalScore / allDetectedHazards.size.coerceAtLeast(1)
                appViewModel.overallRiskLevel = when {
                    appViewModel.overallRiskScore >= 70 -> "high"
                    appViewModel.overallRiskScore >= 40 -> "medium"
                    else -> "safe"
                }
                appViewModel.generateReminders()
                navController.navigate(Screen.ScanSummary.route)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Text("Finish Scan")
        }
    }
}
