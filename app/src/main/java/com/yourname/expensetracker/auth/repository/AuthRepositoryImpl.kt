package com.yourname.expensetracker.auth.repository

import android.app.PendingIntent
import android.content.Intent
import com.google.firebase.auth.FirebaseUser
import com.yourname.expensetracker.auth.domain.AuthRepository
import com.yourname.expensetracker.auth.domain.AuthResult
import com.yourname.expensetracker.auth.service.AuthService
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val authService: AuthService
) : AuthRepository {

    override val authState: Flow<FirebaseUser?> = authService.authStateFlow

    override val currentUser: String?
        get() = authService.currentUser?.uid

    override val currentUserName: String?
        get() = authService.currentUser?.displayName?.takeIf { it.isNotBlank() }

    override suspend fun signUp(
        name: String,
        email: String,
        password: String
    ): AuthResult<FirebaseUser> {
        return authService.signUp(name, email, password)
    }

    override suspend fun signIn(email: String, password: String): AuthResult<FirebaseUser> {
        return authService.signIn(email, password)
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit> {
        return authService.sendPasswordResetEmail(email)
    }

    override fun signOut() {
        authService.signOut()
    }

    override suspend fun getGoogleSignInIntent(webClientId: String): AuthResult<PendingIntent> {
        return authService.getGoogleSignInIntent(webClientId)
    }

    override suspend fun handleGoogleSignInResult(data: Intent?): AuthResult<FirebaseUser> {
        return authService.handleGoogleSignInResult(data)
    }
}
