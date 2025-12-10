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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
        observeLoginState()
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
        val fullText = "Nie masz konta? Zarejestruj się"
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

        val startIndex = fullText.indexOf("Zarejestruj się")
        val endIndex = startIndex + "Zarejestruj się".length
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun observeLoginState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is LoginState.Idle -> {
                            showLoading(false)
                        }
                        is LoginState.Loading -> {
                            showLoading(true)
                        }
                        is LoginState.Success -> {
                            showLoading(false)
                            SharedPreferencesHelper.saveLoggedInUser(this@LoginActivity, state.email)
                            navigateToMain()
                        }
                        is LoginState.Error -> {
                            showLoading(false)
                            binding.textViewPasswordError.text = state.message
                            binding.textViewPasswordError.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !show
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
