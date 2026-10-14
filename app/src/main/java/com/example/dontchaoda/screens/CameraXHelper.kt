package com.example.dontchaoda.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Rect
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.lifecycle.LifecycleOwner
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val TAG = "CameraXHelper"
private val cameraExecutor = Executors.newSingleThreadExecutor()

private data class FrayedWireTemplate(
    val edgeDensity: Double,
    val copperRatio: Double,
    val darkRatio: Double,
    val skinnyEdgeRatio: Double,
    val diagonalEdgeRatio: Double
)

private data class FlammablesTemplate(
    val yellowRatio: Double,
    val blackRatio: Double,
    val whiteRatio: Double,
    val strongEdgeRatio: Double,
    val centerDarkRatio: Double
)

private data class StorageTemplate(
    val edgeRatio: Double,
    val darkRatio: Double,
    val lowSatGrayRatio: Double,
    val occupiedColumnsRatio: Double,
    val lowerHalfMassRatio: Double
)

private val frayedWireTemplateCache = mutableMapOf<Int, FrayedWireTemplate>()
private val flammablesTemplateCache = mutableMapOf<Int, FlammablesTemplate>()
private val storageTemplateCache = mutableMapOf<Int, StorageTemplate>()

private val defaultWindowFractions = listOf(0.35f, 0.50f, 0.65f)

fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    hazardDefs: List<HazardDef>,
    onHazardsUpdated: (List<HazardBox>) -> Unit,
    onDebugScoresUpdated: (Map<HazardType, Double>) -> Unit,
    onQualityUpdated: (ScanQuality) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also { p ->
                p.surfaceProvider = previewView.surfaceProvider
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            preloadTemplates(context, hazardDefs)

            val streaks = hazardDefs.associate { it.type to 0 }.toMutableMap()
            val framesNeededToShow = 2

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val pvW = previewView.width.toFloat()
                    val pvH = previewView.height.toFloat()
                    if (pvW <= 0f || pvH <= 0f) return@setAnalyzer

                    val frameBitmap = imageProxy.toBitmap()
                    val small = frameBitmap.scale(256, 256, true)

                    val fireScore = detectFire(small)
                    val storageTemplate = buildStorageTemplate(small)
                    val storageDominanceHint = estimateStorageDominance(storageTemplate)

                    val flammablesScore = detectFlammables(small, hazardDefs, fireScore, storageDominanceHint)
                    val obstructionScore = detectStorageObstruction(
                        bitmap = small,
                        hazardDefs = hazardDefs,
                        fireScore = fireScore,
                        flammablesScore = flammablesScore
                    )
                    val frayedWireScore = detectFrayedWire(
                        bitmap = small,
                        hazardDefs = hazardDefs,
                        fireScore = fireScore,
                        flammablesScore = flammablesScore,
                        obstructionScore = obstructionScore
                    )

                    val adjustedFire = fireScore
                    var adjustedWire = frayedWireScore
                    var adjustedFlam = flammablesScore
                    var adjustedObs = obstructionScore

                    if (adjustedFire >= 0.30) {
                        adjustedWire *= 0.10
                        adjustedFlam *= 0.45
                        adjustedObs *= 0.20
                    }

                    if (adjustedFlam >= 0.26) {
                        adjustedWire *= 0.08
                        adjustedObs *= 0.12
                    }

                    if (adjustedObs >= 0.26) {
                        adjustedWire *= 0.10
                    }

                    val scores = mapOf(
                        HazardType.FIRE to adjustedFire.coerceIn(0.0, 1.0),
                        HazardType.FRAYED_WIRE to adjustedWire.coerceIn(0.0, 1.0),
                        HazardType.FLAMMABLES to adjustedFlam.coerceIn(0.0, 1.0),
                        HazardType.STORAGE_OBSTRUCTION to adjustedObs.coerceIn(0.0, 1.0)
                    )

                    onDebugScoresUpdated(scores)

                    hazardDefs.forEach { def ->
                        val score = scores[def.type] ?: 0.0
                        val meets = score >= def.threshold
                        streaks[def.type] = if (meets) (streaks[def.type] ?: 0) + 1 else 0
                    }

                    val visibleDefs = hazardDefs
                        .filter { (streaks[it.type] ?: 0) >= framesNeededToShow }
                        .filter { (scores[it.type] ?: 0.0) >= it.threshold }
                        .sortedByDescending { scores[it.type] ?: 0.0 }
                        .take(3)

                    val boxes = visibleDefs.map { def ->
                        val score = scores[def.type] ?: 0.0
                        HazardBox(
                            label = "${def.label} (${String.format(Locale.US, "%.2f", score)})",
                            risk = def.risk,
                            rect = centerBox(pvW, pvH)
                        )
                    }

                    onHazardsUpdated(boxes)

                    val quality = checkScanQuality(small)
                    onQualityUpdated(quality)
                } catch (t: Throwable) {
                    Log.e(TAG, "Analyzer error", t)
                    onHazardsUpdated(emptyList())
                    onDebugScoresUpdated(emptyMap())
                } finally {
                    imageProxy.close()
                }
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )

            Log.d(TAG, "Camera bound successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Bind failed", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun preloadTemplates(context: Context, hazardDefs: List<HazardDef>) {
    hazardDefs.forEach { def ->
        val bmp = BitmapFactory.decodeResource(context.resources, def.templateResId) ?: return@forEach
        val small = bmp.scale(256, 256, true)

        when (def.type) {
            HazardType.FRAYED_WIRE -> {
                if (!frayedWireTemplateCache.containsKey(def.templateResId)) {
                    frayedWireTemplateCache[def.templateResId] = buildFrayedWireTemplate(small)
                }
            }
            HazardType.FLAMMABLES -> {
                if (!flammablesTemplateCache.containsKey(def.templateResId)) {
                    flammablesTemplateCache[def.templateResId] = buildFlammablesTemplate(small)
                }
            }
            HazardType.STORAGE_OBSTRUCTION -> {
                if (!storageTemplateCache.containsKey(def.templateResId)) {
                    storageTemplateCache[def.templateResId] = buildStorageTemplate(small)
                }
            }
            HazardType.FIRE -> Unit
        }
    }
}

private fun detectFire(bitmap: Bitmap): Double {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    var fireLike = 0
    var veryHot = 0
    var brightWarm = 0
    var total = 0

    val hsv = FloatArray(3)

    for (i in pixels.indices step 2) {
        val c = pixels[i]
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF

        android.graphics.Color.RGBToHSV(r, g, b, hsv)
        val hue = hsv[0]
        val sat = hsv[1]
        val value = hsv[2]

        val isFireHue = hue in 5f..58f
        val isWarm = r > g && g >= b
        val isBrightEnough = value > 0.35f
        val isSatEnough = sat > 0.42f
        val isVeryHot = hue in 8f..45f && sat > 0.55f && value > 0.55f

        if (isFireHue && isWarm && isBrightEnough && isSatEnough) fireLike++
        if (isVeryHot) veryHot++
        if (isWarm && value > 0.45f) brightWarm++

        total++
    }

    if (total == 0) return 0.0

    val fireRatio = fireLike.toDouble() / total
    val hotRatio = veryHot.toDouble() / total
    val warmRatio = brightWarm.toDouble() / total

    return ((fireRatio * 0.50) + (hotRatio * 0.35) + (warmRatio * 0.15)).coerceIn(0.0, 1.0)
}

private fun detectFrayedWire(
    bitmap: Bitmap,
    hazardDefs: List<HazardDef>,
    fireScore: Double,
    flammablesScore: Double,
    obstructionScore: Double
): Double {
    val template = hazardDefs
        .firstOrNull { it.type == HazardType.FRAYED_WIRE }
        ?.let { frayedWireTemplateCache[it.templateResId] }
        ?: return 0.0

    val fullFrameResult = scoreFrayedWireRegion(buildFrayedWireTemplate(bitmap), template)
    val bestWindowScore = findBestWindowScore(
        bitmap = bitmap,
        windowFractions = listOf(0.25f, 0.35f, 0.45f),
        scorer = { window ->
            scoreFrayedWireRegion(buildFrayedWireTemplate(window), template)
        }
    )

    var score = max(fullFrameResult, bestWindowScore)

    if (fireScore > 0.12) score *= 0.08
    if (flammablesScore > 0.12) score *= 0.06
    if (obstructionScore > 0.12) score *= 0.08

    return score.coerceIn(0.0, 1.0)
}

private fun scoreFrayedWireRegion(
    current: FrayedWireTemplate,
    template: FrayedWireTemplate
): Double {
    val edgeSim = 1.0 - abs(current.edgeDensity - template.edgeDensity)
    val copperSim = 1.0 - abs(current.copperRatio - template.copperRatio)
    val darkSim = 1.0 - abs(current.darkRatio - template.darkRatio)
    val skinnySim = 1.0 - abs(current.skinnyEdgeRatio - template.skinnyEdgeRatio)
    val diagSim = 1.0 - abs(current.diagonalEdgeRatio - template.diagonalEdgeRatio)

    val enoughCopper = current.copperRatio >= 0.008
    val enoughDark = current.darkRatio >= 0.08
    val enoughSkinny = current.skinnyEdgeRatio >= 0.05
    val diagPresence = current.diagonalEdgeRatio >= 0.04
    val notTooCluttered = current.edgeDensity <= 0.35

    if (!(enoughCopper && enoughDark && enoughSkinny && notTooCluttered)) {
        return 0.0
    }

    var score = (
        edgeSim * 0.06 +
            copperSim * 0.38 +
            darkSim * 0.14 +
            skinnySim * 0.30 +
            diagSim * 0.12
        )

    if (diagPresence) score += 0.08
    if (current.copperRatio >= 0.015) score += 0.10

    return score.coerceIn(0.0, 1.0)
}

private fun buildFrayedWireTemplate(bitmap: Bitmap): FrayedWireTemplate {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    var edges = 0
    var copper = 0
    var dark = 0
    var skinnyEdges = 0
    var diagonalEdges = 0
    var total = 0

    val hsv = FloatArray(3)

    for (y in 1 until h - 1 step 2) {
        for (x in 1 until w - 1 step 2) {
            val idx = y * w + x
            val c = pixels[idx]
            val gray = rgbToGray(c)

            val diffX = abs(gray - rgbToGray(pixels[idx + 1])) + abs(gray - rgbToGray(pixels[idx - 1]))
            val diffY = abs(gray - rgbToGray(pixels[idx + w])) + abs(gray - rgbToGray(pixels[idx - w]))
            val diff = diffX + diffY

            if (diff > 55) edges++

            val horizontalThin = diffX > 70 && diffY < 45
            val verticalThin = diffY > 70 && diffX < 45
            if (horizontalThin || verticalThin) skinnyEdges++

            val diffDiag1 = abs(gray - rgbToGray(pixels[idx + w + 1])) + abs(gray - rgbToGray(pixels[idx - w - 1]))
            val diffDiag2 = abs(gray - rgbToGray(pixels[idx + w - 1])) + abs(gray - rgbToGray(pixels[idx - w + 1]))
            if (diffDiag1 > 65 || diffDiag2 > 65) diagonalEdges++

            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF

            android.graphics.Color.RGBToHSV(r, g, b, hsv)
            val hue = hsv[0]
            val sat = hsv[1]
            val value = hsv[2]

            val isCopper = hue in 10f..45f && sat in 0.20f..0.90f && value in 0.15f..0.85f
            val isExposedSilver = sat < 0.20f && value > 0.45f && (horizontalThin || verticalThin)
            val isDark = value < 0.32f

            if (isCopper || isExposedSilver) copper++
            if (isDark) dark++

            total++
        }
    }

    if (total == 0) {
        return FrayedWireTemplate(0.0, 0.0, 0.0, 0.0, 0.0)
    }

    return FrayedWireTemplate(
        edgeDensity = edges.toDouble() / total,
        copperRatio = copper.toDouble() / total,
        darkRatio = dark.toDouble() / total,
        skinnyEdgeRatio = skinnyEdges.toDouble() / total,
        diagonalEdgeRatio = diagonalEdges.toDouble() / total
    )
}

private fun detectFlammables(
    bitmap: Bitmap,
    hazardDefs: List<HazardDef>,
    fireScore: Double,
    storageDominanceHint: Double
): Double {
    val template = hazardDefs
        .firstOrNull { it.type == HazardType.FLAMMABLES }
        ?.let { flammablesTemplateCache[it.templateResId] }
        ?: return 0.0

    val fullFrameScore = scoreFlammablesRegion(buildFlammablesTemplate(bitmap), template)
    val bestWindowScore = findBestWindowScore(
        bitmap = bitmap,
        windowFractions = defaultWindowFractions,
        scorer = { window ->
            scoreFlammablesRegion(buildFlammablesTemplate(window), template)
        }
    )

    var score = max(fullFrameScore * 0.70, bestWindowScore)
    if (fireScore > 0.50) {
        score *= 0.82
    }
    if (storageDominanceHint > 0.50 && score < 0.55) {
        score *= 0.72
    }
    if (storageDominanceHint > 0.65 && score < 0.45) {
        score *= 0.56
    }

    return score.coerceIn(0.0, 1.0)
}

private fun scoreFlammablesRegion(
    current: FlammablesTemplate,
    template: FlammablesTemplate
): Double {
    val yellowSim = 1.0 - abs(current.yellowRatio - template.yellowRatio)
    val blackSim = 1.0 - abs(current.blackRatio - template.blackRatio)
    val whiteSim = 1.0 - abs(current.whiteRatio - template.whiteRatio)
    val edgeSim = 1.0 - abs(current.strongEdgeRatio - template.strongEdgeRatio)
    val centerDarkSim = 1.0 - abs(current.centerDarkRatio - template.centerDarkRatio)

    val yellowPresenceBoost = if (current.yellowRatio >= 0.12) 0.12 else 0.0
    val blackPresenceBoost = if (current.blackRatio >= 0.08) 0.10 else 0.0
    val centerDarkBoost = if (current.centerDarkRatio >= 0.14) 0.08 else 0.0
    val signPatternBoost =
        if (
            current.yellowRatio >= 0.18 &&
            current.blackRatio >= 0.06 &&
            current.centerDarkRatio >= 0.12
        ) 0.22 else 0.0

    val hasEnoughYellow = current.yellowRatio >= 0.025
    val hasEnoughBlack = current.blackRatio >= 0.02
    val hasEnoughEdges = current.strongEdgeRatio >= 0.008

    if (!(hasEnoughYellow && hasEnoughBlack && hasEnoughEdges)) {
        return 0.0
    }

    return (
        (yellowSim * 0.30) +
            (blackSim * 0.23) +
            (whiteSim * 0.08) +
            (edgeSim * 0.15) +
            (centerDarkSim * 0.24) +
            yellowPresenceBoost +
            blackPresenceBoost +
            centerDarkBoost +
            signPatternBoost
        ).coerceIn(0.0, 1.0)
}

private fun buildFlammablesTemplate(bitmap: Bitmap): FlammablesTemplate {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    var yellow = 0
    var black = 0
    var white = 0
    var edges = 0
    var centerDark = 0
    var total = 0
    var centerTotal = 0

    val hsv = FloatArray(3)

    val cx1 = (w * 0.25f).toInt()
    val cx2 = (w * 0.75f).toInt()
    val cy1 = (h * 0.20f).toInt()
    val cy2 = (h * 0.80f).toInt()

    for (y in 1 until h - 1 step 2) {
        for (x in 1 until w - 1 step 2) {
            val idx = y * w + x
            val c = pixels[idx]
            val right = pixels[idx + 1]
            val down = pixels[idx + w]

            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF

            android.graphics.Color.RGBToHSV(r, g, b, hsv)
            val hue = hsv[0]
            val sat = hsv[1]
            val value = hsv[2]

            val gray = rgbToGray(c)
            val grayRight = rgbToGray(right)
            val grayDown = rgbToGray(down)
            val diff = abs(gray - grayRight) + abs(gray - grayDown)

            val isYellow = hue in 35f..70f && sat > 0.45f && value > 0.45f
            val isBlack = value < 0.18f
            val isWhite = sat < 0.10f && value > 0.82f

            if (isYellow) yellow++
            if (isBlack) black++
            if (isWhite) white++
            if (diff > 70) edges++

            if (x in cx1..cx2 && y in cy1..cy2) {
                centerTotal++
                if (isBlack || value < 0.28f) {
                    centerDark++
                }
            }

            total++
        }
    }

    if (total == 0 || centerTotal == 0) {
        return FlammablesTemplate(0.0, 0.0, 0.0, 0.0, 0.0)
    }

    return FlammablesTemplate(
        yellowRatio = yellow.toDouble() / total,
        blackRatio = black.toDouble() / total,
        whiteRatio = white.toDouble() / total,
        strongEdgeRatio = edges.toDouble() / total,
        centerDarkRatio = centerDark.toDouble() / centerTotal
    )
}

private fun detectStorageObstruction(
    bitmap: Bitmap,
    hazardDefs: List<HazardDef>,
    fireScore: Double,
    flammablesScore: Double
): Double {
    val template = hazardDefs
        .firstOrNull { it.type == HazardType.STORAGE_OBSTRUCTION }
        ?.let { storageTemplateCache[it.templateResId] }
        ?: return 0.0

    val fullFrameScore = scoreStorageRegion(buildStorageTemplate(bitmap), template)
    val bestWindowScore = findBestWindowScore(
        bitmap = bitmap,
        windowFractions = listOf(0.42f, 0.58f, 0.74f),
        verticalStartFraction = 0.18f,
        scorer = { window ->
            scoreStorageRegion(buildStorageTemplate(window), template)
        }
    )

    var score = max(fullFrameScore * 0.74, bestWindowScore)

    if (fireScore > 0.55) score *= 0.82
    if (flammablesScore > 0.58) score *= 0.78

    return score.coerceIn(0.0, 1.0)
}

private fun scoreStorageRegion(
    current: StorageTemplate,
    template: StorageTemplate
): Double {
    val edgeSim = 1.0 - abs(current.edgeRatio - template.edgeRatio)
    val colorSim = 1.0 - abs(current.lowSatGrayRatio - template.lowSatGrayRatio)
    val massSim = 1.0 - abs(current.lowerHalfMassRatio - template.lowerHalfMassRatio)
    val occupiedSim = 1.0 - abs(current.occupiedColumnsRatio - template.occupiedColumnsRatio)

    val isCluttered = current.edgeRatio >= 0.03
    val hasLargeMass = current.lowerHalfMassRatio >= 0.22
    val isSpreadOut = current.occupiedColumnsRatio >= 0.30

    if (!(isCluttered && hasLargeMass)) return 0.0

    val score = (edgeSim * 0.24) + (colorSim * 0.23) + (massSim * 0.31) + (occupiedSim * 0.22)
    return (if (isSpreadOut) score + 0.12 else score).coerceIn(0.0, 1.0)
}

private fun estimateStorageDominance(current: StorageTemplate): Double {
    val clutter = ((current.edgeRatio - 0.03) / 0.12).coerceIn(0.0, 1.0)
    val spread = ((current.occupiedColumnsRatio - 0.30) / 0.45).coerceIn(0.0, 1.0)
    val lowerMass = ((current.lowerHalfMassRatio - 0.22) / 0.45).coerceIn(0.0, 1.0)
    val neutralColor = ((current.lowSatGrayRatio - 0.18) / 0.45).coerceIn(0.0, 1.0)

    return (
        clutter * 0.24 +
            spread * 0.26 +
            lowerMass * 0.32 +
            neutralColor * 0.18
        ).coerceIn(0.0, 1.0)
}

private fun buildStorageTemplate(bitmap: Bitmap): StorageTemplate {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    // Focus on the bottom 80% of the screen where rubbish piles up
    val startX = (w * 0.05f).toInt()
    val endX = (w * 0.95f).toInt()
    val startY = (h * 0.20f).toInt()
    val endY = (h * 0.98f).toInt()
    val lowerHalfY = (h * 0.50f).toInt()

    var edges = 0
    var dark = 0
    var trashColor = 0 // Broadened from "gray" to include white bags/cardboard
    var lowerMass = 0
    var total = 0
    var lowerTotal = 0

    val occupiedColumns = BooleanArray(max(1, endX - startX))
    val hsv = FloatArray(3)

    for (y in startY until endY step 2) {
        for (x in startX until endX - 1 step 2) {
            val idx = y * w + x
            val c = pixels[idx]
            val gray = rgbToGray(c)

            val diffX = abs(gray - rgbToGray(pixels[idx + 1]))
            val diffY = abs(gray - rgbToGray(pixels[min(idx + w, pixels.lastIndex)]))
            val diff = diffX + diffY

            // RUBBISH PILES create lots of "cluttered" edges
            if (diff > 50) {
                edges++
                occupiedColumns[(x - startX).coerceIn(0, occupiedColumns.lastIndex)] = true
            }

            android.graphics.Color.RGBToHSV((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF, hsv)
            val hue = hsv[0]
            val sat = hsv[1]
            val value = hsv[2]

            // BROADENED COLOR CHECK:
            // 1. Includes white/clear plastic bags (low saturation, high brightness)
            // 2. Includes cardboard boxes (brownish hues, mid-saturation)
            val isWhiteBag = sat < 0.25f && value > 0.60f
            val isCardboard = hue in 20f..50f && sat in 0.20f..0.60f && value > 0.30f
            val isGray = sat < 0.35f && value in 0.20f..0.85f

            if (isWhiteBag || isCardboard || isGray) trashColor++
            if (value < 0.40f) dark++

            if (y >= lowerHalfY) {
                lowerTotal++
                // Rubbish is "dense" in the bottom half
                if (diff > 45 || value < 0.70f) lowerMass++
            }
            total++
        }
    }

    return StorageTemplate(
        edgeRatio = edges.toDouble() / total,
        darkRatio = dark.toDouble() / total,
        lowSatGrayRatio = trashColor.toDouble() / total,
        occupiedColumnsRatio = occupiedColumns.count { it }.toDouble() / occupiedColumns.size,
        lowerHalfMassRatio = if (lowerTotal > 0) lowerMass.toDouble() / lowerTotal else 0.0
    )
}

private fun centerBox(previewW: Float, previewH: Float): Rect {
    val boxW = previewW * 0.70f
    val boxH = previewH * 0.45f
    val left = (previewW - boxW) / 2f
    val top = (previewH - boxH) / 2f
    return Rect(left, top, left + boxW, top + boxH)
}

private fun rgbToGray(color: Int): Int {
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
}

enum class ScanQuality {
    GOOD, TOO_DARK, BLURRY
}

private fun checkScanQuality(bitmap: Bitmap): ScanQuality {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    var totalBrightness = 0L
    var edgeScore = 0L
    val step = 4 // Sampling every 4th pixel for performance

    for (y in 1 until h - 1 step step) {
        for (x in 1 until w - 1 step step) {
            val c = pixels[y * w + x]
            val gray = ( ( (c shr 16) and 0xFF) * 0.299 + ((c shr 8) and 0xFF) * 0.587 + (c and 0xFF) * 0.114 ).toInt()
            totalBrightness += gray

            // Simple edge detection: difference with neighbor
            val rightGray = rgbToGray(pixels[y * w + (x + 1)])
            edgeScore += abs(gray - rightGray)
        }
    }

    val sampleCount = (w / step) * (h / step)
    val avgBrightness = totalBrightness / sampleCount
    val avgEdge = edgeScore / sampleCount

    return when {
        avgBrightness < 50 -> ScanQuality.TOO_DARK
        avgEdge < 5 -> ScanQuality.BLURRY // Adjust this threshold based on testing
        else -> ScanQuality.GOOD
    }
}

private fun findBestWindowScore(
    bitmap: Bitmap,
    windowFractions: List<Float>,
    verticalStartFraction: Float = 0f,
    scorer: (Bitmap) -> Double
): Double {
    val w = bitmap.width
    val h = bitmap.height
    var bestScore = 0.0

    for (fraction in windowFractions) {
        val winW = (w * fraction).toInt()
        val winH = (h * fraction).toInt()
        if (winW < 24 || winH < 24) continue

        val stepX = max(12, winW / 4)
        val stepY = max(12, winH / 4)
        val startTop = (h * verticalStartFraction).toInt().coerceIn(0, max(0, h - winH))

        var top = startTop
        while (top + winH <= h) {
            var left = 0
            while (left + winW <= w) {
                val window = Bitmap.createBitmap(bitmap, left, top, winW, winH)
                val score = scorer(window)
                if (score > bestScore) {
                    bestScore = score
                }
                left += stepX
            }
            top += stepY
        }
    }

    return bestScore.coerceIn(0.0, 1.0)
}
