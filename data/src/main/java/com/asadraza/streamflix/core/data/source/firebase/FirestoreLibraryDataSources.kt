package com.asadraza.streamflix.core.data.source.firebase

import com.asadraza.streamflix.core.data.source.MyListDataSource
import com.asadraza.streamflix.core.data.source.UnauthenticatedException
import com.asadraza.streamflix.core.data.source.WatchHistoryDataSource
import com.asadraza.streamflix.core.model.library.MyListItem
import com.asadraza.streamflix.core.model.library.WatchedMovie
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private fun FirebaseFirestore.profileCollection(auth: FirebaseAuth, profileId: String, name: String) =
    collection(FirestorePaths.USERS)
        .document(auth.currentUser?.uid ?: throw UnauthenticatedException())
        .collection(FirestorePaths.PROFILES)
        .document(profileId)
        .collection(name)

private fun Query.snapshots(): Flow<List<DocumentSnapshot>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
        } else if (snapshot != null) {
            trySend(snapshot.documents)
        }
    }
    awaitClose { registration.remove() }
}

/** Firestore keeps a local cache, so the list is readable offline and syncs when the network returns. */
class FirestoreMyListDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : MyListDataSource {
    private fun collection(profileId: String): CollectionReference =
        firestore.profileCollection(auth, profileId, FirestorePaths.MY_LIST)

    override fun observeItems(profileId: String): Flow<List<MyListItem>> =
        collection(profileId).orderBy(FIELD_ADDED_AT, Query.Direction.DESCENDING).snapshots()
            .map { docs -> docs.mapNotNull { it.toItem() } }

    override fun observeContains(profileId: String, movieId: String): Flow<Boolean> =
        collection(profileId).whereEqualTo(FIELD_MOVIE_ID, movieId).snapshots().map { it.isNotEmpty() }

    override suspend fun put(profileId: String, item: MyListItem) {
        collection(profileId).document(item.movieId).set(
            mapOf(
                FIELD_MOVIE_ID to item.movieId,
                "title" to item.title,
                "posterPath" to item.posterPath,
                FIELD_ADDED_AT to item.addedAt
            )
        ).await()
    }

    override suspend fun remove(profileId: String, movieId: String) {
        collection(profileId).document(movieId).delete().await()
    }

    private fun DocumentSnapshot.toItem(): MyListItem? {
        val movieId = getString(FIELD_MOVIE_ID) ?: return null
        val title = getString("title") ?: return null
        return MyListItem(
            movieId = movieId,
            title = title,
            posterPath = getString("posterPath"),
            addedAt = getLong(FIELD_ADDED_AT) ?: 0L
        )
    }

    private companion object {
        const val FIELD_MOVIE_ID = "movieId"
        const val FIELD_ADDED_AT = "addedAt"
    }
}

class FirestoreWatchHistoryDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : WatchHistoryDataSource {
    private fun collection(profileId: String): CollectionReference =
        firestore.profileCollection(auth, profileId, FirestorePaths.HISTORY)

    override fun observeRecent(profileId: String, limit: Int): Flow<List<WatchedMovie>> =
        collection(profileId).orderBy(FIELD_WATCHED_AT, Query.Direction.DESCENDING).limit(limit.toLong()).snapshots()
            .map { docs -> docs.mapNotNull { it.toItem() } }

    override suspend fun put(profileId: String, item: WatchedMovie) {
        collection(profileId).document(item.movieId).set(
            mapOf(
                "movieId" to item.movieId,
                "title" to item.title,
                "posterPath" to item.posterPath,
                "backdropPath" to item.backdropPath,
                FIELD_WATCHED_AT to item.watchedAt,
                "positionMillis" to item.positionMillis
            )
        ).await()
    }

    override suspend fun remove(profileId: String, movieId: String) {
        collection(profileId).document(movieId).delete().await()
    }

    private fun DocumentSnapshot.toItem(): WatchedMovie? {
        val movieId = getString("movieId") ?: return null
        val title = getString("title") ?: return null
        return WatchedMovie(
            movieId = movieId,
            title = title,
            posterPath = getString("posterPath"),
            backdropPath = getString("backdropPath"),
            watchedAt = getLong(FIELD_WATCHED_AT) ?: 0L,
            positionMillis = getLong("positionMillis") ?: 0L
        )
    }

    private companion object {
        const val FIELD_WATCHED_AT = "watchedAt"
    }
}
