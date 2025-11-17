package com.example.triplog.data.dao

import androidx.room.*
import com.example.triplog.data.entities.TripImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripImageDao {
    @Query("SELECT * FROM trip_images WHERE tripId = :tripId ORDER BY orderIndex ASC")
    fun getImagesByTripId(tripId: Long): Flow<List<TripImageEntity>>

    @Query("SELECT * FROM trip_images WHERE tripId = :tripId ORDER BY orderIndex ASC")
    suspend fun getImagesByTripIdSync(tripId: Long): List<TripImageEntity>

    @Insert
    suspend fun insertImage(image: TripImageEntity): Long

    @Insert
    suspend fun insertImages(images: List<TripImageEntity>)

    @Delete
    suspend fun deleteImage(image: TripImageEntity)

    @Query("DELETE FROM trip_images WHERE tripId = :tripId")
    suspend fun deleteImagesByTripId(tripId: Long)
}

