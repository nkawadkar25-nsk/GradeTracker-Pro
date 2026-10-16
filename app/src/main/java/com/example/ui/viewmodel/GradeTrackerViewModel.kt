package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ChatMessage
import com.example.data.api.GeminiClient
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.GradeTrackerRepository
import com.example.service.BiometricHelper
import com.example.service.NotificationHelper
import com.example.service.PdfReportGenerator
import com.example.service.TtsAudioHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

sealed class ScreenDestination {
    object Dashboard : ScreenDestination()
    object Semesters : ScreenDestination()
    object Attendance : ScreenDestination()
    object TargetPlanner : ScreenDestination()
    object AdvisorChat : ScreenDestination()
    object VisionBoard : ScreenDestination()
    object Settings : ScreenDestination()
}

class GradeTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = GradeTrackerRepository(database)
    val ttsHelper = TtsAudioHelper(application)

    // Current navigation tab
    private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.Dashboard)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    // Biometric lock state
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Dark mode state ("SYSTEM", "LIGHT", "DARK")
    private val _themeMode = MutableStateFlow("SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // Room Flows
    val semesters = repository.semesters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCourses = repository.allCourses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val profile = repository.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val schedules = repository.schedules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Semester for detail view
    private val _selectedSemesterId = MutableStateFlow<Long?>(null)
    val selectedSemesterId: StateFlow<Long?> = _selectedSemesterId.asStateFlow()

    // Dashboard Semester Sort Order
    private val _dashboardSortOrder = MutableStateFlow(GradeUtils.SemesterSortOrder.CHRONOLOGICAL_ASC)
    val dashboardSortOrder: StateFlow<GradeUtils.SemesterSortOrder> = _dashboardSortOrder.asStateFlow()

    fun setDashboardSortOrder(order: GradeUtils.SemesterSortOrder) {
        _dashboardSortOrder.value = order
    }

    // Status snackbar/toast messages
    private val _statusMessage = MutableSharedFlow<String>()
    val statusMessage = _statusMessage.asSharedFlow()

    // Chatbot state
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "model",
                content = "Hello! I am your AI Academic Advisor & Study Coach. I can analyze your current CGPA, recommend study strategies for low-attendance subjects, and help you reach your graduation target. How can I help you today?"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // TTS Speaking state
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // Image Generator state (gemini-3-pro-image-preview)
    private val _generatedImage = MutableStateFlow<Bitmap?>(null)
    val generatedImage: StateFlow<Bitmap?> = _generatedImage.asStateFlow()

    private val _isImageLoading = MutableStateFlow(false)
    val isImageLoading: StateFlow<Boolean> = _isImageLoading.asStateFlow()

    private val _imageSizeOption = MutableStateFlow("1K") // "1K", "2K", "4K"
    val imageSizeOption: StateFlow<String> = _imageSizeOption.asStateFlow()

    init {
        viewModelScope.launch {
            repository.profile.collectLatest { p ->
                if (p != null) {
                    _themeMode.value = p.darkModePreference
                    if (p.biometricLockEnabled && BiometricHelper.isBiometricAvailable(getApplication())) {
                        _isAppLocked.value = true
                    }
                }
            }
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun navigateTo(screen: ScreenDestination) {
        _currentScreen.value = screen
    }

    fun selectSemester(id: Long?) {
        _selectedSemesterId.value = id
    }

    fun toggleDarkMode(mode: String) {
        _themeMode.value = mode
        viewModelScope.launch {
            val curr = repository.getProfileSync()
            repository.updateProfile(curr.copy(darkModePreference = mode))
        }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            val curr = repository.getProfileSync()
            repository.updateProfile(curr.copy(biometricLockEnabled = enabled))
            if (enabled) {
                _statusMessage.emit("Biometric privacy lock activated.")
            } else {
                _statusMessage.emit("Biometric lock disabled.")
            }
        }
    }

    fun updateProfile(
        name: String,
        degree: String,
        university: String,
        targetCgpa: Double,
        totalSemesters: Int,
        totalCredits: Double,
        scale: String,
        defaultAttendance: Double
    ) {
        viewModelScope.launch {
            val curr = repository.getProfileSync()
            val updated = curr.copy(
                studentName = name,
                degreeName = degree,
                universityName = university,
                targetCgpa = targetCgpa,
                totalDegreeSemesters = totalSemesters,
                totalDegreeCredits = totalCredits,
                gradingScale = scale,
                defaultAttendanceThreshold = defaultAttendance
            )
            repository.updateProfile(updated)
            _statusMessage.emit("Academic profile updated.")
        }
    }

    fun addSemester(number: Int, name: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.addSemester(
                SemesterEntity(
                    semesterNumber = number,
                    semesterName = name,
                    isCompleted = isCompleted,
                    orderIndex = number
                )
            )
            _statusMessage.emit("Semester added successfully.")
        }
    }

    fun deleteSemester(semester: SemesterEntity) {
        viewModelScope.launch {
            repository.deleteSemester(semester)
            if (_selectedSemesterId.value == semester.id) {
                _selectedSemesterId.value = null
            }
            _statusMessage.emit("Semester removed.")
        }
    }

    fun saveCourse(course: CourseEntity) {
        viewModelScope.launch {
            if (course.id == 0L) {
                repository.addCourse(course)
                _statusMessage.emit("Course added.")
            } else {
                repository.updateCourse(course)
                _statusMessage.emit("Course updated.")
            }
        }
    }

    fun deleteCourse(course: CourseEntity) {
        viewModelScope.launch {
            repository.deleteCourse(course)
            _statusMessage.emit("Course removed.")
        }
    }

    fun recordAttendance(courseId: Long, attended: Boolean) {
        viewModelScope.launch {
            repository.markAttendance(courseId, attended)
            _statusMessage.emit(if (attended) "Attended class recorded! 🎉" else "Missed class recorded.")
        }
    }

    fun triggerTestNotification(courseName: String, startTime: String, room: String, attendancePct: Double) {
        NotificationHelper.showUpcomingClassNotification(
            context = getApplication(),
            courseName = courseName,
            startTime = startTime,
            room = room,
            attendancePct = attendancePct,
            isAttendanceCritical = attendancePct < 75.0
        )
        viewModelScope.launch {
            _statusMessage.emit("Class reminder notification sent!")
        }
    }

    // PDF Performance Report
    fun exportPdfReport(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val prof = repository.getProfileSync()
                val semList = semesters.value
                val courseList = allCourses.value
                val pairs = semList.map { sem ->
                    Pair(sem, courseList.filter { it.semesterId == sem.id })
                }
                val result = PdfReportGenerator.generateAndShareReport(
                    context = getApplication(),
                    profile = prof,
                    semestersWithCourses = pairs
                )
                if (result.isSuccess) {
                    _statusMessage.emit("PDF report generated successfully.")
                    onComplete(true)
                } else {
                    _statusMessage.emit("Failed to generate PDF: ${result.exceptionOrNull()?.message}")
                    onComplete(false)
                }
            } catch (e: Exception) {
                _statusMessage.emit("Export error: ${e.message}")
                onComplete(false)
            }
        }
    }

    // JSON Sync Export
    fun exportBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDatabaseToJson()
            onResult(json)
            _statusMessage.emit("Database backup exported.")
        }
    }

    // JSON Sync Import
    fun importBackupJson(jsonString: String) {
        viewModelScope.launch {
            val success = repository.importDatabaseFromJson(jsonString)
            if (success) {
                _statusMessage.emit("Database synced successfully! All data updated.")
            } else {
                _statusMessage.emit("Import failed: Invalid JSON format.")
            }
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.resetToDefaultDemo()
            _statusMessage.emit("Sample demo data reloaded.")
        }
    }

    // Gemini Chatbot Integration (gemini-3.8-flash)
    fun sendUserMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage(role = "user", content = text.trim())
        val updatedList = _chatMessages.value + userMsg
        _chatMessages.value = updatedList
        _isChatLoading.value = true

        viewModelScope.launch {
            // Build rich contextual system prompt from user's current academic state
            val prof = repository.getProfileSync()
            val semList = semesters.value
            val courses = allCourses.value
            val map = semList.associateWith { s -> courses.filter { it.semesterId == s.id } }
            val currentCgpa = GradeUtils.calculateCgpa(map)
            val lowAttendanceCourses = courses.filter { it.attendancePercentage < it.requiredAttendancePercentage }

            val lowAttStr = if (lowAttendanceCourses.isNotEmpty()) {
                "Courses with attendance alerts: " + lowAttendanceCourses.joinToString { "${it.courseName} (${String.format("%.1f", it.attendancePercentage)}%)" }
            } else {
                "All courses have satisfactory attendance."
            }

            val systemInstruction = """
                You are GradeTracker Pro's AI Academic Advisor & Study Coach.
                Student Name: ${prof.studentName}
                Degree: ${prof.degreeName} (${prof.universityName})
                Current CGPA: ${String.format("%.2f", currentCgpa)} / ${prof.gradingScale}
                Target CGPA: ${String.format("%.2f", prof.targetCgpa)}
                $lowAttStr
                
                Provide highly encouraging, concrete academic guidance. Calculate required study strategies, exam prep plans, and attendance recovery advice. Keep replies structured and clear with bullet points when explaining grade targets.
            """.trimIndent()

            val result = GeminiClient.sendChatMessage(updatedList, systemInstruction)
            _isChatLoading.value = false

            if (result.isSuccess) {
                val reply = result.getOrNull() ?: "I'm here to help with your academic success!"
                _chatMessages.value = _chatMessages.value + ChatMessage(role = "model", content = reply)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Unable to contact AI Advisor."
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    role = "model",
                    content = "Academic Advisor Note: $err"
                )
            }
        }
    }

    // TTS Speaking (gemini-3.8-flash-tts)
    fun speakBriefing(text: String) {
        _isSpeaking.value = true
        ttsHelper.speakText(
            scope = viewModelScope,
            text = text,
            onStart = { _isSpeaking.value = true },
            onDone = { _isSpeaking.value = false },
            onError = {
                _isSpeaking.value = false
                viewModelScope.launch {
                    _statusMessage.emit("Audio: $it")
                }
            }
        )
    }

    fun stopSpeaking() {
        ttsHelper.stopPlayback()
        _isSpeaking.value = false
    }

    // Vision Board & Motivational Image Generation (gemini-3-pro-image-preview)
    fun setImageSizeOption(size: String) {
        _imageSizeOption.value = size
    }

    fun generateVisionArt(customTheme: String = "") {
        _isImageLoading.value = true
        viewModelScope.launch {
            val prof = repository.getProfileSync()
            val prompt = if (customTheme.isNotBlank()) {
                customTheme
            } else {
                "A breathtaking, high-aesthetic academic vision board celebrating graduation and academic excellence in ${prof.degreeName}, with a golden degree diploma, honor cords, library study desk, glowing digital analytics holographic chart, inspiring motivational atmosphere, ultra-detailed 8k render."
            }

            val result = GeminiClient.generateImage(
                prompt = prompt,
                imageSize = _imageSizeOption.value,
                aspectRatio = "1:1"
            )
            _isImageLoading.value = false
            if (result.isSuccess) {
                _generatedImage.value = result.getOrNull()
                _statusMessage.emit("Vision artwork generated successfully in ${_imageSizeOption.value} resolution!")
            } else {
                _statusMessage.emit("Image generation note: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.destroy()
    }
}
