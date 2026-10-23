package com.asadraza.streamflix.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.asadraza.streamflix.core.database.entity.MovieCategoryEntity
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.database.entity.SearchResultEntity

@Dao
interface MovieDao {
    // Upsert, not REPLACE: REPLACE deletes the row first, which would cascade into the list tables.
    @Upsert
    suspend fun upsertMovies(movies: List<MovieEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryEntries(entries: List<MovieCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchResults(results: List<SearchResultEntity>)

    @Query(
        """
        SELECT m.* FROM movies m
        INNER JOIN movie_category c ON c.movie_id = m.id
        WHERE c.category = :category
        ORDER BY c.position ASC
        """
    )
    fun categoryPagingSource(category: String): PagingSource<Int, MovieEntity>

    @Query(
        """
        SELECT m.* FROM movies m
        INNER JOIN search_result s ON s.movie_id = m.id
        WHERE s.`query` = :query
        ORDER BY s.position ASC
        """
    )
    fun searchPagingSource(query: String): PagingSource<Int, MovieEntity>

    @Query("DELETE FROM movie_category WHERE category = :category")
    suspend fun deleteCategoryEntries(category: String)

    @Query("DELETE FROM search_result WHERE `query` = :query")
    suspend fun deleteSearchResults(query: String)

    @Query("DELETE FROM search_result WHERE created_at < :timestamp")
    suspend fun deleteSearchResultsOlderThan(timestamp: Long)

    // Movies that no list refers to any more; list tables reference them with ON DELETE CASCADE.
    @Query(
        """
        DELETE FROM movies
        WHERE id NOT IN (SELECT movie_id FROM movie_category)
        AND id NOT IN (SELECT movie_id FROM search_result)
        """
    )
    suspend fun deleteOrphanMovies()

    @Query(
        """
        SELECT MAX(m.cached_at) FROM movies m
        INNER JOIN movie_category c ON c.movie_id = m.id
        WHERE c.category = :category
        """
    )
    suspend fun getNewestCachedAt(category: String): Long?

    @Query("SELECT COUNT(*) FROM movie_category WHERE category = :category")
    suspend fun countByCategory(category: String): Int

    // Cascades into the category and search tables
    @Query("DELETE FROM movies")
    suspend fun deleteAllMovies()

    @Query("SELECT COUNT(*) FROM movies")
    suspend fun countMovies(): Int
}
