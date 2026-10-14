package com.example.dontchaoda

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import com.example.dontchaoda.screens.HazardBox
import com.example.dontchaoda.screens.RecommendationEntity
import com.example.dontchaoda.screens.RoomEntity
import com.example.dontchaoda.screens.RoomHistoryScreen
import com.example.dontchaoda.screens.Risk
import com.example.dontchaoda.screens.ScanDao
import com.example.dontchaoda.screens.ScanEntity
import com.example.dontchaoda.screens.ScanSummaryScreen
import com.example.dontchaoda.screens.ScanWithRecommendations
import com.example.dontchaoda.ui.theme.DontChaoDaTheme
import com.example.dontchaoda.viewmodel.AppViewModel
import com.example.dontchaoda.viewmodel.AuthGateway
import com.example.dontchaoda.viewmodel.SavedScan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppInstrumentedUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val loggingRule = InstrumentedTestLoggingRule(
        mapOf(
            "authentication_successfulLoginFlow_navigatesToHome" to listOf(
                "open the login screen",
                "enter valid email and password",
                "tap login and verify the home screen appears"
            ),
            "authentication_registrationValidation_showsInvalidEmailError" to listOf(
                "open the register screen",
                "enter an invalid email",
                "tap create account and verify the email error is shown"
            ),
            "navigation_bottomBar_switchesBetweenHomeAndProfile" to listOf(
                "start from a logged-in home state",
                "tap the profile tab and verify profile content",
                "tap the home tab and verify the home content returns"
            ),
            "roomManagement_addingNewRoom_makesItSelectable" to listOf(
                "open room selection from home",
                "tap add new room and enter Balcony",
                "confirm and verify Balcony appears in the list"
            ),
            "profile_logoutFunctionality_returnsToWelcomeScreen" to listOf(
                "open the profile screen",
                "tap log out",
                "verify the welcome screen login and register buttons are shown"
            ),
            "scanSummary_displaysResultsAndRiskScore" to listOf(
                "seed scan results in the view model",
                "open the scan summary screen",
                "verify hazards and the overall risk score are displayed"
            ),
            "history_navigationFromHomeToRoomSpecificScanRecords_displaysSavedData" to listOf(
                "seed a saved scan for Kitchen",
                "open the home screen and tap Kitchen",
                "verify the saved date and score are visible"
            ),
            "reminders_automaticGeneration_populatesDefaultItems" to listOf(
                "open reminders from a logged-in home state",
                "verify the default reminder items are visible"
            ),
            "hazardManagement_markingHazardFixed_updatesStatus" to listOf(
                "seed an active hazard in room history",
                "mark the hazard as complete",
                "verify the hazard now shows under fixed hazards"
            ),
            "faqNavigation_displaysFrequentlyAskedQuestions" to listOf(
                "open help and support from profile",
                "navigate to FAQ",
                "open common FAQ items",
                "verify their answers are displayed"
            ),
            "login_forgotPassword_invalidEmail_showsValidationMessage" to listOf(
                "open the login screen",
                "enter an invalid email",
                "tap forgot password and verify validation feedback"
            )
        )
    )

    private lateinit var fakeAuthGateway: InstrumentedFakeAuthGateway
    private lateinit var viewModel: AppViewModel

    @Before
    fun setup() {
        fakeAuthGateway = InstrumentedFakeAuthGateway()
        viewModel = buildViewModel(fakeAuthGateway)
    }

    @Test
    fun authentication_successfulLoginFlow_navigatesToHome() {
        setAppContent()

        composeRule.onNodeWithText("Login").performClick()
        composeRule.onNodeWithText("Email").performTextInput("user@example.com")
        composeRule.onNodeWithText("Password").performTextInput("secret123")
        composeRule.onAllNodesWithText("Login")[1].performClick()

        composeRule.onNodeWithText("Start Scan").assertIsDisplayed()
    }

    @Test
    fun authentication_registrationValidation_showsInvalidEmailError() {
        setAppContent()

        composeRule.onNodeWithText("Register").performClick()
        composeRule.onNodeWithText("Email").performTextInput("bad-email")
        composeRule.onNodeWithText("Password").performTextInput("secret123")
        composeRule.onNodeWithText("Confirm Password").performTextInput("secret123")
        composeRule.onNodeWithText("Create Account").performClick()

        composeRule.onNodeWithText("Please enter a valid email address").assertIsDisplayed()
    }

    @Test
    fun navigation_bottomBar_switchesBetweenHomeAndProfile() {
        fakeAuthGateway.currentUserEmail = "user@example.com"
        viewModel = buildViewModel(fakeAuthGateway).apply {
            isLoggedIn = true
            email = "user@example.com"
        }
        setAppContent()

        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("Personal Information").assertIsDisplayed()

        composeRule.onNodeWithText("Home").performClick()
        composeRule.onNodeWithText("Start Scan").assertIsDisplayed()
    }

    @Test
    fun roomManagement_addingNewRoom_makesItSelectable() {
        fakeAuthGateway.currentUserEmail = "user@example.com"
        viewModel = buildViewModel(fakeAuthGateway).apply {
            isLoggedIn = true
            email = "user@example.com"
        }
        setAppContent()

        composeRule.onNodeWithText("Start Scan").performClick()
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.onNodeWithText("Room Name").performTextInput("Balcony")
        composeRule.onNodeWithText("Confirm").performClick()

        composeRule.onNodeWithText("Balcony").assertIsDisplayed()
    }

    @Test
    fun profile_logoutFunctionality_returnsToWelcomeScreen() {
        fakeAuthGateway.currentUserEmail = "user@example.com"
        viewModel = buildViewModel(fakeAuthGateway).apply {
            isLoggedIn = true
            email = "user@example.com"
        }
        setAppContent()

        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("Log Out").performClick()

        composeRule.onNodeWithText("Login").assertIsDisplayed()
        composeRule.onNodeWithText("Register").assertIsDisplayed()
    }

    @Test
    fun scanSummary_displaysResultsAndRiskScore() {
        viewModel.selectedRoom = "Kitchen"
        viewModel.updateScanResults(
            listOf(
                HazardBox("Fire hazard", Risk.HIGH, Rect.Zero),
                HazardBox("Frayed wire", Risk.MEDIUM, Rect.Zero)
            )
        )

        composeRule.setContent {
            DontChaoDaTheme {
                ScanSummaryScreen(
                    navController = rememberNavController(),
                    appViewModel = viewModel
                )
            }
        }

        composeRule.onNodeWithText("Your Kitchen has been analyzed").assertIsDisplayed()
        composeRule.onNodeWithText("HIGH").assertIsDisplayed()
        composeRule.onNodeWithText("Risk Score: 75 / 100").assertIsDisplayed()
        composeRule.onNodeWithText("Fire hazard").assertIsDisplayed()
        composeRule.onNodeWithText("Frayed wire").assertIsDisplayed()
    }

    @Test
    fun history_navigationFromHomeToRoomSpecificScanRecords_displaysSavedData() {
        fakeAuthGateway.currentUserEmail = "user@example.com"
        viewModel = buildViewModel(fakeAuthGateway).apply {
            isLoggedIn = true
            email = "user@example.com"
        }
        viewModel.roomScans["Kitchen"] = mutableListOf(
            SavedScan(
                date = "03 Apr 2026, 09:30",
                riskscore = 82,
                hazards = listOf("Fire hazard", "Frayed wire")
            )
        )
        setAppContent()

        composeRule.onNodeWithText("Kitchen").performClick()

        composeRule.onNodeWithText("Overview").assertIsDisplayed()
        composeRule.onAllNodesWithText("03 Apr 2026, 09:30")[0].assertIsDisplayed()
        composeRule.onNodeWithText("82").assertIsDisplayed()
    }

    @Test
    fun reminders_automaticGeneration_populatesDefaultItems() {
        fakeAuthGateway.currentUserEmail = "user@example.com"
        viewModel = buildViewModel(fakeAuthGateway).apply {
            isLoggedIn = true
            email = "user@example.com"
        }
        setAppContent()

        composeRule.onNodeWithContentDescription("Reminders").performClick()

        composeRule.onNodeWithText("Daily Quick Scan").assertIsDisplayed()
        composeRule.onNodeWithText("Safety Tip").assertIsDisplayed()
    }

    @Test
    fun hazardManagement_markingHazardFixed_updatesStatus() {
        viewModel.selectedRoom = "Kitchen"
        viewModel.roomScans["Kitchen"] = mutableListOf(
            SavedScan(
                date = "03 Apr 2026, 09:30",
                riskscore = 82,
                hazards = listOf("Fire hazard")
            )
        )

        composeRule.setContent {
            DontChaoDaTheme {
                RoomHistoryScreen(
                    navController = rememberNavController(),
                    appViewModel = viewModel
                )
            }
        }

        composeRule.onNodeWithText("Fire hazard").performClick()
        composeRule.onNodeWithText("Complete").performClick()

        composeRule.onNodeWithText("Fixed Hazards").assertIsDisplayed()
        composeRule.onAllNodesWithText("Fixed")[0].assertIsDisplayed()
    }

    @Test
    fun faqNavigation_displaysFrequentlyAskedQuestions() {
        fakeAuthGateway.currentUserEmail = "user@example.com"
        viewModel = buildViewModel(fakeAuthGateway).apply {
            isLoggedIn = true
            email = "user@example.com"
        }
        setAppContent()

        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("Help & Support").performClick()
        composeRule.onNodeWithText("View FAQ").performClick()

        composeRule.onNodeWithText("How do I start a scan?").assertIsDisplayed()
        composeRule.onNodeWithText("How do I start a scan?").performClick()
        composeRule.onNodeWithText("Go to the Home screen and tap the large 'Start Scan' button. Point your camera at electrical outlets or appliances to detect potential fire hazards.").assertIsDisplayed()

        composeRule.onNodeWithText("What does the scan actually detect?").performClick()
        composeRule.onNodeWithText("The scan detects common household hazards such as fire risks, frayed wires, flammable materials, and obstructions.").assertIsDisplayed()
    }

    @Test
    fun login_forgotPassword_invalidEmail_showsValidationMessage() {
        setAppContent()

        composeRule.onNodeWithText("Login").performClick()
        composeRule.onNodeWithText("Email").performTextInput("bad-email")
        composeRule.onNodeWithText("Forgot password?").performClick()

        composeRule.onNodeWithText("Please enter a valid email address").assertIsDisplayed()
    }

    private fun setAppContent() {
        composeRule.setContent {
            DontChaoDaTheme {
                val navController = rememberNavController()
                com.example.dontchaoda.navigation.AppNavHost(
                    navController = navController,
                    appViewModel = viewModel
                )
            }
        }
    }

    private fun buildViewModel(authGateway: InstrumentedFakeAuthGateway): AppViewModel {
        return AppViewModel(
            scanDao = InstrumentedFakeScanDao(),
            authGateway = authGateway,
            enableRoomPersistence = false
        )
    }
}

