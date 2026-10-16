package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.BiometricHelper
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.GradeTrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GradeTrackerViewModel,
    onRequestBiometricAuth: (onSuccess: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupJsonContent by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }

    val isBiometricSupported = remember { BiometricHelper.isBiometricAvailable(context) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Settings & Preferences",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Personalize GradeTracker Pro, manage data sync, and configure security.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Academic Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Academic Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showEditProfileDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = AcademicBluePrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Name: ${profile?.studentName ?: "Scholar"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Degree: ${profile?.degreeName ?: "Degree Program"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("University: ${profile?.universityName ?: "University"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Grading Scale: ${profile?.gradingScale ?: "4.0"} Scale", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Default Attendance Threshold: ${profile?.defaultAttendanceThreshold?.toInt() ?: 75}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 2. Appearance & Theme (Light / Dark mode)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Appearance & Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (mode, label) ->
                            val isSelected = themeMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.toggleDarkMode(mode) },
                                label = { Text(label) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f).testTag("theme_chip_$mode")
                            )
                        }
                    }
                }
            }
        }

        // 3. Security & Biometric Authentication Lock
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric Privacy Lock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isBiometricSupported) {
                                    "Require fingerprint, face, or device PIN to access academic records."
                                } else {
                                    "Biometric hardware not configured on device."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = profile?.biometricLockEnabled == true,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    onRequestBiometricAuth {
                                        viewModel.setBiometricLock(true)
                                    }
                                } else {
                                    viewModel.setBiometricLock(false)
                                }
                            },
                            enabled = isBiometricSupported,
                            modifier = Modifier.testTag("biometric_switch")
                        )
                    }
                }
            }
        }

        // 4. Cross-Device Database Syncing & Local Backups
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Local Database Syncing & Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Transfer your semesters, courses, and attendance between devices via secure JSON backups.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.exportBackupJson { json ->
                                    backupJsonContent = json
                                    showBackupDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("export_backup_button")
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f).testTag("import_backup_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Sync", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 5. PDF Performance Report Export
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Detailed PDF Performance Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Export an official document containing your multi-semester SGPA, CGPA, attendance metrics, and future grade breakdown.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.exportPdfReport {} },
                        modifier = Modifier.fillMaxWidth().testTag("generate_pdf_report_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate & Share PDF Report")
                    }
                }
            }
        }

        // 6. Data Reset
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Demo Data Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reset all semesters, sample grades, and attendance metrics back to default demo state.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.resetDemoData() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCritical),
                        modifier = Modifier.fillMaxWidth().testTag("reset_demo_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = StatusCritical)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reload Sample Demo Data")
                    }
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var name by remember { mutableStateOf(profile?.studentName ?: "Alex Morgan") }
        var degree by remember { mutableStateOf(profile?.degreeName ?: "B.S. Computer Science") }
        var university by remember { mutableStateOf(profile?.universityName ?: "University Tech") }
        var targetCgpaStr by remember { mutableStateOf(profile?.targetCgpa?.toString() ?: "3.85") }
        var totalSemestersStr by remember { mutableStateOf(profile?.totalDegreeSemesters?.toString() ?: "8") }
        var totalCreditsStr by remember { mutableStateOf(profile?.totalDegreeCredits?.toString() ?: "128.0") }
        var gradingScale by remember { mutableStateOf(profile?.gradingScale ?: "4.0") }
        var defaultAttendanceStr by remember { mutableStateOf(profile?.defaultAttendanceThreshold?.toString() ?: "75.0") }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Academic Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Student Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = degree,
                        onValueChange = { degree = it },
                        label = { Text("Degree / Major") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = university,
                        onValueChange = { university = it },
                        label = { Text("University / College") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = targetCgpaStr,
                            onValueChange = { targetCgpaStr = it },
                            label = { Text("Target CGPA") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = defaultAttendanceStr,
                            onValueChange = { defaultAttendanceStr = it },
                            label = { Text("Min Att %") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = totalSemestersStr,
                            onValueChange = { totalSemestersStr = it },
                            label = { Text("Total Semesters") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = totalCreditsStr,
                            onValueChange = { totalCreditsStr = it },
                            label = { Text("Degree Credits") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tc = targetCgpaStr.toDoubleOrNull() ?: 3.85
                        val ts = totalSemestersStr.toIntOrNull() ?: 8
                        val cr = totalCreditsStr.toDoubleOrNull() ?: 128.0
                        val att = defaultAttendanceStr.toDoubleOrNull() ?: 75.0
                        viewModel.updateProfile(
                            name = name,
                            degree = degree,
                            university = university,
                            targetCgpa = tc,
                            totalSemesters = ts,
                            totalCredits = cr,
                            scale = gradingScale,
                            defaultAttendance = att
                        )
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Backup Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Database Sync Backup (JSON)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Copy this JSON snippet to transfer your data to another device:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupJsonContent,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("GradeTracker Backup", backupJsonContent)
                        clipboard.setPrimaryClip(clip)
                        showBackupDialog = false
                    }
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Backup Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Database Sync", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Paste the backup JSON text from your other device:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        placeholder = { Text("Paste JSON here...") },
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("import_json_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonInput.isNotBlank()) {
                            viewModel.importBackupJson(importJsonInput)
                            showImportDialog = false
                        }
                    }
                ) {
                    Text("Import & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
