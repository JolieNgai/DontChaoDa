package com.example.dontchaoda

import androidx.compose.ui.geometry.Rect
import com.example.dontchaoda.screens.HazardBox
import com.example.dontchaoda.screens.RecommendationEntity
import com.example.dontchaoda.screens.Risk
import com.example.dontchaoda.screens.RoomEntity
import com.example.dontchaoda.screens.ScanDao
import com.example.dontchaoda.screens.ScanEntity
import com.example.dontchaoda.screens.ScanWithRecommendations
import com.example.dontchaoda.viewmodel.AuthGateway
import com.example.dontchaoda.viewmodel.AppViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val loggingRule = TestLoggingRule(
        mapOf(
            "login_withEmptyEmail_returnsFalseAndDoesNotLogIn" to listOf(
                "attempt login with an empty email",
                "verify validation message is shown",
                "verify the user remains logged out"
            ),
            "login_withInvalidEmail_returnsFalseAndDoesNotLogIn" to listOf(
                "attempt login with a malformed email",
                "verify invalid email validation is returned",
                "verify auth state does not change"
            ),
            "logout_clearsEmailAndLoggedInState" to listOf(
                "set a logged-in session in the view model",
                "call logout",
                "verify email, room selection, and tutorial state are cleared"
            ),
            "addRoom_addsNewRoomToList" to listOf(
                "add a custom room name",
                "verify it appears in the room list"
            ),
            "renameRoom_updatesExistingRoomNameAndSelection" to listOf(
                "rename an existing room",
                "verify the old name is removed",
                "verify the selected room follows the rename"
            ),
            "updateScanResults_setsAverageRiskAndLevel" to listOf(
                "submit mixed hazard results",
                "verify the average risk score is calculated",
                "verify the overall risk level is updated"
            ),
            "getRecommendationsForRoom_returnsRecommendationForSavedHazards" to listOf(
                "seed saved hazards for a room",
                "fetch recommendations",
                "verify the correct recommendation is returned"
            ),
            "register_withShortPassword_returnsValidationMessage" to listOf(
                "attempt registration with a short password",
                "verify password length validation is returned"
            ),
            "register_withValidCredentials_logsInAndShowsTutorial" to listOf(
                "register with valid credentials",
                "verify the user is logged in",
                "verify the tutorial flag is enabled"
            ),
            "login_whenGatewayFails_returnsGatewayMessage" to listOf(
                "force the auth gateway login to fail",
                "attempt login with valid-looking credentials",
                "verify the failure message is surfaced"
            ),
            "updateScanResults_withNoHazards_setsSafeDefaults" to listOf(
                "submit an empty hazard list",
                "verify the score resets to zero",
                "verify the overall state becomes safe"
            ),
            "saveCurrentScan_deduplicatesHazardsAndCreatesReminder" to listOf(
                "save a scan containing duplicate hazard labels",
                "verify stored hazards are deduplicated",
                "verify a hazard recheck reminder is generated"
            ),
            "fixHazard_movesHazardToFixedAndUpdatesLatestScan" to listOf(
                "mark a hazard as fixed for the latest room scan",
                "verify it moves to fixed hazards",
                "verify the latest stored scan is updated"
            ),
            "generateReminders_addsOnlyDefaultEntriesOnce" to listOf(
                "generate reminders twice",
                "verify default reminders are not duplicated"
            ),
            "getRecommendationsForRoom_prefersCurrentScanResultsOverSavedHistory" to listOf(
                "seed both live scan results and saved history",
                "fetch recommendations",
                "verify the current scan results take priority"
            )
        )
    )

    private lateinit var viewModel: AppViewModel
    private lateinit var authGateway: FakeAuthGateway
    private lateinit var scanDao: FakeScanDao

    @Before
    fun setup() {
        authGateway = FakeAuthGateway()
        scanDao = FakeScanDao()
        viewModel = AppViewModel(
            scanDao = scanDao,
            authGateway = authGateway,
            enableRoomPersistence = false
        )
    }

    @Test
    fun login_withEmptyEmail_returnsFalseAndDoesNotLogIn() {
        var result: Boolean? = null
        var message: String? = null

        viewModel.login("", "1234") { success, errorMessage ->
            result = success
            message = errorMessage
        }

        assertEquals(false, result)
        assertEquals("Email cannot be empty", message)
        assertFalse(viewModel.isLoggedIn)
        assertEquals("", viewModel.email)
    }

    @Test
    fun login_withInvalidEmail_returnsFalseAndDoesNotLogIn() {
        var result: Boolean? = null
        var message: String? = null

        viewModel.login("wronguser", "wrongpass") { success, errorMessage ->
            result = success
            message = errorMessage
        }

        assertEquals(false, result)
        assertEquals("Please enter a valid email address", message)
        assertFalse(viewModel.isLoggedIn)
        assertEquals("", viewModel.email)
    }

    @Test
    fun logout_clearsEmailAndLoggedInState() {
        viewModel.isLoggedIn = true
        viewModel.email = "test@example.com"
        viewModel.selectedRoom = "Kitchen"
        viewModel.showTutorial = true

        viewModel.logout()

        assertFalse(viewModel.isLoggedIn)
        assertEquals("", viewModel.email)
        assertEquals("", viewModel.selectedRoom)
        assertFalse(viewModel.showTutorial)
    }

    @Test
    fun addRoom_addsNewRoomToList() {
        viewModel.email = "test@example.com"
        val initialSize = viewModel.rooms.size

        viewModel.addRoom("New Test Room")

        assertEquals(initialSize + 1, viewModel.rooms.size)
        assertTrue(viewModel.rooms.contains("New Test Room"))
    }

    @Test
    fun renameRoom_updatesExistingRoomNameAndSelection() {
        viewModel.email = "test@example.com"
        viewModel.addRoom("Old Room")
        viewModel.selectedRoom = "Old Room"

        viewModel.renameRoom("Old Room", "Renamed Room")

        assertTrue(viewModel.rooms.contains("Renamed Room"))
        assertFalse(viewModel.rooms.contains("Old Room"))
        assertEquals("Renamed Room", viewModel.selectedRoom)
    }

    @Test
    fun updateScanResults_setsAverageRiskAndLevel() {
        val hazards = listOf(
            HazardBox("Fire hazard", Risk.HIGH, Rect.Zero),
            HazardBox("Frayed wire", Risk.MEDIUM, Rect.Zero)
        )

        viewModel.updateScanResults(hazards)

        assertEquals(75, viewModel.overallRiskScore)
        assertEquals("high", viewModel.overallRiskLevel)
        assertEquals(2, viewModel.currentScanResults.size)
    }

    @Test
    fun getRecommendationsForRoom_returnsRecommendationForSavedHazards() {
        viewModel.selectedRoom = "Kitchen"
        viewModel.roomScans["Kitchen"] = mutableListOf(
            com.example.dontchaoda.viewmodel.SavedScan(
                date = "31 Mar 2026, 10:00",
                riskscore = 50,
                hazards = listOf("Frayed wire")
            )
        )

        val recommendations = viewModel.getRecommendationsForRoom("Kitchen")

        assertEquals(1, recommendations.size)
        assertEquals("Fix Damaged Wires", recommendations.first().title)
    }

    @Test
    fun register_withShortPassword_returnsValidationMessage() {
        var result: Boolean? = null
        var message: String? = null

        viewModel.register("user@example.com", "123") { success, errorMessage ->
            result = success
            message = errorMessage
        }

        assertFalse(result == true)
        assertEquals("Password must be at least 6 characters", message)
        assertFalse(viewModel.isLoggedIn)
    }

    @Test
    fun register_withValidCredentials_logsInAndShowsTutorial() {
        var result: Boolean? = null

        viewModel.register("user@example.com", "secret123") { success, _ ->
            result = success
        }

        assertTrue(result == true)
        assertTrue(viewModel.isLoggedIn)
        assertEquals("user@example.com", viewModel.email)
        assertTrue(viewModel.showTutorial)
    }

    @Test
    fun login_whenGatewayFails_returnsGatewayMessage() {
        authGateway.loginShouldSucceed = false
        authGateway.failure = IllegalStateException("Service unavailable")

        var result: Boolean? = null
        var message: String? = null

        viewModel.login("user@example.com", "secret123") { success, errorMessage ->
            result = success
            message = errorMessage
        }

        assertFalse(result == true)
        assertEquals("Service unavailable", message)
        assertFalse(viewModel.isLoggedIn)
    }

    @Test
    fun updateScanResults_withNoHazards_setsSafeDefaults() {
        viewModel.updateScanResults(emptyList())

        assertEquals(0, viewModel.overallRiskScore)
        assertEquals("safe", viewModel.overallRiskLevel)
        assertTrue(viewModel.currentScanResults.isEmpty())
    }

    @Test
    fun saveCurrentScan_deduplicatesHazardsAndCreatesReminder() = runTest {
        viewModel.email = "user@example.com"
        viewModel.selectedRoom = "Kitchen"
        viewModel.currentScanResults.addAll(
            listOf(
                HazardBox("Fire hazard (0.91)", Risk.HIGH, Rect.Zero),
                HazardBox("Fire hazard (0.88)", Risk.HIGH, Rect.Zero),
                HazardBox("Frayed wire", Risk.MEDIUM, Rect.Zero)
            )
        )
        viewModel.overallRiskScore = 75
        viewModel.overallRiskLevel = "high"

        viewModel.saveCurrentScan()
        advanceUntilIdle()

        val saved = viewModel.roomScans["Kitchen"]?.firstOrNull()
        assertEquals(listOf("Fire hazard", "Frayed wire"), saved?.hazards)
        assertTrue(viewModel.reminders.any { it.title == "Hazard Recheck" })
        assertEquals("Fire hazard,Frayed wire", scanDao.insertedScans.single().hazards)
    }

    @Test
    fun fixHazard_movesHazardToFixedAndUpdatesLatestScan() = runTest {
        viewModel.email = "user@example.com"
        viewModel.roomScans["Kitchen"] = mutableListOf(
            com.example.dontchaoda.viewmodel.SavedScan(
                date = "31 Mar 2026, 10:00",
                riskscore = 80,
                hazards = listOf("Fire hazard", "Frayed wire")
            )
        )
        scanDao.insertScan(
            ScanEntity(
                id = 1,
                ownerEmail = "user@example.com",
                roomName = "Kitchen",
                date = "31 Mar 2026, 10:00",
                riskScore = 80,
                hazards = "Fire hazard,Frayed wire"
            )
        )

        viewModel.fixHazard("Kitchen", "Fire hazard")
        advanceUntilIdle()

        assertEquals(listOf("Fire hazard"), viewModel.roomFixedHazards["Kitchen"])
        assertEquals(listOf("Frayed wire"), viewModel.roomScans["Kitchen"]?.first()?.hazards)
        assertEquals("Frayed wire", scanDao.updatedScans.single().hazards)
    }

    @Test
    fun generateReminders_addsOnlyDefaultEntriesOnce() {
        viewModel.generateReminders()
        viewModel.generateReminders()

        assertEquals(2, viewModel.reminders.size)
        assertTrue(viewModel.reminders.any { it.title == "Daily Quick Scan" })
        assertTrue(viewModel.reminders.any { it.title == "Safety Tip" })
    }

    @Test
    fun getRecommendationsForRoom_prefersCurrentScanResultsOverSavedHistory() {
        viewModel.selectedRoom = "Kitchen"
        viewModel.currentScanResults.add(
            HazardBox("Storage obstruction", Risk.MEDIUM, Rect.Zero)
        )
        viewModel.roomScans["Kitchen"] = mutableListOf(
            com.example.dontchaoda.viewmodel.SavedScan(
                date = "31 Mar 2026, 10:00",
                riskscore = 50,
                hazards = listOf("Frayed wire")
            )
        )

        val recommendations = viewModel.getRecommendationsForRoom("Kitchen")

        assertEquals(1, recommendations.size)
        assertEquals("Clear Pathways", recommendations.first().title)
    }
}

