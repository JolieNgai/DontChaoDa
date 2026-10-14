package com.example.dontchaoda.screens

import androidx.annotation.DrawableRes

enum class HazardType { FIRE, FRAYED_WIRE, FLAMMABLES, STORAGE_OBSTRUCTION }

data class HazardDef(
    val type: HazardType,
    val label: String,
    val risk: Risk,
    @DrawableRes val templateResId: Int,
    val threshold: Double
)