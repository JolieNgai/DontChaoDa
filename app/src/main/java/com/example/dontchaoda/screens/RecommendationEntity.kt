package com.example.dontchaoda.screens

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recommendations",
    foreignKeys = [
        ForeignKey(
            entity = ScanEntity::class,
            parentColumns = ["id"],
            childColumns = ["scanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["scanId"])]
)
data class RecommendationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val scanId: Int,
    val title: String,
    val description: String
)