private class FakeAuthGateway : AuthGateway {
    override var currentUserEmail: String? = null
    var loginShouldSucceed = true
    var registerShouldSucceed = true
    var failure: Exception? = null

    override fun register(email: String, password: String, onResult: (Boolean, Exception?) -> Unit) {
        if (registerShouldSucceed) {
            currentUserEmail = email
            onResult(true, null)
        } else {
            onResult(false, failure)
        }
    }

    override fun login(email: String, password: String, onResult: (Boolean, Exception?) -> Unit) {
        if (loginShouldSucceed) {
            currentUserEmail = email
            onResult(true, null)
        } else {
            onResult(false, failure)
        }
    }

    override fun logout() {
        currentUserEmail = null
    }
}

private class FakeScanDao : ScanDao {
    private val rooms = mutableListOf<RoomEntity>()
    private val scans = mutableListOf<ScanEntity>()
    val insertedScans = mutableListOf<ScanEntity>()
    val updatedScans = mutableListOf<ScanEntity>()

    override fun getAllRoomsForUser(ownerEmail: String): Flow<List<RoomEntity>> {
        return MutableStateFlow(rooms.filter { it.ownerEmail == ownerEmail })
    }

    override suspend fun insertRoom(room: RoomEntity): Long {
        if (rooms.none { it.ownerEmail == room.ownerEmail && it.name == room.name }) {
            rooms.add(room)
            return 1L
        }
        return -1L
    }

