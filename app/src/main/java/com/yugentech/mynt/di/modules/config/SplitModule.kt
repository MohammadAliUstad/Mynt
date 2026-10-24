package com.yugentech.mynt.di.modules.finance

import com.yugentech.mynt.split.repository.SplitRepository
import com.yugentech.mynt.split.repository.SplitRepositoryImpl
import org.koin.dsl.module

val splitModule = module {
    single<SplitRepository> {
        SplitRepositoryImpl(
            splitDao = get(),
            firestore = get(),
            auth = get()
        )
    }
}