package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.BiometricHelper
import com.example.service.NotificationHelper
import com.example.ui.screens.*
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.GradeTrackerProTheme
import com.example.ui.viewmodel.GradeTrackerViewModel
import com.example.ui.viewmodel.ScreenDestination
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)

        setContent {
            val viewModel: GradeTrackerViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val isAppLocked by viewModel.isAppLocked.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()

            val darkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            GradeTrackerProTheme(darkTheme = darkTheme) {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    viewModel.statusMessage.collectLatest { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                if (isAppLocked) {
                    BiometricLockScreen(
                        onUnlockRequested = {
                            BiometricHelper.authenticate(
                                activity = this@MainActivity,
                                title = "Unlock GradeTracker Pro",
                                subtitle = "Confirm identity to access student grades",
                                onSuccess = { viewModel.unlockApp() },
                                onError = { errMsg ->
                                    scope.launch { snackbarHostState.showSnackbar(errMsg) }
                                }
                            )
                        }
                    )
                } else {
                    MainAppScaffold(
                        viewModel = viewModel,
                        currentScreen = currentScreen,
                        snackbarHostState = snackbarHostState,
                        onRequestBiometricAuth = { onSuccess ->
                            BiometricHelper.authenticate(
                                activity = this@MainActivity,
                                title = "Confirm Security Setting",
                                subtitle = "Authenticate to toggle biometric privacy lock",
                                onSuccess = onSuccess,
                                onError = { errMsg ->
                                    scope.launch { snackbarHostState.showSnackbar(errMsg) }
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: GradeTrackerViewModel,
    currentScreen: ScreenDestination,
    snackbarHostState: SnackbarHostState,
    onRequestBiometricAuth: (onSuccess: () -> Unit) -> Unit
) {
    // BackHandler to navigate back to Dashboard if on a sub-screen
    BackHandler(enabled = currentScreen !is ScreenDestination.Dashboard) {
        viewModel.navigateTo(ScreenDestination.Dashboard)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            GradeTrackerBottomBar(
                currentScreen = currentScreen,
                onSelectScreen = { viewModel.navigateTo(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                is ScreenDestination.Dashboard -> DashboardScreen(viewModel = viewModel)
                is ScreenDestination.Semesters -> SemestersScreen(viewModel = viewModel)
                is ScreenDestination.Attendance -> AttendanceScreen(viewModel = viewModel)
                is ScreenDestination.TargetPlanner -> TargetPlannerScreen(viewModel = viewModel)
                is ScreenDestination.AdvisorChat -> AdvisorChatScreen(viewModel = viewModel)
                is ScreenDestination.VisionBoard -> VisionBoardScreen(viewModel = viewModel)
                is ScreenDestination.Settings -> SettingsScreen(
                    viewModel = viewModel,
                    onRequestBiometricAuth = onRequestBiometricAuth
                )
            }
        }
    }
}

data class NavItem(
    val destination: ScreenDestination,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun GradeTrackerBottomBar(
    currentScreen: ScreenDestination,
    onSelectScreen: (ScreenDestination) -> Unit
) {
    val items = listOf(
        NavItem(ScreenDestination.Dashboard, "Home", Icons.Default.Dashboard, "nav_dashboard"),
        NavItem(ScreenDestination.Semesters, "Grades", Icons.Default.School, "nav_semesters"),
        NavItem(ScreenDestination.Attendance, "Attendance", Icons.Default.CheckCircle, "nav_attendance"),
        NavItem(ScreenDestination.TargetPlanner, "Planner", Icons.AutoMirrored.Filled.TrendingUp, "nav_planner"),
        NavItem(ScreenDestination.AdvisorChat, "Advisor", Icons.Default.AutoAwesome, "nav_advisor"),
        NavItem(ScreenDestination.VisionBoard, "Vision", Icons.Default.Palette, "nav_vision"),
        NavItem(ScreenDestination.Settings, "Settings", Icons.Default.Settings, "nav_settings")
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.testTag("bottom_navigation_bar")
    ) {
        items.forEach { item ->
            val isSelected = currentScreen::class == item.destination::class
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectScreen(item.destination) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}

@Composable
fun BiometricLockScreen(onUnlockRequested: () -> Unit) {
    LaunchedEffect(Unit) {
        onUnlockRequested()
    }

    Surface(
        modifier = Modifier.fillMaxSize().testTag("biometric_lock_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(AcademicBluePrimary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Lock",
                    tint = AcademicBluePrimary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "GradeTracker Pro is Locked",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your academic records and personal grades are protected by biometric privacy authentication.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onUnlockRequested,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("unlock_biometric_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock with Fingerprint / PIN")
            }
        }
    }
}
