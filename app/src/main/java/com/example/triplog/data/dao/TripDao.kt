package com.example.triplog.data.dao

import androidx.room.*
import com.example.triplog.data.entities.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips WHERE userEmail = :userEmail ORDER BY date DESC")
    fun getTripsByUser(userEmail: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE userEmail = :userEmail AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY date DESC")
    fun searchTrips(userEmail: String, query: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): TripEntity?

    @Insert
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)
}

