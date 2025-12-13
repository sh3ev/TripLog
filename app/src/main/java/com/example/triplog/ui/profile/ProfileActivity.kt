package com.example.triplog.ui.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.example.triplog.R
import com.example.triplog.data.AppDatabase
import com.example.triplog.databinding.ActivityProfileBinding
import com.example.triplog.ui.login.LoginActivity
import com.example.triplog.utils.SharedPreferencesHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private var currentUserEmail: String? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { saveProfileImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserEmail = SharedPreferencesHelper.getLoggedInUser(this)
        if (currentUserEmail == null) {
            finish()
            return
        }

        setupClickListeners()
        loadUserData()
    }

    private fun setupClickListeners() {
        // Przycisk wstecz
        binding.buttonBack.setOnClickListener { finish() }

        // Zmień zdjęcie
        binding.buttonChangePhoto.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        
        // Kliknięcie w zdjęcie też otwiera wybór
        binding.imageViewProfile.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Edycja imienia
        binding.itemFirstName.setOnClickListener {
            showEditDialog(
                title = getString(R.string.profile_first_name),
                currentValue = binding.textViewFirstName.text.toString().let { 
                    if (it == getString(R.string.profile_not_set)) "" else it 
                },
                onSave = { newValue -> updateFirstName(newValue) }
            )
        }

        // Edycja nazwiska
        binding.itemLastName.setOnClickListener {
            showEditDialog(
                title = getString(R.string.profile_last_name),
                currentValue = binding.textViewLastName.text.toString().let { 
                    if (it == getString(R.string.profile_not_set)) "" else it 
                },
                onSave = { newValue -> updateLastName(newValue) }
            )
        }

        // Zmiana hasła
        binding.itemChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Wyloguj
        binding.buttonLogout.setOnClickListener {
            logout()
        }
    }

    private fun showEditDialog(title: String, currentValue: String, onSave: (String) -> Unit) {
        val inputLayout = TextInputLayout(this).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            hint = title
            setPadding(48, 32, 48, 16)
        }
        
        val input = TextInputEditText(this).apply {
            setText(currentValue)
        }
        inputLayout.addView(input)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.edit_field_title, title.lowercase()))
            .setView(inputLayout)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newValue = input.text.toString().trim()
                onSave(newValue)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val currentPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.editTextCurrentPassword)
        val newPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.editTextNewPassword)
        val confirmPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.editTextConfirmPassword)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.profile_change_password))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val currentPassword = currentPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()
                
                changePassword(currentPassword, newPassword, confirmPassword)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateFirstName(firstName: String) {
        lifecycleScope.launch {
            currentUserEmail?.let { email ->
                withContext(Dispatchers.IO) {
                    val user = database.userDao().getUserByEmail(email)
                    user?.let {
                        database.userDao().updateProfile(email, firstName.ifEmpty { null }, it.lastName)
                    }
                }
                binding.textViewFirstName.text = firstName.ifEmpty { getString(R.string.profile_not_set) }
                Toast.makeText(this@ProfileActivity, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
            }
        }
    }

    private fun updateLastName(lastName: String) {
        lifecycleScope.launch {
            currentUserEmail?.let { email ->
                withContext(Dispatchers.IO) {
                    val user = database.userDao().getUserByEmail(email)
                    user?.let {
                        database.userDao().updateProfile(email, it.firstName, lastName.ifEmpty { null })
                    }
                }
                binding.textViewLastName.text = lastName.ifEmpty { getString(R.string.profile_not_set) }
                Toast.makeText(this@ProfileActivity, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
            }
        }
    }

    private fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        lifecycleScope.launch {
            currentUserEmail?.let { email ->
                // Walidacja
                if (newPassword.length < 6) {
                    Toast.makeText(this@ProfileActivity, getString(R.string.password_error_too_short), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                if (newPassword != confirmPassword) {
                    Toast.makeText(this@ProfileActivity, getString(R.string.password_error_mismatch), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val success = withContext(Dispatchers.IO) {
                    val user = database.userDao().getUserByEmail(email)
                    if (user?.passwordHash == currentPassword) {
                        database.userDao().updatePassword(email, newPassword)
                        true
                    } else {
                        false
                    }
                }

                if (success) {
                    Toast.makeText(this@ProfileActivity, getString(R.string.password_changed), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ProfileActivity, getString(R.string.password_error_current), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            currentUserEmail?.let { email ->
                val user = withContext(Dispatchers.IO) {
                    database.userDao().getUserByEmail(email)
                }
                user?.let {
                    binding.textViewEmail.text = it.email
                    binding.textViewFirstName.text = it.firstName ?: getString(R.string.profile_not_set)
                    binding.textViewLastName.text = it.lastName ?: getString(R.string.profile_not_set)
                    
                    // Załaduj zdjęcie profilowe
                    it.profileImagePath?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(path)
                            val rotatedBitmap = rotateBitmapIfRequired(bitmap, path)
                            binding.imageViewProfile.setImageBitmap(rotatedBitmap)
                        }
                    }
                }
            }
        }
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, imagePath: String): Bitmap {
        return try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                else -> return bitmap
            }
            
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun saveProfileImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                val imagePath = withContext(Dispatchers.IO) {
                    val inputStream = contentResolver.openInputStream(uri)
                    val imagesDir = File(getExternalFilesDir(null), "profile_images")
                    if (!imagesDir.exists()) imagesDir.mkdirs()
                    
                    val imageFile = File(imagesDir, "profile_${currentUserEmail?.replace("@", "_")}.jpg")
                    
                    inputStream?.use { input ->
                        FileOutputStream(imageFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    // Zapisz ścieżkę w bazie
                    currentUserEmail?.let { email ->
                        database.userDao().updateProfileImage(email, imageFile.absolutePath)
                    }
                    
                    imageFile.absolutePath
                }
                
                // Załaduj nowe zdjęcie z korektą orientacji
                val bitmap = BitmapFactory.decodeFile(imagePath)
                val rotatedBitmap = rotateBitmapIfRequired(bitmap, imagePath)
                binding.imageViewProfile.setImageBitmap(rotatedBitmap)
                
                Toast.makeText(this@ProfileActivity, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Błąd zapisu zdjęcia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        SharedPreferencesHelper.clearLoggedInUser(this)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
