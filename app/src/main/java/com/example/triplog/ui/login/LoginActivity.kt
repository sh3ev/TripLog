package com.example.triplog.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.triplog.R
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

        val loggedInUser = SharedPreferencesHelper.getLoggedInUser(this)
        if (loggedInUser != null) {
            navigateToMain()
            return
        }

        binding.buttonLogin.setOnClickListener {
            clearErrors()
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString()

            var isValid = true
            if (email.isEmpty()) {
                binding.textViewEmailError.text = "Wypełnij to pole"
                binding.textViewEmailError.visibility = View.VISIBLE
                isValid = false
            } else if (!isValidEmail(email)) {
                binding.textViewEmailError.text = "Niepoprawny format email"
                binding.textViewEmailError.visibility = View.VISIBLE
                isValid = false
            }

            if (password.isEmpty()) {
                binding.textViewPasswordError.text = "Wypełnij to pole"
                binding.textViewPasswordError.visibility = View.VISIBLE
                isValid = false
            }

            if (isValid) {
                viewModel.login(email, password)
            }
        }

        setupRegisterLink()

        observeLoginResult()
    }

    private fun clearErrors() {
        binding.textViewEmailError.visibility = View.GONE
        binding.textViewPasswordError.visibility = View.GONE
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun setupRegisterLink() {
        val textView = binding.textRegisterLink
        val fullText = "Don't have an account? Register"
        val spannableString = SpannableString(fullText)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.color = resources.getColor(R.color.accent_blue, null)
            }
        }

        val startIndex = fullText.indexOf("Register")
        val endIndex = startIndex + "Register".length
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
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
                        binding.textViewPasswordError.text = result.message
                        binding.textViewPasswordError.visibility = View.VISIBLE
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
