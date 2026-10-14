package com.yourname.expensetracker.auth.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.yourname.expensetracker.auth.domain.AuthRepository
import com.yourname.expensetracker.auth.domain.AuthResult
import com.yourname.expensetracker.auth.state.AuthState
import com.yourname.expensetracker.auth.state.UserData
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

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { firebaseUser ->
                if (firebaseUser != null) {
                    val user = UserData(
                        userId = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email ?: ""
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

    fun signOut() {
        _authState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun clearError() {
        _authState.update { it.copy(error = null) }
    }
}
