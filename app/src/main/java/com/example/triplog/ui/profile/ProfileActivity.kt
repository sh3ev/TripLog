package com.example.triplog.ui.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.example.triplog.data.AppDatabase
import com.example.triplog.databinding.ActivityProfileBinding
import com.example.triplog.ui.login.LoginActivity
import com.example.triplog.utils.SharedPreferencesHelper
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

        // Zapisz zmiany
        binding.buttonSave.setOnClickListener {
            saveProfile()
        }

        // Wyloguj
        binding.buttonLogout.setOnClickListener {
            logout()
        }

        loadUserData()
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            currentUserEmail?.let { email ->
                val user = withContext(Dispatchers.IO) {
                    database.userDao().getUserByEmail(email)
                }
                user?.let {
                    binding.textViewEmail.text = it.email
                    binding.editTextFirstName.setText(it.firstName ?: "")
                    binding.editTextLastName.setText(it.lastName ?: "")
                    
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

    private fun saveProfile() {
        val firstName = binding.editTextFirstName.text.toString().trim().ifEmpty { null }
        val lastName = binding.editTextLastName.text.toString().trim().ifEmpty { null }

        lifecycleScope.launch {
            currentUserEmail?.let { email ->
                withContext(Dispatchers.IO) {
                    database.userDao().updateProfile(email, firstName, lastName)
                }
                Toast.makeText(this@ProfileActivity, "Profil zaktualizowany", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
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
                
                Toast.makeText(this@ProfileActivity, "Zdjęcie zaktualizowane", Toast.LENGTH_SHORT).show()
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
