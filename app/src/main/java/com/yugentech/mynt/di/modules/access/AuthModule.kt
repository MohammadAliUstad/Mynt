package com.yugentech.mynt.di.modules.access

import com.yugentech.mynt.auth.repository.AuthRepositoryImpl
import com.yugentech.mynt.auth.service.AuthService
import com.yugentech.mynt.auth.viewmodel.AuthViewModel
import com.yugentech.mynt.domain.AuthRepository
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {

    single {
        AuthService(
            auth = get(),
            oneTapClient = get()
        )
    }

    single<AuthRepository> {
        AuthRepositoryImpl(
            authService = get()
        )
    }

    viewModel {
        AuthViewModel(
            authRepository = get()
        )
    }
}
