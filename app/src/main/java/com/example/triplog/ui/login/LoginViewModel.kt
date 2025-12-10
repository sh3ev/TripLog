package com.example.triplog.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.triplog.data.AppDatabase
import com.example.triplog.utils.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(private val database: AppDatabase) : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val passwordHash = PasswordHasher.hashPassword(password)
                val user = withContext(Dispatchers.IO) {
                    database.userDao().authenticateUser(email, passwordHash)
                }
                
                if (user != null) {
                    _loginState.value = LoginState.Success(user.email)
                } else {
                    _loginState.value = LoginState.Error("Nieprawidłowy email lub hasło")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Błąd logowania: ${e.message}")
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val email: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

