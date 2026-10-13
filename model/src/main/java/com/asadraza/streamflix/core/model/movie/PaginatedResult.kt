package com.asadraza.streamflix.core.model.movie

data class PaginatedResult<T>(
    val data : List<T>,
    val page: Int,
    val totalPages: Int,
    val totalResult: Int
){
    val hasNextPage : Boolean
        get() = page < totalPages

    val hasPreviousPage: Boolean
        get() = page > 1

    val isEmpty: Boolean
        get() = data.isEmpty()

    companion object PagingResult {
        fun <T> empty() : PaginatedResult<T> = PaginatedResult (
            data = emptyList(),
            page = 1,
            totalResult = 0,
            totalPages = 0
        )
    }
}