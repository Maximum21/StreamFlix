package com.asadraza.streamflix.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.asadraza.streamflix.core.database.datasource.LocalDataSource
import com.asadraza.streamflix.core.database.entity.MovieEntity
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** In-memory Room database shared by the DAO and data source tests. */
class DatabaseTestRule : TestWatcher() {
    lateinit var db: StreamFlixDatabase
        private set

    val localDataSource: LocalDataSource
        get() = LocalDataSource(db, db.movieDao(), db.movieDetailDao(), db.remoteKeyDao())

    override fun starting(description: Description) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, StreamFlixDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    override fun finished(description: Description) {
        db.close()
    }
}

fun movieEntity(id: String, cachedAt: Long = 1_000L) = MovieEntity(
    id = id,
    title = "Movie $id",
    description = "Description",
    backdropPath = null,
    posterPath = null,
    releaseDate = "2024-01-01",
    voteAverage = 7.5,
    voteCount = 100,
    genreIds = "28,12",
    originalLanguage = "en",
    adult = false,
    video = false,
    cachedAt = cachedAt
)
