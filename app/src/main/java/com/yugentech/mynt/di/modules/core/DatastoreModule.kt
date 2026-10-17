package com.yugentech.mynt.di.modules.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme")

// Keeps the pre-refactor file name so existing currency/notification preferences are preserved
private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "expense_tracker_preferences")

val dataStoreModule = module {

    single<DataStore<Preferences>>(named("theme")) {
        androidContext().themeDataStore
    }

    single<DataStore<Preferences>>(named("user")) {
        androidContext().userDataStore
    }
}
