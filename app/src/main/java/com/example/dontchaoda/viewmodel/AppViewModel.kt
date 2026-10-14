package com.example.dontchaoda.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dontchaoda.model.Recommendation
import com.example.dontchaoda.model.Reminder
import com.example.dontchaoda.screens.HazardBox
import com.example.dontchaoda.screens.RecommendationEntity
import com.example.dontchaoda.screens.RoomEntity
import com.example.dontchaoda.screens.ScanDao
import com.example.dontchaoda.screens.ScanEntity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedScan(
    val date: String,
    val riskscore: Int,
    var hazards: List<String>
)

interface AuthGateway {
    val currentUserEmail: String?

    fun register(email: String, password: String, onResult: (Boolean, Exception?) -> Unit)

    fun login(email: String, password: String, onResult: (Boolean, Exception?) -> Unit)

    fun logout()
}

class FirebaseAuthGateway(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthGateway {
    override val currentUserEmail: String?
        get() = auth.currentUser?.email

    override fun register(email: String, password: String, onResult: (Boolean, Exception?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task -> onResult(task.isSuccessful, task.exception) }
    }

    override fun login(email: String, password: String, onResult: (Boolean, Exception?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task -> onResult(task.isSuccessful, task.exception) }
    }

    override fun logout() {
        auth.signOut()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val scanDao: ScanDao,
    private val authGateway: AuthGateway = FirebaseAuthGateway(),
    private val enableRoomPersistence: Boolean = true
) : ViewModel() {

    private val defaultRooms = listOf(
        "Living Room", "Kitchen", "Master Bedroom", "Bedroom", "Garage"
    )
    private val userEmailFlow = MutableStateFlow("")

    var isLoggedIn by mutableStateOf(false)
    var email by mutableStateOf("")
    var selectedRoom by mutableStateOf("")
    var showTutorial by mutableStateOf(false)

    val reminders = mutableStateListOf<Reminder>()
    val reminderCount: Int
        get() = reminders.size

    val roomScans = mutableStateMapOf<String, MutableList<SavedScan>>()
    val roomFixedHazards = mutableStateMapOf<String, MutableList<String>>()
    val rooms = mutableStateListOf<String>()

    var currentScanResults = mutableStateListOf<HazardBox>()
    var overallRiskScore by mutableStateOf(0)
    var overallRiskLevel by mutableStateOf("safe")

    init {
        syncAuthState()
        if (enableRoomPersistence) {
            observeRooms()
            observeSavedScans()
        } else {
            rooms.addAll(defaultRooms)
        }
    }

    private fun syncAuthState() {
        email = authGateway.currentUserEmail.orEmpty()
        isLoggedIn = email.isNotBlank()
        userEmailFlow.value = email
    }

    private fun observeRooms() {
        viewModelScope.launch {
            userEmailFlow
                .flatMapLatest { ownerEmail ->
                    if (ownerEmail.isBlank()) {
                        flowOf(emptyList())
                    } else {
                        flow {
                            seedDefaultRoomsIfNeeded(ownerEmail)
                            emitAll(scanDao.getAllRoomsForUser(ownerEmail))
                        }
                    }
                }
                .collectLatest { storedRooms ->
                    rooms.clear()
                    rooms.addAll(storedRooms.map { it.name })
                }
        }
    }

    private fun observeSavedScans() {
        viewModelScope.launch {
            userEmailFlow
                .flatMapLatest { ownerEmail ->
                    if (ownerEmail.isBlank()) flowOf(emptyList())
                    else scanDao.getAllScansForUser(ownerEmail)
                }
                .collectLatest { storedScans ->
                    val scansByRoom = storedScans
                        .sortedByDescending { it.id }
                        .groupBy { it.roomName }
                        .mapValues { (_, scans) ->
                            scans.map { scan ->
                                SavedScan(
                                    date = scan.date,
                                    riskscore = scan.riskScore,
                                    hazards = scan.hazards
                                        .split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                )
                            }.toMutableList()
                        }

                    roomScans.clear()
                    roomScans.putAll(scansByRoom)
                }
        }
    }

    private suspend fun seedDefaultRoomsIfNeeded(ownerEmail: String) {
        if (scanDao.getAllRoomsForUser(ownerEmail).first().isEmpty()) {
            defaultRooms.forEach { roomName ->
                scanDao.insertRoom(RoomEntity(ownerEmail = ownerEmail, name = roomName))
            }
        }
    }

    private fun isValidEmail(value: String): Boolean {
        return EMAIL_REGEX.matches(value)
    }

    fun register(
        inputEmail: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val e = inputEmail.trim().lowercase()
        val p = password.trim()

        when {
            e.isBlank() -> {
                onResult(false, "Email cannot be empty")
                return
            }

            !isValidEmail(e) -> {
                onResult(false, "Please enter a valid email address")
                return
            }

            p.isBlank() -> {
                onResult(false, "Password cannot be empty")
                return
            }

            p.length < 6 -> {
                onResult(false, "Password must be at least 6 characters")
                return
            }
        }

        authGateway.register(e, p) { success, exception ->
            if (success) {
                syncAuthState()
                showTutorial = true
                onResult(true, null)
            } else {
                val message = when (exception) {
                    is FirebaseAuthUserCollisionException -> "Email already exists"
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email address"
                    is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later."
                    is FirebaseNetworkException -> "Network error. Check your internet connection."
                    else -> exception?.localizedMessage ?: "Registration failed"
                }
                onResult(false, message)
            }
        }
    }

    fun login(
        inputEmail: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val e = inputEmail.trim().lowercase()
        val p = password.trim()

        when {
            e.isBlank() -> {
                onResult(false, "Email cannot be empty")
                return
            }

            !isValidEmail(e) -> {
                onResult(false, "Please enter a valid email address")
                return
            }

            p.isBlank() -> {
                onResult(false, "Password cannot be empty")
                return
            }
        }

        authGateway.login(e, p) { success, exception ->
            if (success) {
                syncAuthState()
                showTutorial = false
                onResult(true, null)
            } else {
                val message = when (exception) {
                    is FirebaseAuthInvalidUserException,
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
                    is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later."
                    is FirebaseNetworkException -> "Network error. Check your internet connection."
                    else -> exception?.localizedMessage ?: "Login failed"
                }
                onResult(false, message)
            }
        }
    }

    fun logout() {
        authGateway.logout()
        isLoggedIn = false
        email = ""
        selectedRoom = ""
        showTutorial = false
        userEmailFlow.value = ""
        rooms.clear()
        roomScans.clear()
        roomFixedHazards.clear()
    }

    fun resetPassword(
        inputEmail: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val e = inputEmail.trim().lowercase()

        when {
            e.isBlank() -> {
                onResult(false, "Email cannot be empty")
                return
            }

            !isValidEmail(e) -> {
                onResult(false, "Please enter a valid email address")
                return
            }
        }

        val auth = FirebaseAuth.getInstance()
        auth.sendPasswordResetEmail(e)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(
                        true,
                        "If an account exists for this email, a password reset email has been sent"
                    )
                } else {
                    val message = when (task.exception) {
                        is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later."
                        is FirebaseNetworkException -> "Network error. Check your internet connection."
                        else -> task.exception?.localizedMessage ?: "Could not send reset email"
                    }
                    onResult(false, message)
                }
            }
    }

    fun addRoom(name: String) {
        val ownerEmail = email
        val trimmedName = name.trim()
        if (ownerEmail.isBlank() || trimmedName.isBlank() || rooms.contains(trimmedName)) return

        rooms.add(trimmedName)

        if (enableRoomPersistence) {
            viewModelScope.launch {
                scanDao.insertRoom(RoomEntity(ownerEmail = ownerEmail, name = trimmedName))
            }
        }
    }

    fun renameRoom(oldName: String, newName: String) {
        val ownerEmail = email
        val trimmedName = newName.trim()
        val index = rooms.indexOf(oldName)
        if (ownerEmail.isBlank() || index == -1 || trimmedName.isBlank() || rooms.contains(trimmedName)) return

        rooms[index] = trimmedName
        roomScans.remove(oldName)?.let { roomScans[trimmedName] = it }
        roomFixedHazards.remove(oldName)?.let { roomFixedHazards[trimmedName] = it }

        if (selectedRoom == oldName) {
            selectedRoom = trimmedName
        }

        if (enableRoomPersistence) {
            viewModelScope.launch {
                scanDao.renameRoom(ownerEmail, oldName, trimmedName)
                scanDao.renameScansRoom(ownerEmail, oldName, trimmedName)
            }
        }
    }

    fun updateScanResults(hazards: List<HazardBox>) {
        currentScanResults.clear()
        currentScanResults.addAll(hazards)

        val totalScore = hazards.sumOf { it.risk.score }
        overallRiskScore = if (hazards.isNotEmpty()) totalScore / hazards.size else 0

        overallRiskLevel = when {
            overallRiskScore >= 70 -> "high"
            overallRiskScore >= 40 -> "medium"
            hazards.isEmpty() -> "safe"
            else -> "low"
        }
    }

    fun saveCurrentScan() {
        val ownerEmail = email
        if (ownerEmail.isBlank() || selectedRoom.isBlank()) return

        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val dateString = dateFormat.format(Date())
        val mergedHazards = currentScanResults
            .map { it.label.substringBefore(" (").trim() }
            .distinct()

        val newScan = SavedScan(
            date = dateString,
            riskscore = overallRiskScore,
            hazards = mergedHazards
        )

        val history = roomScans.getOrPut(selectedRoom) { mutableStateListOf() }
        history.add(0, newScan)

        viewModelScope.launch {
            val scanId = scanDao.insertScan(
                ScanEntity(
                    ownerEmail = ownerEmail,
                    roomName = selectedRoom,
                    date = dateString,
                    riskScore = overallRiskScore,
                    hazards = mergedHazards.joinToString(",")
                )
            ).toInt()

            val recommendationEntities = buildRecommendationsFromHazards(mergedHazards)
                .map { recommendation ->
                    RecommendationEntity(
                        scanId = scanId,
                        title = recommendation.title,
                        description = recommendation.description
                    )
                }

            if (recommendationEntities.isNotEmpty()) {
                scanDao.insertRecommendations(recommendationEntities)
            }
        }

        generateReminders()
    }

    fun fixHazard(roomName: String, hazardName: String) {
        val fixedList = roomFixedHazards.getOrPut(roomName) { mutableStateListOf() }
        if (!fixedList.contains(hazardName)) {
            fixedList.add(hazardName)
        }

        val scans = roomScans[roomName]
        if (!scans.isNullOrEmpty()) {
            val latest = scans[0]
            val updatedHazards = latest.hazards.filter { it != hazardName }
            scans[0] = latest.copy(hazards = updatedHazards)

            viewModelScope.launch {
                val ownerEmail = email
                if (ownerEmail.isBlank()) return@launch

                val latestDbScan = scanDao.getLatestScanForRoom(ownerEmail, roomName)
                if (latestDbScan != null) {
                    scanDao.updateScan(
                        latestDbScan.copy(hazards = updatedHazards.joinToString(","))
                    )
                }
            }
        }
    }

    fun generateReminders() {
        if (reminders.none { it.title == "Daily Quick Scan" }) {
            reminders.add(Reminder("Daily Quick Scan", "Don't forget to do a quick scan today!"))
        }

        if ((overallRiskLevel == "high" || overallRiskLevel == "medium")
            && reminders.none { it.title == "Hazard Recheck" }
        ) {
            reminders.add(
                Reminder(
                    "Hazard Recheck",
                    "Hazards were detected in your last scan. Check if they are fixed."
                )
            )
        }

        if (reminders.none { it.title == "Safety Tip" }) {
            reminders.add(
                Reminder(
                    "Safety Tip",
                    "Check electrical wires regularly to prevent fire hazards."
                )
            )
        }
    }

    fun getRecommendationsForRoom(roomName: String): List<Recommendation> {
        val currentHazards = currentScanResults
            .map { it.label.substringBefore(" (").trim() }
            .distinct()

        val savedHazards = roomScans[roomName]
            ?.firstOrNull()
            ?.hazards
            ?: emptyList()

        val hazardsToUse = if (currentHazards.isNotEmpty()) currentHazards else savedHazards
        return buildRecommendationsFromHazards(hazardsToUse)
    }

    private fun buildRecommendationsFromHazards(hazards: List<String>): List<Recommendation> {
        return hazards.mapNotNull { hazard ->
            when {
                hazard.contains("Fire", ignoreCase = true) -> Recommendation(
                    hazard = hazard,
                    title = "Fire Safety",
                    description = "Keep flammable items away and ensure a fire extinguisher is nearby."
                )

                hazard.contains("Frayed wire", ignoreCase = true) -> Recommendation(
                    hazard = hazard,
                    title = "Fix Damaged Wires",
                    description = "Replace or repair frayed wires immediately to prevent electrical fires."
                )

                hazard.contains("Flammable", ignoreCase = true) -> Recommendation(
                    hazard = hazard,
                    title = "Store Flammable Items Safely",
                    description = "Keep flammable materials away from heat sources."
                )

                hazard.contains("Storage obstruction", ignoreCase = true) -> Recommendation(
                    hazard = hazard,
                    title = "Clear Pathways",
                    description = "Remove objects blocking walkways to prevent accidents."
                )

                else -> null
            }
        }
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}

class AppViewModelFactory(
    private val scanDao: ScanDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(scanDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
