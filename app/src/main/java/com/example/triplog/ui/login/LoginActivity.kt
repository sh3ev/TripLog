package com.example.triplog.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.triplog.data.AppDatabase
import com.example.triplog.databinding.ActivityLoginBinding
import com.example.triplog.ui.main.MainActivity
import com.example.triplog.ui.register.RegisterActivity
import com.example.triplog.utils.SharedPreferencesHelper
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val viewModel: LoginViewModel by lazy { 
        androidx.lifecycle.ViewModelProvider(this, LoginViewModelFactory(database))[LoginViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sprawdź czy użytkownik jest już zalogowany
        val loggedInUser = SharedPreferencesHelper.getLoggedInUser(this)
        if (loggedInUser != null) {
            navigateToMain()
            return
        }

        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        binding.textViewRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        observeLoginResult()
    }

    private fun observeLoginResult() {
        lifecycleScope.launch {
            viewModel.loginResult.observe(this@LoginActivity) { result ->
                when (result) {
                    is LoginResult.Success -> {
                        SharedPreferencesHelper.saveLoggedInUser(this@LoginActivity, result.email)
                        navigateToMain()
                    }
                    is LoginResult.Error -> {
                        Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

class LoginViewModelFactory(private val database: AppDatabase) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

