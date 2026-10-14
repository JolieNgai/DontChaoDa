package com.example.dontchaoda.screens

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Query("SELECT * FROM rooms WHERE ownerEmail = :ownerEmail ORDER BY name ASC")
    fun getAllRoomsForUser(ownerEmail: String): Flow<List<RoomEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRoom(room: RoomEntity): Long

    @Query("UPDATE rooms SET name = :newName WHERE ownerEmail = :ownerEmail AND name = :oldName")
    suspend fun renameRoom(ownerEmail: String, oldName: String, newName: String)

    @Query("UPDATE scans SET roomName = :newName WHERE ownerEmail = :ownerEmail AND roomName = :oldName")
    suspend fun renameScansRoom(ownerEmail: String, oldName: String, newName: String)

    @Query("SELECT * FROM scans WHERE ownerEmail = :ownerEmail AND roomName = :roomName")
    fun getScansForRoom(ownerEmail: String, roomName: String): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE ownerEmail = :ownerEmail AND roomName = :roomName ORDER BY id DESC LIMIT 1")
    suspend fun getLatestScanForRoom(ownerEmail: String, roomName: String): ScanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity): Long

    @Update
    suspend fun updateScan(scan: ScanEntity)

    @Query("SELECT * FROM scans WHERE ownerEmail = :ownerEmail")
    fun getAllScansForUser(ownerEmail: String): Flow<List<ScanEntity>>

    @Delete
    suspend fun deleteScan(scan: ScanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendations(recommendations: List<RecommendationEntity>)

    @Query("SELECT * FROM recommendations WHERE scanId = :scanId")
    suspend fun getRecommendationsForScan(scanId: Int): List<RecommendationEntity>

    @Query("DELETE FROM recommendations WHERE scanId = :scanId")
    suspend fun deleteRecommendationsForScan(scanId: Int)

    @Transaction
    @Query("SELECT * FROM scans WHERE id = :scanId")
    suspend fun getScanWithRecommendations(scanId: Int): ScanWithRecommendations?

    @Transaction
    @Query("SELECT * FROM scans WHERE ownerEmail = :ownerEmail AND roomName = :roomName")
    fun getScansWithRecommendationsForRoom(ownerEmail: String, roomName: String): Flow<List<ScanWithRecommendations>>
}
