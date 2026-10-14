package com.example.dontchaoda.screens

import androidx.room.Embedded
import androidx.room.Relation

data class ScanWithRecommendations(
    @Embedded val scan: ScanEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "scanId"
    )
    val recommendations: List<RecommendationEntity>
)