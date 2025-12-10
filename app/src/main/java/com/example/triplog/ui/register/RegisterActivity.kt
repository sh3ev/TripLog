package com.example.triplog.ui.register

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.triplog.R
import com.example.triplog.data.AppDatabase
import com.example.triplog.data.entities.UserEntity
import com.example.triplog.databinding.ActivityRegisterBinding
import com.example.triplog.ui.login.LoginActivity
import com.example.triplog.utils.PasswordHasher
import com.example.triplog.utils.SharedPreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.buttonSingup.setOnClickListener {
            clearErrors()
            val email = binding.editTextEmail.text.toString().trim()
            val firstName = binding.editTextFirstName.text.toString().trim()
            val lastName = binding.editTextLastName.text.toString().trim()
            val password = binding.editTextPassword.text.toString()
            val confirmPassword = binding.editTextRetypePassword.text.toString()

            var isValid = true

            if (firstName.isEmpty()) {
                binding.textViewFirstNameError.text = "Wypełnij to pole"
                binding.textViewFirstNameError.visibility = View.VISIBLE
                isValid = false
            }

            if (lastName.isEmpty()) {
                binding.textViewLastNameError.text = "Wypełnij to pole"
                binding.textViewLastNameError.visibility = View.VISIBLE
                isValid = false
            }

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
            } else if (password.length < 6) {
                binding.textViewPasswordError.text = "Hasło musi mieć co najmniej 6 znaków"
                binding.textViewPasswordError.visibility = View.VISIBLE
                isValid = false
            }

            if (confirmPassword.isEmpty()) {
                binding.textViewRetypePasswordError.text = "Wypełnij to pole"
                binding.textViewRetypePasswordError.visibility = View.VISIBLE
                isValid = false
            } else if (password != confirmPassword) {
                binding.textViewRetypePasswordError.text = "Hasła nie są identyczne"
                binding.textViewRetypePasswordError.visibility = View.VISIBLE
                isValid = false
            }

            if (isValid) {
                val fullName = "$firstName $lastName"
                registerUser(email, fullName, password)
            }
        }

        setupLoginLink()
    }

    private fun clearErrors() {
        binding.textViewFirstNameError.visibility = View.GONE
        binding.textViewLastNameError.visibility = View.GONE
        binding.textViewEmailError.visibility = View.GONE
        binding.textViewPasswordError.visibility = View.GONE
        binding.textViewRetypePasswordError.visibility = View.GONE
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun setupLoginLink() {
        val textView = binding.textRegisterLink
        val fullText = "Masz już konto? Zaloguj się"
        val spannableString = SpannableString(fullText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                finish()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.color = resources.getColor(R.color.accent_blue, null)
            }
        }

        val startIndex = fullText.indexOf("Zaloguj się")
        val endIndex = startIndex + "Zaloguj się".length
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun registerUser(email: String, name: String, password: String) {
        lifecycleScope.launch {
            try {
                val existingUser = withContext(Dispatchers.IO) {
                    database.userDao().getUserByEmail(email)
                }

                if (existingUser != null) {
                    binding.textViewEmailError.text = "Użytkownik o tym emailu już istnieje"
                    binding.textViewEmailError.visibility = View.VISIBLE
                    return@launch
                }

                val passwordHash = PasswordHasher.hashPassword(password)

                val user = UserEntity(
                    email = email,
                    name = name,
                    passwordHash = passwordHash
                )

                withContext(Dispatchers.IO) {
                    database.userDao().insertUser(user)
                }

                Toast.makeText(
                    this@RegisterActivity,
                    "Rejestracja zakończona sukcesem",
                    Toast.LENGTH_SHORT
                ).show()

                SharedPreferencesHelper.saveLoggedInUser(this@RegisterActivity, email)

                val intent = Intent(
                    this@RegisterActivity,
                    com.example.triplog.ui.main.MainActivity::class.java
                )
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    "Błąd rejestracji: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
