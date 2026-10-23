package com.asadraza.streamflix.core.data.source

/** Thrown by data sources when an operation needs a signed-in user and there is none. */
class UnauthenticatedException : IllegalStateException("No signed-in user")

class ProfileLimitReachedException(limit: Int) : IllegalStateException("Maximum of $limit profiles reached")

class LastProfileException : IllegalStateException("The last remaining profile cannot be deleted")
