package com.yugentech.mynt.navigation.screen

import com.yugentech.mynt.database.model.TransactionType

sealed class AppScreen(val route: String) {
    data object SignIn : AppScreen("sign_in")
    data object SignUp : AppScreen("sign_up")
    data object Main : AppScreen("main")
    data object LentBorrowed : AppScreen("lent_borrowed")
    data object Voice : AppScreen("voice")
    data object Appearance : AppScreen("appearance")

    /** Adding and editing an entry: pass [ARG_ID] to edit, or [ARG_TYPE] to start a new one. */
    data object Transaction : AppScreen("transaction?$ARG_ID={$ARG_ID}&$ARG_TYPE={$ARG_TYPE}") {
        fun add(type: TransactionType = TransactionType.EXPENSE) = "transaction?$ARG_TYPE=${type.name}"
        fun edit(id: String) = "transaction?$ARG_ID=$id"
    }

    companion object {
        const val ARG_ID = "id"
        const val ARG_TYPE = "type"
    }
}
