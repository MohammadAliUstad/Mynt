package com.yugentech.mynt.ui.tabs.profileScreen.viewmodel

import com.yugentech.mynt.domain.model.HeroCardKind
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.mynt.domain.model.CardAnimationStyle
import com.yugentech.mynt.domain.model.SettingsConfiguration
import com.yugentech.mynt.user.datastore.UserDataStore
import com.yugentech.theme.service.HapticService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userDataStore: UserDataStore,
    private val hapticService: HapticService
) : ViewModel() {

    val settingsConfiguration: StateFlow<SettingsConfiguration> = userDataStore.settingsConfiguration
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SettingsConfiguration()
        )

    fun setCurrencySymbol(symbol: String) {
        viewModelScope.launch { userDataStore.setCurrencySymbol(symbol) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { userDataStore.setNotificationsEnabled(enabled) }
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { userDataStore.setDailyReminderEnabled(enabled) }
    }

    fun setPlayfulRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { userDataStore.setPlayfulRemindersEnabled(enabled) }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch { userDataStore.setReminderTime(hour, minute) }
    }

    fun setCardAnimationStyle(style: CardAnimationStyle) {
        viewModelScope.launch { userDataStore.setCardAnimationStyle(style) }
    }

    fun setHeroCardVisible(card: HeroCardKind, visible: Boolean) {
        viewModelScope.launch { userDataStore.setHeroCardVisible(card, visible) }
    }

    fun setMonthlyBudget(amount: Double) {
        viewModelScope.launch { userDataStore.setMonthlyBudget(amount) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userDataStore.setHapticsEnabled(enabled)
            if (enabled) {
                hapticService.performHaptic()
            }
        }
    }

    // Light tick for taps that change what's on screen (tabs, filters, chips).
    fun tick() = hapticService.performTickHaptic()

    // A firmer confirmation for actions that commit something, like saving an entry.
    fun confirm() = hapticService.performCompletionHaptic()
}