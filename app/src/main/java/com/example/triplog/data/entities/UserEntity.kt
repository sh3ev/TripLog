package com.example.triplog.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val email: String,
    val name: String,
    val passwordHash: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val profileImagePath: String? = null
)

