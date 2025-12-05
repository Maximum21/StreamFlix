package com.asadraza.streamflix.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.asadraza.streamflix.core.database.entity.MovieDetailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovieDetail(movieDetail: MovieDetailEntity)

    @Query("SELECT * FROM movie_details WHERE id = :movieId")
    fun getMovieDetailById(movieId: String): Flow<MovieDetailEntity?>

    @Query("DELETE FROM movie_details WHERE cached_at < :timestamp")
    suspend fun deleteOldMovieDetails(timestamp: Long)

    @Query("DELETE FROM movie_details")
    suspend fun clearAllMovieDetails()
}