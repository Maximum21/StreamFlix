package com.asadraza.streamflix.core.data.source

import com.asadraza.streamflix.core.model.library.MyListItem
import com.asadraza.streamflix.core.model.library.WatchedMovie
import kotlinx.coroutines.flow.Flow

/** Port over the per-profile "My List" store. Throws [UnauthenticatedException] when nobody is signed in. */
interface MyListDataSource {
    fun observeItems(profileId: String): Flow<List<MyListItem>>

    fun observeContains(profileId: String, movieId: String): Flow<Boolean>

    suspend fun put(profileId: String, item: MyListItem)

    suspend fun remove(profileId: String, movieId: String)
}

/** Port over the per-profile watch history. */
interface WatchHistoryDataSource {
    fun observeRecent(profileId: String, limit: Int): Flow<List<WatchedMovie>>

    suspend fun put(profileId: String, item: WatchedMovie)

    suspend fun remove(profileId: String, movieId: String)
}
