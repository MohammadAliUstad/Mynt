package com.yugentech.mynt.di.modules.finance

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.yugentech.mynt.database.dao.TransactionDao
import com.yugentech.mynt.transactions.repository.TransactionRepository
import com.yugentech.mynt.transactions.repository.TransactionRepositoryImpl
import com.yugentech.mynt.transactions.viewmodel.TransactionsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val transactionsModule = module {

    single<TransactionRepository> {
        TransactionRepositoryImpl(
            transactionDao = get<TransactionDao>(),
            firestore = get<FirebaseFirestore>(),
            auth = get<FirebaseAuth>()
        )
    }

    viewModel {
        TransactionsViewModel(
            transactionRepository = get<TransactionRepository>()
        )
    }
}