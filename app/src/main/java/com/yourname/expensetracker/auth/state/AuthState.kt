package com.yourname.expensetracker.auth.state

import android.app.PendingIntent
import com.google.firebase.auth.FirebaseUser

data class UserData(
    val userId: String = "",
    val name: String = "",
    val email: String = ""
)

data class AuthState(
    val isInitializing: Boolean = true,
    val isLoading: Boolean = false,
    val userId: String? = null,
    val error: String? = null,
    val intent: PendingIntent? = null,
    val userData: UserData? = null,
    val firebaseUser: FirebaseUser? = null,
    val isUserLoggedIn: Boolean = false
)
