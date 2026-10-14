package com.example.dontchaoda

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.dontchaoda.screens.AppDatabase
import com.example.dontchaoda.screens.RecommendationEntity
import com.example.dontchaoda.screens.RoomEntity
import com.example.dontchaoda.screens.ScanDao
import com.example.dontchaoda.screens.ScanEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanDaoIntegrationTest {

    @get:org.junit.Rule
    val loggingRule = InstrumentedTestLoggingRule(
        mapOf(
            "insertRoom_andQueryByOwner_returnsOnlyMatchingRooms" to listOf(
                "insert rooms for two different users",
                "query rooms for one owner",
                "verify only that owner's rooms are returned"
            ),
            "renameRoomAndScans_updatesStoredRoomNameEverywhere" to listOf(
                "insert a room and a scan for that room",
                "rename the room and associated scans",
                "verify the updated room name is reflected everywhere"
            ),
            "insertScanWithRecommendations_returnsJoinedRecommendations" to listOf(
                "insert a scan with linked recommendations",
                "load the scan with its recommendations",
                "verify the joined data is returned correctly"
            )
        )
    )

    private lateinit var database: AppDatabase
    private lateinit var scanDao: ScanDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scanDao = database.scanDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertRoom_andQueryByOwner_returnsOnlyMatchingRooms() = runBlocking {
        scanDao.insertRoom(RoomEntity(ownerEmail = "alice@example.com", name = "Kitchen"))
        scanDao.insertRoom(RoomEntity(ownerEmail = "alice@example.com", name = "Living Room"))
        scanDao.insertRoom(RoomEntity(ownerEmail = "bob@example.com", name = "Garage"))

        val rooms = scanDao.getAllRoomsForUser("alice@example.com").first()

        assertEquals(listOf("Kitchen", "Living Room"), rooms.map { it.name })
    }

    @Test
    fun renameRoomAndScans_updatesStoredRoomNameEverywhere() = runBlocking {
        scanDao.insertRoom(RoomEntity(ownerEmail = "alice@example.com", name = "Kitchen"))
        scanDao.insertScan(
            ScanEntity(
                ownerEmail = "alice@example.com",
                roomName = "Kitchen",
                date = "03 Apr 2026, 12:00",
                riskScore = 70,
                hazards = "Fire hazard"
            )
        )

        scanDao.renameRoom("alice@example.com", "Kitchen", "Pantry")
        scanDao.renameScansRoom("alice@example.com", "Kitchen", "Pantry")

        val rooms = scanDao.getAllRoomsForUser("alice@example.com").first()
        val latestScan = scanDao.getLatestScanForRoom("alice@example.com", "Pantry")

        assertEquals(listOf("Pantry"), rooms.map { it.name })
        assertNotNull(latestScan)
        assertEquals("Pantry", latestScan?.roomName)
    }

    @Test
    fun insertScanWithRecommendations_returnsJoinedRecommendations() = runBlocking {
        val scanId = scanDao.insertScan(
            ScanEntity(
                ownerEmail = "alice@example.com",
                roomName = "Kitchen",
                date = "03 Apr 2026, 14:30",
                riskScore = 85,
                hazards = "Fire hazard,Frayed wire"
            )
        ).toInt()

        scanDao.insertRecommendations(
            listOf(
                RecommendationEntity(scanId = scanId, title = "Fire Safety", description = "Keep combustibles away."),
                RecommendationEntity(scanId = scanId, title = "Fix Damaged Wires", description = "Replace the frayed cable.")
            )
        )

        val joined = scanDao.getScanWithRecommendations(scanId)

        assertNotNull(joined)
        assertEquals("Kitchen", joined?.scan?.roomName)
        assertEquals(2, joined?.recommendations?.size)
        assertTrue(joined?.recommendations?.any { it.title == "Fix Damaged Wires" } == true)
    }
}
