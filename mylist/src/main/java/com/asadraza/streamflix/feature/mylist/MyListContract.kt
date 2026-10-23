package com.asadraza.streamflix.feature.mylist

import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.model.library.MyListItem

data class MyListState(
    val items: List<MyListItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorType: ErrorType? = null
)

sealed interface MyListEvent {
    data class OnItemClick(val movieId: String) : MyListEvent

    data object OnRetry : MyListEvent
}

sealed interface MyListEffect {
    data class NavigateToDetail(val movieId: String) : MyListEffect
}
