package com.yugentech.mynt.auth.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.yugentech.mynt.domain.AuthRepository
import com.yugentech.mynt.domain.AuthResult
import com.yugentech.mynt.auth.state.AuthState
import com.yugentech.mynt.auth.state.UserData
import com.yugentech.mynt.ui.access.signIn.state.ForgotPasswordState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState(isInitializing = true))
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _forgotPasswordState =
        MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { firebaseUser ->
                if (firebaseUser != null) {
                    val user = UserData(
                        userId = firebaseUser.uid,
                        name = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: "User",
                        email = firebaseUser.email ?: "No email provided"
                    )
                    _authState.update {
                        it.copy(
                            isInitializing = false,
                            isLoading = false,
                            isUserLoggedIn = true,
                            userId = firebaseUser.uid,
                            userData = user,
                            firebaseUser = firebaseUser,
                            error = null
                        )
                    }
                } else {
                    _authState.update {
                        AuthState(
                            isInitializing = false,
                            isLoading = false,
                            isUserLoggedIn = false,
                            userId = null,
                            userData = null,
                            firebaseUser = null,
                            error = null
                        )
                    }
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        _authState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.signIn(email, password)) {
                is AuthResult.Success -> {
                    _authState.update { it.copy(isLoading = false) }
                }
                is AuthResult.Error -> {
                    _authState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun signUp(name: String, email: String, password: String) {
        _authState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.signUp(name, email, password)) {
                is AuthResult.Success -> {
                    _authState.update { it.copy(isLoading = false) }
                }
                is AuthResult.Error -> {
                    _authState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun getGoogleSignInIntent(webClientId: String) {
        _authState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.getGoogleSignInIntent(webClientId)) {
                is AuthResult.Success -> {
                    _authState.update { it.copy(isLoading = false, intent = result.data) }
                }
                is AuthResult.Error -> {
                    _authState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun handleGoogleSignInResult(data: Intent?) {
        _authState.update { it.copy(isLoading = true, error = null, intent = null) }
        viewModelScope.launch {
            when (val result = authRepository.handleGoogleSignInResult(data)) {
                is AuthResult.Success -> {
                    _authState.update { it.copy(isLoading = false) }
                }
                is AuthResult.Error -> {
                    _authState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun onGoogleSignInCancelled() {
        _authState.update { it.copy(isLoading = false, intent = null, error = "Sign-In was cancelled.") }
    }

    fun signOut() {
        _authState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun forgotPassword(email: String) {
        _forgotPasswordState.value = ForgotPasswordState.Loading
        viewModelScope.launch {
            when (val result = authRepository.sendPasswordResetEmail(email)) {
                is AuthResult.Success -> {
                    _forgotPasswordState.value = ForgotPasswordState.Success
                }
                is AuthResult.Error -> {
                    _forgotPasswordState.value = ForgotPasswordState.Error(result.message)
                }
            }
        }
    }

    fun clearForgotPasswordState() {
        _forgotPasswordState.value = ForgotPasswordState.Idle
    }

    fun clearError() {
        _authState.update { it.copy(error = null) }
    }
}
