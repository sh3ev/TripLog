package com.example.triplog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.triplog.data.entities.UserEntity

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email AND passwordHash = :passwordHash")
    suspend fun authenticateUser(email: String, passwordHash: String): UserEntity?
}

