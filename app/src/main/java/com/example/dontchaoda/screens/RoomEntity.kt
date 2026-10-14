package com.example.dontchaoda.screens

import androidx.room.Entity

@Entity(
    tableName = "rooms",
    primaryKeys = ["ownerEmail", "name"]
)
data class RoomEntity(
    val ownerEmail: String,
    val name: String
)
