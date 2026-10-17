package com.yugentech.mynt.di.modules.config

import com.yugentech.mynt.ui.tabs.profileScreen.viewmodel.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { SettingsViewModel(userDataStore = get(), hapticService = get()) }
}