    override suspend fun renameRoom(ownerEmail: String, oldName: String, newName: String) {
        val index = rooms.indexOfFirst { it.ownerEmail == ownerEmail && it.name == oldName }
        if (index != -1) {
            rooms[index] = RoomEntity(ownerEmail = ownerEmail, name = newName)
        }
    }

    override suspend fun renameScansRoom(ownerEmail: String, oldName: String, newName: String) = Unit

    override fun getScansForRoom(ownerEmail: String, roomName: String): Flow<List<ScanEntity>> {
        return flowOf(scans.filter { it.ownerEmail == ownerEmail && it.roomName == roomName })
    }

    override suspend fun getLatestScanForRoom(ownerEmail: String, roomName: String): ScanEntity? {
        return scans.lastOrNull { it.ownerEmail == ownerEmail && it.roomName == roomName }
    }

    override suspend fun insertScan(scan: ScanEntity): Long {
        scans.add(scan)
        insertedScans.add(scan)
        return 1L
    }

    override suspend fun updateScan(scan: ScanEntity) {
        updatedScans.add(scan)
        val index = scans.indexOfFirst { it.ownerEmail == scan.ownerEmail && it.roomName == scan.roomName }
        if (index != -1) {
            scans[index] = scan
        }
    }

    override fun getAllScansForUser(ownerEmail: String): Flow<List<ScanEntity>> = flowOf(scans.filter { it.ownerEmail == ownerEmail })

    override suspend fun deleteScan(scan: ScanEntity) = Unit

    override suspend fun insertRecommendations(recommendations: List<RecommendationEntity>) = Unit

    override suspend fun getRecommendationsForScan(scanId: Int): List<RecommendationEntity> = emptyList()

    override suspend fun deleteRecommendationsForScan(scanId: Int) = Unit

    override suspend fun getScanWithRecommendations(scanId: Int): ScanWithRecommendations? = null

    override fun getScansWithRecommendationsForRoom(ownerEmail: String, roomName: String): Flow<List<ScanWithRecommendations>> {
        return flowOf(emptyList())
    }
}
