package com.yugentech.mynt.di.modules.finance

import com.yugentech.mynt.transactions.repository.TransactionRepository
import com.yugentech.mynt.transactions.repository.TransactionRepositoryImpl
import com.yugentech.mynt.transactions.viewmodel.TransactionsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val transactionsModule = module {

    single<TransactionRepository> {
        TransactionRepositoryImpl(
            transactionDao = get()
        )
    }

    viewModel {
        TransactionsViewModel(
            transactionRepository = get()
        )
    }
}
