package com.asadraza.streamflix.core.domain.usecase.library

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.MyListRepository
import com.asadraza.streamflix.core.domain.repository.WatchHistoryRepository
import com.asadraza.streamflix.core.model.library.MyListItem
import com.asadraza.streamflix.core.model.library.WatchedMovie
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveMyListUseCase @Inject constructor(private val repository: MyListRepository) {
    operator fun invoke(): Flow<Result<List<MyListItem>>> = repository.observeMyList()
}

class ObserveIsInMyListUseCase @Inject constructor(private val repository: MyListRepository) {
    operator fun invoke(movieId: String): Flow<Boolean> = repository.observeIsInMyList(movieId)
}

class AddToMyListUseCase @Inject constructor(private val repository: MyListRepository) {
    operator fun invoke(item: MyListItem): Flow<Result<Unit>> = repository.add(item)
}

class RemoveFromMyListUseCase @Inject constructor(private val repository: MyListRepository) {
    operator fun invoke(movieId: String): Flow<Result<Unit>> = repository.remove(movieId)
}

class ObserveWatchHistoryUseCase @Inject constructor(private val repository: WatchHistoryRepository) {
    operator fun invoke(): Flow<Result<List<WatchedMovie>>> = repository.observeRecent()
}

class RecordWatchUseCase @Inject constructor(private val repository: WatchHistoryRepository) {
    suspend operator fun invoke(item: WatchedMovie): Result<Unit> = repository.record(item)
}

class RemoveFromWatchHistoryUseCase @Inject constructor(private val repository: WatchHistoryRepository) {
    operator fun invoke(movieId: String): Flow<Result<Unit>> = repository.remove(movieId)
}
