package com.yugentech.mynt.ui.tabs.profileScreen.parent

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.mynt.BuildConfig
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.transactions.export.CsvExporter
import com.yugentech.mynt.transactions.insights.FinanceInsights
import com.yugentech.mynt.transactions.insights.yearMonth
import com.yugentech.mynt.ui.shared.components.BudgetDialog
import com.yugentech.mynt.ui.shared.components.MyntCard
import com.yugentech.mynt.ui.shared.util.FinanceColors
import com.yugentech.mynt.ui.shared.util.formatAmountShort
import com.yugentech.mynt.ui.tabs.profileScreen.viewmodel.SettingsViewModel
import com.yugentech.theme.tokens.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val currencies = listOf("₹", "$", "€", "£", "¥", "د.إ", "৳", "₨")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userName: String,
    userEmail: String,
    expenses: List<Expense>,
    settingsViewModel: SettingsViewModel,
    bottomPadding: Dp,
    onShowMessage: (String) -> Unit,
    onSignOutClick: () -> Unit,
    onExitClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onLentBorrowedClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by settingsViewModel.settingsConfiguration.collectAsStateWithLifecycle()
    val notifications = settings.notificationConfig

    var showBudget by remember { mutableStateOf(false) }
    var showCurrency by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }

    val streak = remember(expenses) { FinanceInsights.loggingStreak(expenses) }
    val monthsTracked = remember(expenses) { expenses.map { it.yearMonth }.distinct().size }

    // Android 13+ asks before an app may post notifications.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        settingsViewModel.setNotificationsEnabled(granted)
        if (granted) {
            settingsViewModel.setDailyReminderEnabled(true)
        } else {
            onShowMessage("Notifications are off. You can allow them in system settings.")
        }
    }

    fun enableNotifications(enabled: Boolean, onGranted: () -> Unit = {}) {
        val needsPermission = enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            settingsViewModel.setNotificationsEnabled(enabled)
            if (enabled) onGranted()
        }
    }

    fun enableDailyReminder(enabled: Boolean) {
        if (enabled) {
            enableNotifications(true) {
                settingsViewModel.setDailyReminderEnabled(true)
            }
        } else {
            settingsViewModel.setDailyReminderEnabled(false)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(CsvExporter.export(expenses, settings.currencySymbol).toByteArray())
                    } ?: error("Couldn't open the file")
                }.isSuccess
            }
            onShowMessage(if (ok) "Exported ${expenses.size} entries" else "Export failed. Try another location.")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomPadding)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Me",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(16.dp))

        // Profile with a few numbers worth being proud of
        MyntCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        userName.trim().firstOrNull()?.uppercase() ?: "M",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        userName.ifBlank { "Mynt user" },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileStat("Day streak", "$streak", FinanceColors.lent, Modifier.weight(1f))
                ProfileStat("Entries", "${expenses.size}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                ProfileStat("Months", "$monthsTracked", FinanceColors.income, Modifier.weight(1f))
            }
        }

        SettingsSection("Money") {
            SettingsRow(
                icon = Icons.Rounded.Savings,
                title = "Monthly budget",
                subtitle = if (settings.monthlyBudget > 0) formatAmountShort(settings.monthlyBudget) else "Not set",
                position = Position.First,
                onClick = { showBudget = true }
            )
            SettingsRow(
                icon = Icons.Rounded.CurrencyExchange,
                title = "Currency",
                subtitle = settings.currencySymbol,
                position = Position.Middle,
                onClick = { showCurrency = true }
            )
            SettingsRow(
                icon = Icons.Rounded.Handshake,
                title = "Lend & owe",
                subtitle = "Balances with friends and family",
                position = Position.Middle,
                onClick = onLentBorrowedClick
            )
            SettingsRow(
                icon = Icons.Rounded.FileDownload,
                title = "Export to CSV",
                subtitle = "Open your data in any spreadsheet",
                position = Position.Last,
                onClick = {
                    if (expenses.isEmpty()) onShowMessage("Nothing to export yet")
                    else exportLauncher.launch(CsvExporter.fileName())
                }
            )
        }

        SettingsSection("Appearance") {
            SettingsRow(
                icon = Icons.Rounded.Palette,
                title = "Theme & colours",
                subtitle = "Light, dark, colour palettes and fonts",
                position = Position.Single,
                onClick = onAppearanceClick
            )
        }

        SettingsSection("Reminders & feel") {
            SwitchRow(
                icon = Icons.Rounded.Notifications,
                title = "Notifications",
                subtitle = "Let Mynt send you reminders",
                checked = notifications.notificationsEnabled,
                position = Position.First,
                onCheckedChange = { enableNotifications(it) }
            )
            SwitchRow(
                icon = Icons.Rounded.Alarm,
                title = "Daily reminder",
                subtitle = "Every day at " + LocalTime.of(notifications.reminderHour, notifications.reminderMinute)
                    .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())),
                checked = notifications.dailyReminderEnabled,
                position = Position.Middle,
                onCheckedChange = ::enableDailyReminder,
                onClick = { enableNotifications(true) { showTimePicker = true } }
            )
            SwitchRow(
                icon = Icons.Rounded.AutoAwesome,
                title = "Playful reminders",
                subtitle = "Cheeky nudges instead of plain ones",
                checked = notifications.playfulRemindersEnabled,
                enabled = notifications.notificationsEnabled && notifications.dailyReminderEnabled,
                position = Position.Middle,
                onCheckedChange = settingsViewModel::setPlayfulRemindersEnabled
            )
            SwitchRow(
                icon = Icons.Rounded.Vibration,
                title = "Haptic feedback",
                subtitle = "Subtle vibrations on taps and saves",
                checked = settings.hapticsEnabled,
                position = Position.Last,
                onCheckedChange = settingsViewModel::setHapticsEnabled
            )
        }

        SettingsSection("About") {
            SettingsRow(
                icon = Icons.Rounded.Info,
                title = "What's new",
                subtitle = "Version ${BuildConfig.VERSION_NAME}",
                position = Position.First,
                onClick = { showWhatsNew = true }
            )
            SettingsRow(
                icon = Icons.Rounded.Share,
                title = "Share Mynt",
                subtitle = "Tell a friend who loses track of money",
                position = Position.Middle,
                onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, AppConstants.SHARE_MESSAGE)
                    }
                    context.startActivity(Intent.createChooser(share, "Share Mynt"))
                }
            )
            SettingsRow(
                icon = Icons.Rounded.Star,
                title = "Rate Mynt",
                subtitle = "It genuinely helps",
                position = Position.Last,
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.MARKET_URL)))
                    } catch (_: ActivityNotFoundException) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.PLAY_STORE_URL)))
                    }
                }
            )
        }

        SettingsSection("Session") {
            SettingsRow(
                icon = Icons.AutoMirrored.Rounded.Logout,
                title = "Sign out",
                subtitle = null,
                tint = MaterialTheme.colorScheme.error,
                position = Position.First,
                onClick = { confirmSignOut = true }
            )
            SettingsRow(
                icon = Icons.AutoMirrored.Rounded.ExitToApp,
                title = "Close app",
                subtitle = null,
                position = Position.Last,
                onClick = onExitClick
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Mynt ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    if (showBudget) {
        BudgetDialog(
            currentBudget = settings.monthlyBudget,
            onSave = {
                settingsViewModel.setMonthlyBudget(it)
                showBudget = false
            },
            onDismiss = { showBudget = false }
        )
    }

    if (showCurrency) {
        AlertDialog(
            onDismissRequest = { showCurrency = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Currency") },
            text = {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currencies.forEach { symbol ->
                        FilterChip(
                            selected = settings.currencySymbol == symbol,
                            onClick = {
                                settingsViewModel.setCurrencySymbol(symbol)
                                showCurrency = false
                            },
                            label = { Text(symbol, style = MaterialTheme.typography.titleMedium) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCurrency = false }) { Text("Done") } }
        )
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = notifications.reminderHour,
            initialMinute = notifications.reminderMinute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Remind me at") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    enableNotifications(true) {
                        settingsViewModel.setReminderTime(state.hour, state.minute)
                        settingsViewModel.setDailyReminderEnabled(true)
                    }
                    showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

    if (showWhatsNew) {
        AlertDialog(
            onDismissRequest = { showWhatsNew = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("What's new in ${BuildConfig.VERSION_NAME}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "A living voice orb and a frosted floating navigation bar",
                        "Monthly budgets with a safe-to-spend-per-day figure",
                        "Insights: category breakdown, daily spending and 6-month trends",
                        "Lend & owe balances per person, with one-tap settle up",
                        "History grouped by day, search across every month, swipe to delete with undo",
                        "Daily reminders that actually remind you, plus CSV export"
                    ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = { TextButton(onClick = { showWhatsNew = false }) { Text("Nice") } }
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Sign out?") },
            text = { Text("Your entries stay on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    onSignOutClick()
                }) { Text("Sign out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } }
        )
    }
}

private enum class Position { Single, First, Middle, Last }

/** Grouped-list corners: big on the outside of a group, small between its rows. */
private fun Position.shape(): Shape {
    val big = 24.dp
    val small = 6.dp
    return when (this) {
        Position.Single -> RoundedCornerShape(big)
        Position.First -> RoundedCornerShape(topStart = big, topEnd = big, bottomStart = small, bottomEnd = small)
        Position.Middle -> RoundedCornerShape(small)
        Position.Last -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = big, bottomEnd = big)
    }
}

@Composable
private fun ProfileStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 6.dp, top = 24.dp, bottom = 8.dp)
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { content() }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    position: Position,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        shape = position.shape(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        RowContent(icon = icon, title = title, subtitle = subtitle, tint = tint) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    position: Position,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { if (onClick != null) onClick() else onCheckedChange(!checked) },
        enabled = enabled,
        shape = position.shape(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        RowContent(
            icon = icon,
            title = title,
            subtitle = subtitle,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f)
        ) {
            Switch(checked = checked && enabled, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

@Composable
private fun RowContent(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    tint: Color,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = tint)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing()
    }
}
