package com.example.triplog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.triplog.data.entities.UserEntity

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email AND passwordHash = :passwordHash")
    suspend fun authenticateUser(email: String, passwordHash: String): UserEntity?

    @Query("UPDATE users SET firstName = :firstName, lastName = :lastName WHERE email = :email")
    suspend fun updateProfile(email: String, firstName: String?, lastName: String?)

    @Query("UPDATE users SET profileImagePath = :imagePath WHERE email = :email")
    suspend fun updateProfileImage(email: String, imagePath: String?)

    @Query("UPDATE users SET passwordHash = :newPassword WHERE email = :email")
    suspend fun updatePassword(email: String, newPassword: String)
}

