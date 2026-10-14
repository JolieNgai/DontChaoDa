package com.example.dontchaoda.screens

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scans",
    indices = [Index(value = ["ownerEmail"]), Index(value = ["ownerEmail", "roomName"])]
)
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ownerEmail: String,
    val roomName: String,
    val date: String,
    val riskScore: Int,
    val hazards: String,
)
