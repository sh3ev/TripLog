package com.example.triplog.ui.register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonRegister.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val name = binding.editTextName.text.toString().trim()
            val password = binding.editTextPassword.text.toString()
            val confirmPassword = binding.editTextConfirmPassword.text.toString()

            if (email.isEmpty() || name.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Hasła nie są identyczne", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Hasło musi mieć co najmniej 6 znaków", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(email, name, password)
        }

        binding.textViewLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser(email: String, name: String, password: String) {
        lifecycleScope.launch {
            try {
                val existingUser = withContext(Dispatchers.IO) {
                    database.userDao().getUserByEmail(email)
                }

                if (existingUser != null) {
                    Toast.makeText(this@RegisterActivity, "Użytkownik o tym emailu już istnieje", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val passwordHash = PasswordHasher.hashPassword(password)
                val user = UserEntity(email = email, name = name, passwordHash = passwordHash)

                withContext(Dispatchers.IO) {
                    database.userDao().insertUser(user)
                }

                Toast.makeText(this@RegisterActivity, "Rejestracja zakończona sukcesem", Toast.LENGTH_SHORT).show()
                SharedPreferencesHelper.saveLoggedInUser(this@RegisterActivity, email)
                
                val intent = Intent(this@RegisterActivity, com.example.triplog.ui.main.MainActivity::class.java)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "Błąd rejestracji: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

