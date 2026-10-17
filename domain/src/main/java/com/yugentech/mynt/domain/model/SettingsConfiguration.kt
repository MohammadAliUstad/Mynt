package com.yugentech.mynt.domain.model

data class NotificationConfig(
    val notificationsEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val playfulRemindersEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0
)

data class SettingsConfiguration(
    val currencySymbol: String = "₹",
    val hapticsEnabled: Boolean = true,
    // 0 means no budget has been set.
    val monthlyBudget: Double = 0.0,
    val notificationConfig: NotificationConfig = NotificationConfig()
)
