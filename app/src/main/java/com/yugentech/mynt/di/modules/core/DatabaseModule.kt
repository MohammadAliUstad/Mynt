package com.yugentech.mynt.di.modules.core

import androidx.room.Room
import com.yugentech.mynt.database.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "mynt_database"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    single { get<AppDatabase>().transactionDao() }

    single { get<AppDatabase>().splitDao() }
}