private class InstrumentedFakeAuthGateway : AuthGateway {
    override var currentUserEmail: String? = null
    var loginShouldSucceed = true
    var registerShouldSucceed = true

    override fun register(email: String, password: String, onResult: (Boolean, Exception?) -> Unit) {
        if (registerShouldSucceed) {
            currentUserEmail = email
            onResult(true, null)
        } else {
            onResult(false, IllegalStateException("Registration failed"))
        }
    }

    override fun login(email: String, password: String, onResult: (Boolean, Exception?) -> Unit) {
        if (loginShouldSucceed) {
            currentUserEmail = email
            onResult(true, null)
        } else {
            onResult(false, IllegalArgumentException("Login failed"))
        }
    }

    override fun logout() {
        currentUserEmail = null
    }
}

private class InstrumentedFakeScanDao : ScanDao {
    private val rooms = mutableListOf<RoomEntity>()
    private val scans = mutableListOf<ScanEntity>()
    private val recommendations = mutableListOf<RecommendationEntity>()
    private var nextScanId = 1

    override fun getAllRoomsForUser(ownerEmail: String): Flow<List<RoomEntity>> {
        return flowOf(rooms.filter { it.ownerEmail == ownerEmail }.sortedBy { it.name })
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

    override suspend fun renameScansRoom(ownerEmail: String, oldName: String, newName: String) {
        scans.replaceAll { scan ->
            if (scan.ownerEmail == ownerEmail && scan.roomName == oldName) {
                scan.copy(roomName = newName)
            } else {
                scan
            }
        }
    }

    override fun getScansForRoom(ownerEmail: String, roomName: String): Flow<List<ScanEntity>> {
        return flowOf(scans.filter { it.ownerEmail == ownerEmail && it.roomName == roomName })
    }

    override suspend fun getLatestScanForRoom(ownerEmail: String, roomName: String): ScanEntity? {
        return scans.lastOrNull { it.ownerEmail == ownerEmail && it.roomName == roomName }
    }

    override suspend fun insertScan(scan: ScanEntity): Long {
        val id = nextScanId++
        scans.add(scan.copy(id = id))
        return id.toLong()
    }

    override suspend fun updateScan(scan: ScanEntity) {
        val index = scans.indexOfFirst { it.id == scan.id }
        if (index != -1) {
            scans[index] = scan
        }
    }

    override fun getAllScansForUser(ownerEmail: String): Flow<List<ScanEntity>> {
        return flowOf(scans.filter { it.ownerEmail == ownerEmail })
    }

    override suspend fun deleteScan(scan: ScanEntity) {
        scans.removeIf { it.id == scan.id }
        recommendations.removeIf { it.scanId == scan.id }
    }

    override suspend fun insertRecommendations(recommendations: List<RecommendationEntity>) {
        this.recommendations.addAll(recommendations)
    }

    override suspend fun getRecommendationsForScan(scanId: Int): List<RecommendationEntity> {
        return recommendations.filter { it.scanId == scanId }
    }

    override suspend fun deleteRecommendationsForScan(scanId: Int) {
        recommendations.removeIf { it.scanId == scanId }
    }

    override suspend fun getScanWithRecommendations(scanId: Int): ScanWithRecommendations? {
        val scan = scans.firstOrNull { it.id == scanId } ?: return null
        return ScanWithRecommendations(scan = scan, recommendations = recommendations.filter { it.scanId == scanId })
    }

    override fun getScansWithRecommendationsForRoom(ownerEmail: String, roomName: String): Flow<List<ScanWithRecommendations>> {
        val roomScans = scans.filter { it.ownerEmail == ownerEmail && it.roomName == roomName }
        return flowOf(
            roomScans.map { scan ->
                ScanWithRecommendations(
                    scan = scan,
                    recommendations = recommendations.filter { it.scanId == scan.id }
                )
            }
        )
    }
}
