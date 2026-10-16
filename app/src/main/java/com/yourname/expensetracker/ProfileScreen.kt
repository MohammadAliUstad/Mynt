package com.yourname.expensetracker

import android.app.Activity
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Profile ("Me") Screen modeled directly on Quill's MoreScreen design.
 * Features Profile Card, Insights & Streak buttons, AI, Appearance,
 * Notifications & Haptics, About, and Session sections.
 */
@Composable
fun ProfileScreen(
    onSignOutClick: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val userName = AuthManager.userDisplayName
    val userEmail = AuthManager.userEmail

    var reminderHour by remember { mutableStateOf(20) }
    var reminderMinute by remember { mutableStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }

    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                reminderHour = hourOfDay
                reminderMinute = minute
            },
            reminderHour,
            reminderMinute,
            false
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionColor = MaterialTheme.colorScheme.primary,
                    dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. PROFILE CARD (Modeled on Quill ProfileCard)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(GoldAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Insights Button
                        Button(
                            onClick = onNavigateToInsights,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Insights", fontWeight = FontWeight.SemiBold)
                        }

                        // Streak Button
                        Button(
                            onClick = {},
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("5 Days", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. AI SECTION
            SectionHeader(icon = Icons.Default.AutoAwesome, title = "AI")
            SettingsListItem(
                title = "Manager",
                subtitle = "AI Assistant & Expense Manager",
                index = 0,
                totalCount = 1,
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("AI Manager is active")
                    }
                }
            )

            // 3. APPEARANCE SECTION
            SectionHeader(icon = Icons.Default.Palette, title = "Appearance")
            SettingsListItem(
                title = "Theme & Colors",
                subtitle = "Customize your app's look and feel",
                index = 0,
                totalCount = 1,
                onClick = onNavigateToSettings
            )

            // 4. NOTIFICATIONS & HAPTICS SECTION
            SectionHeader(icon = Icons.Default.Notifications, title = "Notifications & Haptics")
            SettingsSwitchItem(
                title = "Enable Notifications",
                subtitle = "Allow ExpenseTracker to send you updates",
                checked = SettingsManager.notificationsEnabled,
                index = 0,
                totalCount = 4,
                onCheckedChange = { enabled ->
                    SettingsManager.updateNotificationsEnabled(context, enabled)
                }
            )
            SettingsSwitchItem(
                title = "Daily Entry Reminder",
                subtitle = String.format(Locale.getDefault(), "%02d:%02d PM", if (reminderHour > 12) reminderHour - 12 else reminderHour, reminderMinute),
                checked = SettingsManager.dailyReminderEnabled,
                enabled = SettingsManager.notificationsEnabled,
                index = 1,
                totalCount = 4,
                onCheckedChange = { isChecked ->
                    SettingsManager.updateDailyReminderEnabled(context, isChecked)
                    if (isChecked) timePickerDialog.show()
                },
                onClick = {
                    timePickerDialog.show()
                }
            )
            SettingsSwitchItem(
                title = "Playful Reminders",
                subtitle = "Occasional friendly nudges to keep your tracking habit alive",
                checked = SettingsManager.playfulRemindersEnabled,
                enabled = SettingsManager.notificationsEnabled,
                index = 2,
                totalCount = 4,
                onCheckedChange = { isChecked ->
                    SettingsManager.updatePlayfulRemindersEnabled(context, isChecked)
                }
            )
            SettingsSwitchItem(
                title = "Haptic Feedback",
                subtitle = "Feel vibrations throughout the app",
                checked = SettingsManager.hapticsEnabled,
                index = 3,
                totalCount = 4,
                onCheckedChange = { isChecked ->
                    SettingsManager.updateHapticsEnabled(context, isChecked)
                }
            )

            // 5. ABOUT SECTION
            SectionHeader(icon = Icons.Default.Info, title = "About")
            SettingsListItem(
                title = "About ExpenseTracker",
                subtitle = "Version 1.1.0",
                index = 0,
                totalCount = 2,
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("ExpenseTracker v1.1.0 • Built with Jetpack Compose & Firebase")
                    }
                }
            )
            SettingsListItem(
                title = "What's New",
                subtitle = "See the latest changes and features",
                index = 1,
                totalCount = 2,
                onClick = { showWhatsNewDialog = true }
            )

            // 6. SESSION SECTION
            SectionHeader(icon = Icons.AutoMirrored.Filled.ExitToApp, title = "Session")
            SettingsListItem(
                title = "Exit",
                subtitle = "Close the ExpenseTracker app",
                index = 0,
                totalCount = 2,
                onClick = {
                    (context as? Activity)?.finish()
                }
            )
            SettingsListItem(
                title = "Sign Out",
                subtitle = "Log out of your current account",
                index = 1,
                totalCount = 2,
                onClick = {
                    AuthManager.signOut(context)
                    onSignOutClick()
                }
            )
        }

        // WHAT'S NEW DIALOG
        if (showWhatsNewDialog) {
            AlertDialog(
                onDismissRequest = { showWhatsNewDialog = false },
                shape = RoundedCornerShape(20.dp),
                title = { Text("What's New in v1.1.0", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("• Soniox Cloud AI Speech-to-Text Integration")
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Real-Time Firebase Cloud Firestore Sync")
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Glassmorphic Floating Navigation Bar")
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Dedicated Theme & Typography Settings")
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Recent Transactions Downward Expansion & Editing")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showWhatsNewDialog = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Awesome!")
                    }
                }
            )
        }
    }
}

/**
 * Calculates item shape for grouped card items modeled on Quill's itemShape.
 */
@Composable
private fun itemShape(index: Int, count: Int): Shape {
    val largeCorner = 20.dp
    val smallCorner = 4.dp

    return when {
        count == 1 -> RoundedCornerShape(largeCorner)
        index == 0 -> RoundedCornerShape(
            topStart = largeCorner,
            topEnd = largeCorner,
            bottomStart = smallCorner,
            bottomEnd = smallCorner
        )

        index == count - 1 -> RoundedCornerShape(
            topStart = smallCorner,
            topEnd = smallCorner,
            bottomStart = largeCorner,
            bottomEnd = largeCorner
        )

        else -> RoundedCornerShape(smallCorner)
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 6.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SettingsListItem(
    title: String,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    index: Int,
    totalCount: Int,
    onClick: () -> Unit
) {
    val shape = itemShape(index, totalCount)

    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        },
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    index: Int,
    totalCount: Int,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    val shape = itemShape(index, totalCount)

    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = { onClick?.invoke() ?: onCheckedChange(!checked) }),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Preview(showBackground = true, name = "Profile Screen Preview")
@Composable
fun ProfileScreenPreview() {
    ExpenseTrackerTheme {
        ProfileScreen()
    }
}
