package com.example.dontchaoda.screens

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecommendationDao {

    @Insert
    suspend fun insertRecommendations(recommendations: List<RecommendationEntity>)

    @Query("SELECT * FROM recommendations WHERE scanId = :scanId")
    suspend fun getRecommendationsForScan(scanId: Int): List<RecommendationEntity>

    @Query("DELETE FROM recommendations WHERE scanId = :scanId")
    suspend fun deleteRecommendationsForScan(scanId: Int)
}