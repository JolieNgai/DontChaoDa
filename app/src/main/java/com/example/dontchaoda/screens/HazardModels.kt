package com.example.dontchaoda.screens

import androidx.compose.ui.geometry.Rect

enum class Risk(val score: Int) {
    SAFE(0),
    MEDIUM(50),
    HIGH(100)
}

data class HazardBox(
    val label: String,
    val risk: Risk,
    val rect: Rect,          // IMPORTANT: rect is in PreviewView coordinates (pixels)
    val confidence: Float = 0f,
)

