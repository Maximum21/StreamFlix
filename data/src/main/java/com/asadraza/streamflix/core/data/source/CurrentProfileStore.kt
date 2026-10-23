package com.asadraza.streamflix.core.data.source

import kotlinx.coroutines.flow.Flow

/** Local record of which profile is active on this device. */
interface CurrentProfileStore {
    val currentProfileId: Flow<String?>

    suspend fun set(profileId: String)

    suspend fun clear()
}
