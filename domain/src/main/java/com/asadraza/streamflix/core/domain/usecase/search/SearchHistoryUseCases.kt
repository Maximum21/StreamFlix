package com.asadraza.streamflix.core.domain.usecase.search

import com.asadraza.streamflix.core.domain.repository.SearchHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSearchHistoryUseCase @Inject constructor(private val repository: SearchHistoryRepository) {
    operator fun invoke(): Flow<List<String>> = repository.history
}

class AddSearchQueryUseCase @Inject constructor(private val repository: SearchHistoryRepository) {
    suspend operator fun invoke(query: String) = repository.add(query)
}

class RemoveSearchQueryUseCase @Inject constructor(private val repository: SearchHistoryRepository) {
    suspend operator fun invoke(query: String) = repository.remove(query)
}

class ClearSearchHistoryUseCase @Inject constructor(private val repository: SearchHistoryRepository) {
    suspend operator fun invoke() = repository.clear()
}
