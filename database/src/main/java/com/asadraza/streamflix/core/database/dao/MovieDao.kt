package com.asadraza.streamflix.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.asadraza.streamflix.core.database.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for movie operations
 *
 * Database layer
 * Room generates implementation
 * Clean, focused DAO without business logic
 */
@Dao
interface MovieDao {

    /**
     * Insert or replace movies
     * WHY: OnConflictStrategy.REPLACE updates existing
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    /**
     * Get movies by category
     * WHY: Returns Flow for reactive updates
     */
    @Query("SELECT * FROM movies WHERE category = :category ORDER BY vote_average DESC")
    fun getMoviesByCategory(category: String): Flow<List<MovieEntity>>

    /**
     * Get all movies (for search)
     * WHY: Flow emits new list when database changes
     */
    @Query("SELECT * FROM movies")
    fun getAllMovies(): Flow<List<MovieEntity>>

    /**
     * Search movies by title
     * WHY: LIKE operator for substring matching
     */
    @Query("""
        SELECT * FROM movies 
        WHERE title LIKE '%' || :query || '%'
        ORDER BY vote_average DESC
    """)
    fun searchMovies(query: String): Flow<List<MovieEntity>>

    /**
     * Get movie by ID
     */
    @Query("SELECT * FROM movies WHERE id = :movieId")
    suspend fun getMovieById(movieId: String): MovieEntity?

    /**
     * Delete old cached movies
     * WHY: Cache invalidation based on timestamp
     */
    @Query("DELETE FROM movies WHERE cached_at < :timestamp")
    suspend fun deleteOldMovies(timestamp: Long)

    /**
     * Delete movies by category
     * WHY: Refresh specific category
     */
    @Query("DELETE FROM movies WHERE category = :category")
    suspend fun deleteMoviesByCategory(category: String)

    /**
     * Clear all movies
     */
    @Query("DELETE FROM movies")
    suspend fun clearAllMovies()
}