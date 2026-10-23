package com.asadraza.streamflix.core.domain.analytics

/**
 * The complete event taxonomy. Events carry identifiers and counts only, never free text or
 * account details. `docs/ANALYTICS.md` documents each event.
 */
sealed class AnalyticsEvent(val name: String, val params: Map<String, String> = emptyMap()) {
    data object Login : AnalyticsEvent("login", mapOf("method" to "password"))

    data object SignUp : AnalyticsEvent("sign_up", mapOf("method" to "password"))

    /** The query text is deliberately not recorded, only its length. */
    data class SearchResultOpened(val queryLength: Int) :
        AnalyticsEvent("search_result_opened", mapOf("query_length" to queryLength.toString()))

    data class MyListChanged(val movieId: String, val added: Boolean) :
        AnalyticsEvent(
            name = if (added) "my_list_add" else "my_list_remove",
            params = mapOf("movie_id" to movieId)
        )

    data class TrailerPlayed(val movieId: String) :
        AnalyticsEvent("trailer_played", mapOf("movie_id" to movieId))
}

/** Sends events to the analytics backend. Implementations must respect the user's consent. */
interface AnalyticsTracker {
    fun log(event: AnalyticsEvent)
}
