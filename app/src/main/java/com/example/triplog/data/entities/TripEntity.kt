package com.example.triplog.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "trips",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["email"],
            childColumns = ["userEmail"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userEmail: String,
    val title: String,
    val description: String,
    val date: String, // Format: yyyy-MM-dd (data początkowa, zachowana dla kompatybilności)
    val imagePath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val weatherSummary: String? = null,
    val locationName: String? = null, // Nazwa miejsca (np. "Kraków, Polska")
    val endDate: String? = null // Format: yyyy-MM-dd (data końcowa podróży)
)

