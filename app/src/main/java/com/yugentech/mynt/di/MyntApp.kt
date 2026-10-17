package com.yugentech.mynt.di

import android.app.Application
import com.yugentech.mynt.BuildConfig
import com.yugentech.mynt.di.modules.access.authModule
import com.yugentech.mynt.di.modules.access.userModule
import com.yugentech.mynt.di.modules.config.settingsModule
import com.yugentech.mynt.di.modules.config.themeModule
import com.yugentech.mynt.di.modules.core.dataStoreModule
import com.yugentech.mynt.di.modules.core.databaseModule
import com.yugentech.mynt.di.modules.core.firebaseModule
import com.yugentech.mynt.di.modules.finance.transactionsModule
import com.yugentech.mynt.di.modules.finance.voiceModule
import com.yugentech.mynt.reminders.ReminderScheduler
import com.yugentech.mynt.user.datastore.UserDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class MyntApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidLogger()
            androidContext(this@MyntApp)

            modules(
                authModule,
                userModule,
                themeModule,
                settingsModule,
                dataStoreModule,
                databaseModule,
                firebaseModule,
                transactionsModule,
                voiceModule
            )
        }

        keepRemindersInSync()
    }

    // Whenever the reminder settings change, from anywhere, the daily alarm follows.
    private fun keepRemindersInSync() {
        ReminderScheduler.ensureChannel(this)
        get<UserDataStore>().settingsConfiguration
            .map { it.notificationConfig }
            .distinctUntilChanged()
            .onEach { ReminderScheduler.apply(this, it) }
            .launchIn(appScope)
    }
}
