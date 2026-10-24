package com.yugentech.mynt.domain.model

data class NotificationConfig(
    val notificationsEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val playfulRemindersEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0
)

/** The cards you can swipe between at the top of Home, in the order they appear. */
enum class HeroCardKind(val label: String, val description: String) {
    MONTH("This month", "Net balance, income and spending"),
    LEND_OWE("Lend & owe", "What you're owed and what you owe"),
    TODAY("Today", "Today's spending against yesterday"),
    TOP_SPENDING("Top spending", "Your biggest categories this month"),
    VS_LAST_MONTH("Vs last month", "This month's spending against last month")
}

/** How the Home balance cards animate behind their text. Chosen under Theme settings. */
enum class CardAnimationStyle(val label: String) {
    WATER("Water tank"),
    AURORA("Aurora glow"),
    SHIMMER("Shimmer"),
    NONE("Off")
}

data class SettingsConfiguration(
    val currencySymbol: String = "₹",
    val hapticsEnabled: Boolean = true,
    // 0 means no budget has been set.
    val monthlyBudget: Double = 0.0,
    val notificationConfig: NotificationConfig = NotificationConfig(),
    val cardAnimationStyle: CardAnimationStyle = CardAnimationStyle.WATER,
    // Cards switched off under Theme settings. Home always keeps at least one.
    val hiddenHeroCards: Set<HeroCardKind> = emptySet()
)