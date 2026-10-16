package com.yourname.expensetracker

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

// DataStore Extension
private val Context.dataStore by preferencesDataStore(name = "expense_tracker_preferences")

/**
 * App theme mode options.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Font Family selection options.
 */
enum class AppFontOption(val displayName: String, val fontFamily: FontFamily) {
    DEFAULT("Default (Sans)", FontFamily.Default),
    SERIF("Serif", FontFamily.Serif),
    MONOSPACE("Monospace", FontFamily.Monospace),
    CURSIVE("Cursive", FontFamily.Cursive)
}

/**
 * Accent Color Theme options.
 */
enum class ColorAccentTheme(val displayName: String, val primaryHex: Long) {
    GOLD("Gold", 0xFFFFB800),
    EMERALD("Emerald", 0xFF2E7D32),
    OCEAN("Ocean Blue", 0xFF1976D2),
    PURPLE("Royal Purple", 0xFF673AB7)
}

/**
 * Persistent manager for user preferences (Theme, AMOLED Dark, Font, Currency, Notifications, Reminders, Haptics).
 */
object SettingsManager {

    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_IS_AMOLED = booleanPreferencesKey("is_amoled_mode")
    private val KEY_FONT_OPTION = stringPreferencesKey("font_option")
    private val KEY_ACCENT_THEME = stringPreferencesKey("accent_theme")
    private val KEY_CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
    private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    private val KEY_DAILY_REMINDER = booleanPreferencesKey("daily_reminder_enabled")
    private val KEY_PLAYFUL_REMINDER = booleanPreferencesKey("playful_reminder_enabled")
    private val KEY_HAPTICS = booleanPreferencesKey("haptics_enabled")

    private val scope = CoroutineScope(Dispatchers.IO)

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    var isAmoledMode by mutableStateOf(false)
        private set

    var fontOption by mutableStateOf(AppFontOption.DEFAULT)
        private set

    var accentTheme by mutableStateOf(ColorAccentTheme.GOLD)
        private set

    var currencySymbol by mutableStateOf("₹")
        private set

    var notificationsEnabled by mutableStateOf(true)
        private set

    var dailyReminderEnabled by mutableStateOf(true)
        private set

    var playfulRemindersEnabled by mutableStateOf(true)
        private set

    var hapticsEnabled by mutableStateOf(true)
        private set

    /**
     * Initializes preferences from DataStore on app startup.
     */
    fun init(context: Context) {
        val applicationContext = context.applicationContext
        scope.launch {
            try {
                val prefs = applicationContext.dataStore.data.first()

                val themeSaved = prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
                themeMode = try {
                    ThemeMode.valueOf(themeSaved)
                } catch (e: Exception) {
                    ThemeMode.SYSTEM
                }

                isAmoledMode = prefs[KEY_IS_AMOLED] ?: false

                val fontSaved = prefs[KEY_FONT_OPTION] ?: AppFontOption.DEFAULT.name
                fontOption = try {
                    AppFontOption.valueOf(fontSaved)
                } catch (e: Exception) {
                    AppFontOption.DEFAULT
                }

                val accentSaved = prefs[KEY_ACCENT_THEME] ?: ColorAccentTheme.GOLD.name
                accentTheme = try {
                    ColorAccentTheme.valueOf(accentSaved)
                } catch (e: Exception) {
                    ColorAccentTheme.GOLD
                }

                currencySymbol = prefs[KEY_CURRENCY_SYMBOL] ?: "₹"
                notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true
                dailyReminderEnabled = prefs[KEY_DAILY_REMINDER] ?: true
                playfulRemindersEnabled = prefs[KEY_PLAYFUL_REMINDER] ?: true
                hapticsEnabled = prefs[KEY_HAPTICS] ?: true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Updates theme mode persistently.
     */
    fun updateThemeMode(context: Context, mode: ThemeMode) {
        themeMode = mode
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_THEME_MODE] = mode.name
            }
        }
    }

    /**
     * Updates AMOLED mode persistently.
     */
    fun updateAmoledMode(context: Context, isAmoled: Boolean) {
        isAmoledMode = isAmoled
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_IS_AMOLED] = isAmoled
            }
        }
    }

    /**
     * Updates font family selection persistently.
     */
    fun updateFontOption(context: Context, font: AppFontOption) {
        fontOption = font
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_FONT_OPTION] = font.name
            }
        }
    }

    /**
     * Updates accent color theme persistently.
     */
    fun updateAccentTheme(context: Context, theme: ColorAccentTheme) {
        accentTheme = theme
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_ACCENT_THEME] = theme.name
            }
        }
    }

    /**
     * Updates currency symbol persistently.
     */
    fun updateCurrencySymbol(context: Context, symbol: String) {
        currencySymbol = symbol
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_CURRENCY_SYMBOL] = symbol
            }
        }
    }

    /**
     * Updates notifications enabled state persistently.
     */
    fun updateNotificationsEnabled(context: Context, enabled: Boolean) {
        notificationsEnabled = enabled
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_NOTIFICATIONS] = enabled
            }
        }
    }

    fun updateDailyReminderEnabled(context: Context, enabled: Boolean) {
        dailyReminderEnabled = enabled
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_DAILY_REMINDER] = enabled
            }
        }
    }

    fun updatePlayfulRemindersEnabled(context: Context, enabled: Boolean) {
        playfulRemindersEnabled = enabled
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_PLAYFUL_REMINDER] = enabled
            }
        }
    }

    fun updateHapticsEnabled(context: Context, enabled: Boolean) {
        hapticsEnabled = enabled
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_HAPTICS] = enabled
            }
        }
    }

    /**
     * Helper to format double amount with active currency symbol.
     */
    fun formatAmount(amount: Double): String {
        return String.format(Locale.getDefault(), "%s%,.2f", currencySymbol, amount)
    }
}
