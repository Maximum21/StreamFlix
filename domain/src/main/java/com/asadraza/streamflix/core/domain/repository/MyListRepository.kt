package com.asadraza.streamflix.core.domain.repository

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.model.library.MyListItem
import kotlinx.coroutines.flow.Flow

/** The titles the active profile saved, newest first. */
interface MyListRepository {
    fun observeMyList(): Flow<Result<List<MyListItem>>>

    fun observeIsInMyList(movieId: String): Flow<Boolean>

    fun add(item: MyListItem): Flow<Result<Unit>>

    fun remove(movieId: String): Flow<Result<Unit>>
}
