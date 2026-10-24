package com.asadraza.streamflix.feature.search

import com.asadraza.streamflix.core.domain.usecase.search.AddSearchQueryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.ClearSearchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.ObserveSearchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.RemoveSearchQueryUseCase
import javax.inject.Inject

/** The recent-searches operations, grouped so the ViewModel's constructor stays small. */
class SearchHistoryUseCases
@Inject
constructor(
    val observe: ObserveSearchHistoryUseCase,
    val add: AddSearchQueryUseCase,
    val remove: RemoveSearchQueryUseCase,
    val clear: ClearSearchHistoryUseCase
)
