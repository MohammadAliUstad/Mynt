package com.yugentech.mynt.user.datastore

import com.yugentech.mynt.domain.model.HeroCardKind
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yugentech.mynt.domain.model.CardAnimationStyle
import com.yugentech.mynt.domain.model.NotificationConfig
import com.yugentech.mynt.domain.model.SettingsConfiguration
import com.yugentech.theme.tokens.AppConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

class UserDataStore(
    private val dataStore: DataStore<Preferences>
) {

    // Key names match the pre-refactor SettingsManager so saved preferences carry over
    companion object {
        private val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        private val PLAYFUL_REMINDERS_ENABLED = booleanPreferencesKey("playful_reminder_enabled")
        private val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        private val MONTHLY_BUDGET = doublePreferencesKey("monthly_budget")
        private val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        private val CARD_ANIMATION_STYLE = stringPreferencesKey("card_animation_style")
        private val HIDDEN_HERO_CARDS = stringSetPreferencesKey("hidden_hero_cards")
    }

    val settingsConfiguration: Flow<SettingsConfiguration> = dataStore.data
        .catch {
            Timber.e(it, "Error reading settings preferences")
            emit(emptyPreferences())
        }
        .map { prefs ->
            SettingsConfiguration(
                currencySymbol = prefs[CURRENCY_SYMBOL] ?: AppConstants.DEFAULT_CURRENCY_SYMBOL,
                hapticsEnabled = prefs[HAPTICS_ENABLED] ?: true,
                monthlyBudget = prefs[MONTHLY_BUDGET] ?: 0.0,
                cardAnimationStyle = CardAnimationStyle.entries
                    .firstOrNull { it.name == prefs[CARD_ANIMATION_STYLE] }
                    ?: CardAnimationStyle.WATER,
                hiddenHeroCards = prefs[HIDDEN_HERO_CARDS].orEmpty()
                    .mapNotNull { name -> HeroCardKind.entries.firstOrNull { it.name == name } }
                    .toSet(),
                notificationConfig = NotificationConfig(
                    notificationsEnabled = prefs[NOTIFICATIONS_ENABLED] ?: true,
                    dailyReminderEnabled = prefs[DAILY_REMINDER_ENABLED] ?: true,
                    playfulRemindersEnabled = prefs[PLAYFUL_REMINDERS_ENABLED] ?: true,
                    reminderHour = prefs[REMINDER_HOUR] ?: 20,
                    reminderMinute = prefs[REMINDER_MINUTE] ?: 0
                )
            )
        }

    suspend fun setCurrencySymbol(symbol: String) {
        dataStore.edit { it[CURRENCY_SYMBOL] = symbol }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[DAILY_REMINDER_ENABLED] = enabled }
    }

    suspend fun setPlayfulRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[PLAYFUL_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[HAPTICS_ENABLED] = enabled }
    }

    suspend fun setCardAnimationStyle(style: CardAnimationStyle) {
        dataStore.edit { it[CARD_ANIMATION_STYLE] = style.name }
    }

    suspend fun setHeroCardVisible(card: HeroCardKind, visible: Boolean) {
        dataStore.edit { prefs ->
            val hidden = prefs[HIDDEN_HERO_CARDS].orEmpty().toMutableSet()
            if (visible) hidden.remove(card.name) else hidden.add(card.name)
            // Home always keeps at least one card, so switching off the last one is ignored.
            if (HeroCardKind.entries.all { it.name in hidden }) hidden.remove(HeroCardKind.MONTH.name)
            prefs[HIDDEN_HERO_CARDS] = hidden
        }
    }

    suspend fun setMonthlyBudget(amount: Double) {
        dataStore.edit { it[MONTHLY_BUDGET] = amount.coerceAtLeast(0.0) }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }
}