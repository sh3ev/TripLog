package com.example.triplog.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.triplog.data.AppDatabase
import com.example.triplog.utils.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(private val database: AppDatabase) : ViewModel() {
    val loginResult = androidx.lifecycle.MutableLiveData<LoginResult>()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val passwordHash = PasswordHasher.hashPassword(password)
                val user = withContext(Dispatchers.IO) {
                    database.userDao().authenticateUser(email, passwordHash)
                }
                
                if (user != null) {
                    loginResult.postValue(LoginResult.Success(user.email))
                } else {
                    loginResult.postValue(LoginResult.Error("Nieprawidłowy email lub hasło"))
                }
            } catch (e: Exception) {
                loginResult.postValue(LoginResult.Error("Błąd logowania: ${e.message}"))
            }
        }
    }
}

sealed class LoginResult {
    data class Success(val email: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

