package com.asadraza.streamflix.core.data.source

import com.asadraza.streamflix.core.model.auth.EmailVerificationStatus
import com.asadraza.streamflix.core.model.auth.User
import com.asadraza.streamflix.core.model.library.MyListItem
import com.asadraza.streamflix.core.model.library.WatchedMovie
import com.asadraza.streamflix.core.model.profile.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

fun testUser(verified: Boolean = true) = User(
    uid = "uid-1",
    email = "user@example.com",
    displayName = "User",
    photoUrl = null,
    isEmailVerified = verified
)

class FakeAuthDataSource : AuthDataSource {
    var signInResult: Result<User> = Result.success(testUser())
    var failure: Throwable? = null
    var signedOut = 0
    var deletedWithPassword: String? = null
    var registered: Triple<String, String, String>? = null
    var verificationEmailsSent = 0
    val users = MutableStateFlow<User?>(null)
    var verificationFlow: Flow<EmailVerificationStatus> = flow { throw UnauthenticatedException() }

    override suspend fun signIn(email: String, password: String): User {
        failure?.let { throw it }
        return signInResult.getOrThrow()
    }

    override suspend fun register(name: String, email: String, password: String) {
        failure?.let { throw it }
        registered = Triple(name, email, password)
    }

    override suspend fun signOut() {
        signedOut++
    }

    override suspend fun sendPasswordReset(email: String) {
        failure?.let { throw it }
    }

    override suspend fun resendVerificationEmail() {
        failure?.let { throw it }
        verificationEmailsSent++
    }

    override suspend fun deleteAccount(password: String) {
        failure?.let { throw it }
        deletedWithPassword = password
    }

    override fun observeCurrentUser(): Flow<User?> = users

    override fun observeEmailVerification(): Flow<EmailVerificationStatus> = verificationFlow
}

class FakeProfileDataSource : ProfileDataSource {
    val profiles = MutableStateFlow<List<Profile>>(emptyList())
    var failure: Throwable? = null
    private var nextId = 1

    override fun observeProfiles(): Flow<List<Profile>> = flow {
        failure?.let { throw it }
        profiles.collect { emit(it) }
    }

    override fun observeProfile(profileId: String): Flow<Profile?> =
        profiles.map { list -> list.firstOrNull { it.id == profileId } }

    override suspend fun create(profile: Profile): Profile {
        failure?.let { throw it }
        if (profiles.value.size >= ProfileDataSource.MAX_PROFILES) {
            throw ProfileLimitReachedException(ProfileDataSource.MAX_PROFILES)
        }
        return profile.copy(id = "p${nextId++}").also { profiles.value += it }
    }

    override suspend fun update(profile: Profile) {
        profiles.value = profiles.value.map { if (it.id == profile.id) profile else it }
    }

    override suspend fun delete(profileId: String) {
        if (profiles.value.size <= 1) throw LastProfileException()
        profiles.value = profiles.value.filterNot { it.id == profileId }
    }
}

class FakeCurrentProfileStore : CurrentProfileStore {
    private val id = MutableStateFlow<String?>(null)
    override val currentProfileId: Flow<String?> = id

    override suspend fun set(profileId: String) {
        id.value = profileId
    }

    override suspend fun clear() {
        id.value = null
    }
}

class FakeMyListDataSource : MyListDataSource {
    val items = MutableStateFlow<Map<String, List<MyListItem>>>(emptyMap())
    var failure: Throwable? = null

    override fun observeItems(profileId: String): Flow<List<MyListItem>> =
        items.map { (it[profileId] ?: emptyList()).sortedByDescending { item -> item.addedAt } }

    override fun observeContains(profileId: String, movieId: String): Flow<Boolean> =
        items.map { all -> all[profileId].orEmpty().any { it.movieId == movieId } }

    override suspend fun put(profileId: String, item: MyListItem) {
        failure?.let { throw it }
        val current = items.value[profileId].orEmpty().filterNot { it.movieId == item.movieId }
        items.value += (profileId to current + item)
    }

    override suspend fun remove(profileId: String, movieId: String) {
        failure?.let { throw it }
        items.value += (profileId to items.value[profileId].orEmpty().filterNot { it.movieId == movieId })
    }
}

class FakeWatchHistoryDataSource : WatchHistoryDataSource {
    val items = MutableStateFlow<Map<String, List<WatchedMovie>>>(emptyMap())
    var failure: Throwable? = null

    override fun observeRecent(profileId: String, limit: Int): Flow<List<WatchedMovie>> =
        items.map { (it[profileId] ?: emptyList()).sortedByDescending { item -> item.watchedAt }.take(limit) }

    override suspend fun put(profileId: String, item: WatchedMovie) {
        failure?.let { throw it }
        val current = items.value[profileId].orEmpty().filterNot { it.movieId == item.movieId }
        items.value += (profileId to current + item)
    }

    override suspend fun remove(profileId: String, movieId: String) {
        items.value += (profileId to items.value[profileId].orEmpty().filterNot { it.movieId == movieId })
    }
}
