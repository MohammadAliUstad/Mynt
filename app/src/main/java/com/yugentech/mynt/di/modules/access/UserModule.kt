package com.yugentech.mynt.di.modules.access

import com.yugentech.mynt.user.datastore.UserDataStore
import org.koin.core.qualifier.named
import org.koin.dsl.module

val userModule = module {

    single {
        UserDataStore(
            dataStore = get(named("user"))
        )
    }
}
